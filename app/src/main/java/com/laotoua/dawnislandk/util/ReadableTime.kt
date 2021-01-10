/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk.util

import android.content.Context
import android.content.res.Resources
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

object ReadableTime {
    private var sResources: Resources? = null
    private var timeFormat: Int = 1
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
    private val mCalendar = Calendar.getInstance()
    private val localInstance get() = Locale.getDefault()

    // The website use GMT+08:00
    private val SERVER_DATETIME_FORMAT =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", localInstance).apply {
            timeZone = TimeZone.getTimeZone("GMT+08:00")
        }

    // rests use local TZ
    val DATE_ONLY_FORMAT = SimpleDateFormat("yyyy-MM-dd", localInstance)
    private val DATETIME_FORMAT = SimpleDateFormat("yy/MM/dd HH:mm", localInstance)
    private val DATETIME_FORMAT_WITHOUT_YEAR = SimpleDateFormat("MM/dd HH:mm", localInstance)
    private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss", localInstance)
    private val FILENAME_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", localInstance)
    private val sCalendarLock = Any()
    private val sDateFormatLock1 = Any()
    private val sDateFormatLock2 = Any()


    private val TIME_SERVER_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", localInstance)
    private val TIME_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm", localInstance)
    private val TIME_DATETIME_FORMAT_WITHOUT_YEAR = DateTimeFormatter.ofPattern("MM/dd HH:mm", localInstance)
    private val TIME_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", localInstance)

    fun initialize(context: Context) {
        sResources = context.applicationContext.resources
        timeFormat = DawnApp.applicationDataStore.displayTimeFormat
    }

    fun getTodayDateLong(): Long {
        return string2Time(getDateString(Date(), DATE_ONLY_FORMAT), DATE_ONLY_FORMAT)
    }

    fun getDateString(time: Long, format: SimpleDateFormat? = null): String {
        return getDateString(Date(time), format)
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

    private fun serverTimeStringToZonedJavaTime(str: String): ZonedDateTime {
        val s = if (str.contains("(")) str.substring(0, 10) + " " + str.substring(13) else str
        return LocalDateTime.parse(s, TIME_SERVER_DATETIME_FORMAT).atZone(ZoneId.of("Asia/Shanghai"))
    }

    fun getDisplayTime(time: String): String {
        return when (timeFormat) {
            1 -> getDisplayTimeAgo(time)
            0 -> getPlainDisplayTime(time)
            else -> throw Exception("Unhandled time format")
        }
    }

    fun getDisplayTime(time: Long): String {
        return when (timeFormat) {
            1 -> getDisplayTimeAgo(time)
            0 -> getPlainDisplayTime(time)
            else -> throw Exception("Unhandled time format")
        }
    }

    private fun getPlainDisplayTime(time: String): String {
        val nowDate = ZonedDateTime.now()
        val timeDate = serverTimeStringToZonedJavaTime(time)
        return if (nowDate.year == timeDate.year && nowDate.dayOfYear == timeDate.dayOfYear) {
            timeDate.format(TIME_TIME_FORMAT)
        } else if (nowDate.year == timeDate.year) {
            timeDate.format(TIME_DATETIME_FORMAT_WITHOUT_YEAR)
        } else {
            timeDate.format(TIME_DATETIME_FORMAT)
        }
    }

    private fun getDisplayTimeAgo(time: String): String {
        val resources = sResources!!
        val nowDate = ZonedDateTime.now()
        val timeDate = serverTimeStringToZonedJavaTime(time)

        val duration = Duration.between(timeDate, nowDate)
        when {
            duration.isNegative -> { // error case
                return resources.getString(R.string.from_the_future)
            }
            duration.toMinutes() < 3 -> {
                return resources.getString(R.string.just_now)
            }
            duration.toHours() < 1 -> {
                return resources.getString(R.string.some_minutes_ago, duration.toMinutes())
            }
            duration.toHours() < 18 -> {
                return resources.getString(R.string.some_hours_ago, duration.toHours())
            }
        }
        val period = Period.between(timeDate.toLocalDate(), nowDate.toLocalDate())
        when {
            period.years == 0 && period.months == 0 && period.days == 1 -> {
                return resources.getString(R.string.yesterday)
            }
            period.years == 0 && period.months == 0 && period.days <= 7 -> {
                return resources.getString(R.string.some_days_ago, period.days)
            }
        }
        return getPlainDisplayTime(time)

    }

    private fun getPlainDisplayTime(time: Long): String {
        synchronized(
            sCalendarLock
        ) {
            val nowDate = Date(System.currentTimeMillis())
            val timeDate = Date(time)
            mCalendar.time = nowDate
            val nowYear = mCalendar[Calendar.YEAR]
            val nowDayOfYear = mCalendar[Calendar.DAY_OF_YEAR]
            mCalendar.time = timeDate
            val timeYear = mCalendar[Calendar.YEAR]
            val timeDayOfYear = mCalendar[Calendar.DAY_OF_YEAR]
            return if (nowYear == timeYear && nowDayOfYear == timeDayOfYear) {
                TIME_FORMAT.format(timeDate)
            } else if (nowYear == timeYear) {
                DATETIME_FORMAT_WITHOUT_YEAR.format(timeDate)
            } else {
                DATETIME_FORMAT.format(timeDate)
            }
        }
    }

    fun getTimeAgo(time1: Long, time2: Long, applyTimezoneOffset: Boolean = false): Long {
        synchronized(
            sDateFormatLock1
        ) {
            val now = System.currentTimeMillis()
            mCalendar.time =
                if (applyTimezoneOffset) {
                    Date(
                        time1 + (TimeZone.getTimeZone("GMT+08:00")
                            .getOffset(now) - TimeZone.getDefault()
                            .getOffset(now)).toLong()
                    )
                } else {
                    Date(time1)
                }
            val nowLong = mCalendar.time.time
            mCalendar.time = Date(time2)
            val timeLong = mCalendar.time.time
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
        val diffInterval = getTimeInterval(diff)
        return if (diffInterval.sameMinute) {
            resources.getString(R.string.just_now)
        } else if (diffInterval.sameHour) {
            resources.getString(R.string.some_minutes_ago, diffInterval.minuteDiff)
        } else if (diffInterval.sameDay) {
            resources.getString(R.string.some_hours_ago, diffInterval.hourDiff)
        } else if (diffInterval.sameYear && diffInterval.dayDiff < 2) {
            resources.getString(R.string.yesterday)
        } else if (diffInterval.sameYear && diffInterval.dayDiff < 7) {
            resources.getString(R.string.some_days_ago, diffInterval.dayDiff)
        } else {
            getPlainDisplayTime(time)
        }
    }

    private fun getTimeInterval(time: Long): TimeDiffInterval {
        var leftover = time
        val intervals = mutableListOf<Int>()
        for (i in 0 until SIZE) {
            val multiple = MULTIPLES[i]
            val quotient = leftover / multiple
            val remainder = leftover % multiple
            intervals.add(quotient.toInt())
            leftover = remainder
        }
        return TimeDiffInterval(
            intervals[0],
            intervals[1],
            intervals[2],
            intervals[3],
            intervals[4]
        )
    }

    fun getCurrentTimeFileName(): String {
        synchronized(
            sDateFormatLock2
        ) { return FILENAME_DATE_FORMAT.format(Date(System.currentTimeMillis())) }
    }

    private class TimeDiffInterval(
        val yearDiff: Int,
        val dayDiff: Int,
        val hourDiff: Int,
        val minuteDiff: Int,
        val secondDiff: Int
    ) {
        val sameYear: Boolean = yearDiff == 0
        val sameDay: Boolean = sameYear && dayDiff == 0
        val sameHour: Boolean = sameDay && hourDiff == 0
        val sameMinute: Boolean = sameHour && minuteDiff == 0
    }
}