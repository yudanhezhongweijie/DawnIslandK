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

package com.laotoua.dawnislandk.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.dao.EmojiDao
import com.laotoua.dawnislandk.data.local.entity.Emoji
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class EmojiSettingViewModel @Inject constructor(private val emojiDao: EmojiDao) : ViewModel() {

    suspend fun getAllEmoji(): List<Emoji> {
        var res = emojiDao.getAllEmoji(DawnApp.applicationDataStore.getSortEmojiByLastUsedStatus())
        if (res.isEmpty()) {
            emojiDao.resetEmoji()
            res = emojiDao.getAllEmoji(DawnApp.applicationDataStore.getSortEmojiByLastUsedStatus())
        }
        return res
    }

    fun resetEmoji() {
        viewModelScope.launch {
            emojiDao.resetEmoji()
        }
    }

    fun setEmojiList(list: List<Emoji>) {
        // fix ids
        list.forEachIndexed { index: Int, emoji: Emoji ->
            emoji.id = index + 1
        }
        GlobalScope.launch {
            emojiDao.nukeEmoji()
            emojiDao.insertAll(list)
        }
    }
}