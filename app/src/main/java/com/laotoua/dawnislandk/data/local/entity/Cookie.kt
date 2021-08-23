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

package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import com.laotoua.dawnislandk.DawnApp
import java.time.LocalDateTime

@Entity(primaryKeys=["cookieHash","domain"])
data class Cookie(
    val cookieHash: String,
    val cookieName: String,
    var cookieDisplayName: String,
    var lastUsedAt: LocalDateTime = LocalDateTime.now(),
    val domain: String = DawnApp.currentDomain
){
    fun getApiHeaderCookieHash():String = "userhash=$cookieHash"
}