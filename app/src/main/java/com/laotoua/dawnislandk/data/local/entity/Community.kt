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
import com.laotoua.dawnislandk.util.DawnConstants
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity
data class Community(
    @PrimaryKey
    val id: String,
    val sort: String,
    val name: String,
    val status: String,
    val forums: List<Forum>,
    val domain: String = DawnApp.currentDomain
) {
    fun isCommonForums(): Boolean {
        return id == "1000"
    }

    fun isCommonPosts(): Boolean {
        return id == "1001"
    }

    companion object {
        fun makeCommonForums(forums: List<Forum>, domain: String): Community {
            val id = if (domain == DawnConstants.ADNMBDomain) "1000" else "2000"
            return Community(id, "0", "常用板块", "n", forums, domain)
        }

        fun makeCommonPosts(fakeForums: List<Forum>, domain: String): Community {
            val id = if (domain == DawnConstants.ADNMBDomain) "1001" else "2001"
            return Community(id, "-1", "常用串", "n", fakeForums, domain)
        }

    }
}