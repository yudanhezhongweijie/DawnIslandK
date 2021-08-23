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

@Entity(primaryKeys=["id","domain"])
data class BlockedId(
    val id: String,
    /** type 0 refers blocking post fid (uses in timeline only)
     *  type 1 refers blocking post id (uses in all communities)
     */
    val type: Int,
    val domain: String = DawnApp.currentDomain
) {
    fun isBlockedPost(): Boolean = type == 1

    fun isTimelineBlockedForum(): Boolean = type == 0

    companion object {
        fun makeBlockedPost(id: String, domain: String = DawnApp.currentDomain): BlockedId = BlockedId(id, 1, domain)

        fun makeTimelineBlockedForum(id: String, domain: String = DawnApp.currentDomain): BlockedId = BlockedId(id, 0, domain)
    }
}