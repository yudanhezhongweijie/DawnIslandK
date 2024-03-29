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
import androidx.room.PrimaryKey
import com.laotoua.dawnislandk.DawnApp
import java.time.LocalDateTime

@Entity
data class BrowsingHistory(
    var browsedDateTime: LocalDateTime = LocalDateTime.now(),
    val postId: String,
    var postFid: String,
    var pages: MutableSet<Int>, // number of pages read
    val domain: String = DawnApp.currentDomain
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}