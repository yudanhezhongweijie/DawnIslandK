/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.laotoua.dawnislandk.util

import android.content.Context
import android.content.res.Resources
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object ReadableTime {
    private var sResources: Resources? = null
    private var timeFormat: String? = null
    const val SECOND_MILLIS: Long = 1000
    const val MINUTE_MILLIS = 60 * SECOND_MILLIS
    const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    const val DAY_MILLIS = 24 * HOUR_MILLIS
    const val WEEK_MILLIS = 7 * DAY_MILLIS
    const val LAST_30_DAYS_MILLIS = 30 * DAY_MILLIS
    const val YEAR_MILLIS = 365 * DAY_MILLIS
    const val SIZE = 5
    val MULTIPLES = longArrayOf(
        YEAR_MILLIS,
        DAY_MILLIS,
        HOUR_MILLIS,
        MINUTE_MILLIS,
        SECOND_MILLIS
    )
    val UNITS = intArrayOf(
        R.plurals.year,
        R.plurals.day,
        R.plurals.hour,
        R.plurals.minute,
        R.plurals.second
    )
    private val sCalendar =
        Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"))
    private val localInstance get() = Locale.getDefault()
    val DATE_ONLY_FORMAT = SimpleDateFormat("yyyy-MM-dd", localInstance)
    private val SERVER_DATETIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", localInstance)
    private val DATETIME_FORMAT = SimpleDateFormat("yy/MM/dd HH:mm", localInstance)
    private val DATETIME_FORMAT_WITHOUT_YEAR = SimpleDateFormat("MM/dd HH:mm:ss", localInstance)
    private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss", localInstance)
    private val FILENAME_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", localInstance)
    private val sCalendarLock = Any()
    private val sDateFormatLock1 = Any()
    private val sDateFormatLock2 = Any()

    fun initialize(context: Context) {
        sResources = context.applicationContext.resources
        timeFormat = DawnApp.applicationDataStore.displayTimeFormat
    }

    init {
        // The website use GMT+08:00, so tell user the same
        TimeZone.getTimeZone("GMT+08:00").run {
            SERVER_DATETIME_FORMAT.timeZone = this
            DATETIME_FORMAT.timeZone = this
            DATETIME_FORMAT_WITHOUT_YEAR.timeZone = this
            DATE_ONLY_FORMAT.timeZone = this
            TIME_FORMAT.timeZone = this
        }
    }

    fun getTodayDateLong(): Long {
        return string2Time(getDateString(Date(), DATE_ONLY_FORMAT), DATE_ONLY_FORMAT)
    }

    fun getDateString(time: Long, format: SimpleDateFormat? = null): String {
        return format?.format(Date(time)) ?: DATE_ONLY_FORMAT.format(Date(time))
    }

    fun getDateString(date: Date, format: SimpleDateFormat? = null): String {
        return format?.format(date) ?: DATE_ONLY_FORMAT.format(date)
    }

    fun string2Time(str: String, dateFormat: SimpleDateFormat = SERVER_DATETIME_FORMAT): Long {
        var s = str
        if (s.contains("(")) {
            s = s.substring(0, 10) + " " + s.substring(13)
        }
        var date: Date? = null
        try {
            date = dateFormat.parse(s)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return date!!.time
    }

    fun getDisplayTime(time: String): String {
        return when (timeFormat) {
            "simplified" -> getDisplayTimeAgo(string2Time(time))
            "original" -> getPlainDisplayTime(string2Time(time))
            else -> throw Exception("Unhandled time format")
        }
    }

    fun getDisplayTime(time: Long): String {
        return when (timeFormat) {
            "simplified" -> getDisplayTimeAgo(time)
            "original" -> getPlainDisplayTime(time)
            else -> throw Exception("Unhandled time format")
        }
    }

    private fun getPlainDisplayTime(time: Long): String {
        synchronized(
            sCalendarLock
        ) {
            val nowDate = Date(System.currentTimeMillis())
            val timeDate = Date(time)
            sCalendar.time = nowDate
            val nowYear = sCalendar[Calendar.YEAR]
            sCalendar.time = timeDate
            val timeYear = sCalendar[Calendar.YEAR]
            return if (nowYear == timeYear) {
                DATETIME_FORMAT_WITHOUT_YEAR.format(timeDate)
            } else {
                DATETIME_FORMAT.format(timeDate)
            }
        }
    }

    fun getTimeAgo(time1: Long, time2: Long): Long {
        synchronized(
            sDateFormatLock1
        ) {
            sCalendar.time = Date(time1)
            val nowLong = sCalendar.time.time
            sCalendar.time = Date(time2)
            val timeLong = sCalendar.time.time
            return nowLong - timeLong
        }
    }

    private fun getDisplayTimeAgo(time: Long): String {
        val resources = sResources!!
        val now = System.currentTimeMillis()
        val diff = getTimeAgo(now, time)

        if (time > now + 2 * MINUTE_MILLIS || time <= 0) {
            return resources.getString(R.string.from_the_future)
        }
        return if (diff < MINUTE_MILLIS) {
            resources.getString(R.string.just_now)
        } else if (diff < 2 * MINUTE_MILLIS) {
            resources.getQuantityString(R.plurals.some_minutes_ago, 1, 1)
        } else if (diff < 50 * MINUTE_MILLIS) {
            val minutes = (diff / MINUTE_MILLIS).toInt()
            resources.getQuantityString(R.plurals.some_minutes_ago, minutes, minutes)
        } else if (diff < 90 * MINUTE_MILLIS) {
            resources.getQuantityString(R.plurals.some_hours_ago, 1, 1)
        } else if (diff < 24 * HOUR_MILLIS) {
            val hours = (diff / HOUR_MILLIS).toInt()
            resources.getQuantityString(R.plurals.some_hours_ago, hours, hours)
        } else if (diff < 48 * HOUR_MILLIS) {
            resources.getString(R.string.yesterday)
        } else if (diff < WEEK_MILLIS) {
            val days = (diff / DAY_MILLIS).toInt()
            resources.getString(R.string.some_days_ago, days)
        } else {
            getPlainDisplayTime(time)
        }
    }

    fun getTimeInterval(time: Long): String {
        val sb = StringBuilder()
        val resources = sResources
        var leftover = time
        var start = false
        for (i in 0 until SIZE) {
            val multiple = MULTIPLES[i]
            val quotient = leftover / multiple
            val remainder = leftover % multiple
            if (start || quotient != 0L || i == SIZE - 1) {
                if (start) {
                    sb.append(" ")
                }
                sb.append(quotient)
                    .append(" ")
                    .append(resources!!.getQuantityString(UNITS[i], quotient.toInt()))
                start = true
            }
            leftover = remainder
        }
        return sb.toString()
    }

    fun getCurrentTimeFileName(): String {
        synchronized(
            sDateFormatLock2
        ) { return FILENAME_DATE_FORMAT.format(Date(System.currentTimeMillis())) }
    }
}