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

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

object ReadableTime {
    private var sResources: Resources? = null
    private var timeFormat: Int = 1

    // The website use GMT+08:00
    // rests use local TZ
    private val serverZoneID: ZoneId get() = ZoneId.of("Asia/Shanghai")

    @SuppressLint("ConstantLocale")
    private val DATE_ONLY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

    @SuppressLint("ConstantLocale")
    val SERVER_DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    @SuppressLint("ConstantLocale")
    private val DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm", Locale.getDefault())

    @SuppressLint("ConstantLocale")
    private val DATETIME_FORMAT_WITHOUT_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm", Locale.getDefault())

    @SuppressLint("ConstantLocale")
    val TIME_ONLY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())

    @SuppressLint("ConstantLocale")
    private val FILENAME_DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())


    fun initialize(context: Context) {
        sResources = context.applicationContext.resources
        timeFormat = DawnApp.applicationDataStore.displayTimeFormat
    }

    fun getDateString(dateTime: LocalDateTime, format: DateTimeFormatter = DATE_ONLY_FORMAT): String {
        return dateTime.format(format)
    }

    fun localDateTimeToCalendarDate(dateTime: LocalDateTime): Calendar {
        val calendar = Calendar.getInstance()
        calendar.set(dateTime.year, dateTime.monthValue - 1, dateTime.dayOfMonth)
        return calendar
    }

    // server is in GMT+08:00, this convert times to that zone
    private fun serverTimeStringToServerZoneTime(str: String): ZonedDateTime {
        val s = if (str.contains("(")) str.substring(0, 10) + " " + str.substring(13) else str
        return LocalDateTime.parse(s, SERVER_DATETIME_FORMAT).atZone(serverZoneID)
    }

    fun serverTimeStringToServerLocalDateTime(str: String): LocalDateTime =
        serverTimeStringToServerZoneTime(str).toLocalDateTime()

    // assumes the given DateTime is in server's zone, then finds the equivalent local DateTime
    fun serverDateTimeToUserLocalDateTime(dateTime: LocalDateTime): LocalDateTime =
        dateTime.atZone(serverZoneID).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()

    private fun serverZonedDateTimeToUserLocalDateTime(zonedDateTime: ZonedDateTime): LocalDateTime =
        zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()

    // this converts server's time string to equivalent local DateTime to user's system timezone
    fun serverTimeStringToLocalJavaTime(str: String): LocalDateTime =
        serverTimeStringToServerZoneTime(str).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()

    fun getDisplayTime(time: String): String {
        return when (timeFormat) {
            1 -> getDisplayTimeAgo(time)
            0 -> getPlainDisplayTime(time)
            else -> throw Exception("Unhandled time format")
        }
    }

    fun getDisplayTime(time: LocalDateTime, format: DateTimeFormatter = DATETIME_FORMAT): String = time.format(format)

    private fun getPlainDisplayTime(time: String): String {
        val nowDate = ZonedDateTime.now()
        val timeDate = serverTimeStringToServerZoneTime(time)
        return if (nowDate.year == timeDate.year && nowDate.dayOfYear == timeDate.dayOfYear) {
            timeDate.format(TIME_ONLY_FORMAT)
        } else if (nowDate.year == timeDate.year) {
            timeDate.format(DATETIME_FORMAT_WITHOUT_YEAR)
        } else {
            timeDate.format(DATETIME_FORMAT)
        }
    }

    private fun getDisplayTimeAgo(time: String): String {
        val resources = sResources!!
        val nowDate = ZonedDateTime.now()
        val timeDate = serverTimeStringToServerZoneTime(time)

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
        }
        val period = Period.between(
            serverZonedDateTimeToUserLocalDateTime(timeDate).toLocalDate(),
            serverZonedDateTimeToUserLocalDateTime(nowDate).toLocalDate()
        )
        when {
            period.years == 0 && period.months == 0 && (period.days == 0 || (period.days == 1 && duration.toHours() <= 8)) -> {
                return resources.getString(R.string.some_hours_ago, duration.toHours())
            }
            period.years == 0 && period.months == 0 && period.days == 1 -> {
                return resources.getString(R.string.yesterday)
            }
            period.years == 0 && period.months == 0 && period.days <= 7 -> {
                return resources.getString(R.string.some_days_ago, period.days)
            }
        }
        return getPlainDisplayTime(time)

    }

    fun getCurrentTimeFileName(): String = LocalDateTime.now().format(FILENAME_DATETIME_FORMAT)

}