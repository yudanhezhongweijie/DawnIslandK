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

package com.laotoua.dawnislandk.data.local.dao

import androidx.room.*
import com.laotoua.dawnislandk.data.local.entity.Emoji
import java.time.LocalDateTime

@Dao
interface EmojiDao {
    @Query("SELECT * From Emoji ORDER BY lastUsedAt DESC, id ASC")
    suspend fun getAllEmojiByLastUsedAtDESC(): List<Emoji>

    @Query("SELECT * From Emoji ORDER BY id ASC")
    suspend fun getAllEmojiByIdASC(): List<Emoji>

    suspend fun getAllEmoji(sort: Boolean): List<Emoji> {
        return if (sort) getAllEmojiByLastUsedAtDESC() else getAllEmojiByIdASC()
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(emoji: Emoji)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(emojiList: List<Emoji>)

    @Transaction
    suspend fun setLastUsedEmoji(emoji: Emoji) {
        emoji.lastUsedAt = LocalDateTime.now()
        updateEmoji(emoji)
    }

    @Update
    suspend fun updateEmoji(emoji: Emoji)

    @Delete
    suspend fun deleteEmoji(emoji: Emoji)

    @Query("DELETE FROM Emoji")
    suspend fun nukeEmoji()

    suspend fun resetEmoji() {
        nukeEmoji()
        val default = mutableListOf<Emoji>()
        val now = LocalDateTime.now()
        default.add(Emoji("|∀ﾟ", "|∀ﾟ", false, now))
        default.add(Emoji("(´ﾟДﾟ`)", "(´ﾟДﾟ`)", false, now))
        default.add(Emoji("(;´Д`)", "(;´Д`)", false, now))
        default.add(Emoji("(｀･ω･)", "(｀･ω･)", false, now))
        default.add(Emoji("(=ﾟωﾟ)=", "(=ﾟωﾟ)=", false, now))
        default.add(Emoji("| ω・´)", "| ω・´)", false, now))
        default.add(Emoji("|-` )", "|-` )", false, now))
        default.add(Emoji("|д` )", "|д` )", false, now))
        default.add(Emoji("|ー` )", "|ー` )", false, now))
        default.add(Emoji("|∀` )", "|∀` )", false, now))
        default.add(Emoji("(つд⊂)", "(つд⊂)", false, now))
        default.add(Emoji("(ﾟДﾟ≡ﾟДﾟ)", "(ﾟДﾟ≡ﾟДﾟ)", false, now))
        default.add(Emoji("(＾o＾)ﾉ", "(＾o＾)ﾉ", false, now))
        default.add(Emoji("(|||ﾟДﾟ)", "(|||ﾟДﾟ)", false, now))
        default.add(Emoji("( ﾟ∀ﾟ)", "( ﾟ∀ﾟ)", false, now))
        default.add(Emoji("( ´∀`)", "( ´∀`)", false, now))
        default.add(Emoji("(*´∀`)", "(*´∀`)", false, now))
        default.add(Emoji("(*ﾟ∇ﾟ)", "(*ﾟ∇ﾟ)", false, now))
        default.add(Emoji("(*ﾟーﾟ)", "(*ﾟーﾟ)", false, now))
        default.add(Emoji("(　ﾟ 3ﾟ)", "(　ﾟ 3ﾟ)", false, now))
        default.add(Emoji("( ´ー`)", "( ´ー`)", false, now))
        default.add(Emoji("( ・_ゝ・)", "( ・_ゝ・)", false, now))
        default.add(Emoji("( ´_ゝ`)", "( ´_ゝ`)", false, now))
        default.add(Emoji("(*´д`)", "(*´д`)", false, now))
        default.add(Emoji("(・ー・)", "(・ー・)", false, now))
        default.add(Emoji("(・∀・)", "(・∀・)", false, now))
        default.add(Emoji("(ゝ∀･)", "(ゝ∀･)", false, now))
        default.add(Emoji("(〃∀〃)", "(〃∀〃)", false, now))
        default.add(Emoji("(*ﾟ∀ﾟ*)", "(*ﾟ∀ﾟ*)", false, now))
        default.add(Emoji("( ﾟ∀。)", "( ﾟ∀。)", false, now))
        default.add(Emoji("( `д´)", "( `д´)", false, now))
        default.add(Emoji("(`ε´ )", "(`ε´ )", false, now))
        default.add(Emoji("(`ヮ´ )", "(`ヮ´ )", false, now))
        default.add(Emoji("σ`∀´)", "σ`∀´)", false, now))
        default.add(Emoji(" ﾟ∀ﾟ)σ", " ﾟ∀ﾟ)σ", false, now))
        default.add(Emoji("ﾟ ∀ﾟ)ノ", "ﾟ ∀ﾟ)ノ", false, now))
        default.add(Emoji("(╬ﾟдﾟ)", "(╬ﾟдﾟ)", false, now))
        default.add(Emoji("(|||ﾟдﾟ)", "(|||ﾟдﾟ)", false, now))
        default.add(Emoji("( ﾟдﾟ)", "( ﾟдﾟ)", false, now))
        default.add(Emoji("Σ( ﾟдﾟ)", "Σ( ﾟдﾟ)", false, now))
        default.add(Emoji("( ;ﾟдﾟ)", "( ;ﾟдﾟ)", false, now))
        default.add(Emoji("( ;´д`)", "( ;´д`)", false, now))
        default.add(Emoji("(　д ) ﾟ ﾟ", "(　д ) ﾟ ﾟ", false, now))
        default.add(Emoji("( ☉д⊙)", "( ☉д⊙)", false, now))
        default.add(Emoji("(((　ﾟдﾟ)))", "(((　ﾟдﾟ)))", false, now))
        default.add(Emoji("( ` ・´)", "( ` ・´)", false, now))
        default.add(Emoji("( ´д`)", "( ´д`)", false, now))
        default.add(Emoji("( -д-)", "( -д-)", false, now))
        default.add(Emoji("(>д<)", "(>д<)", false, now))
        default.add(Emoji("･ﾟ( ﾉд`ﾟ)", "･ﾟ( ﾉд`ﾟ)", false, now))
        default.add(Emoji("( TдT)", "( TдT)", false, now))
        default.add(Emoji("(￣∇￣)", "(￣∇￣)", false, now))
        default.add(Emoji("(￣3￣)", "(￣3￣)", false, now))
        default.add(Emoji("(￣ｰ￣)", "(￣ｰ￣)", false, now))
        default.add(Emoji("(￣ . ￣)", "(￣ . ￣)", false, now))
        default.add(Emoji("(￣皿￣)", "(￣皿￣)", false, now))
        default.add(Emoji("(￣艸￣)", "(￣艸￣)", false, now))
        default.add(Emoji("(￣︿￣)", "(￣︿￣)", false, now))
        default.add(Emoji("(￣︶￣)", "(￣︶￣)", false, now))
        default.add(Emoji("ヾ(´ωﾟ｀)", "ヾ(´ωﾟ｀)", false, now))
        default.add(Emoji("(*´ω`*)", "(*´ω`*)", false, now))
        default.add(Emoji("(・ω・)", "(・ω・)", false, now))
        default.add(Emoji("( ´・ω)", "( ´・ω)", false, now))
        default.add(Emoji("(｀・ω)", "(｀・ω)", false, now))
        default.add(Emoji("(´・ω・`)", "(´・ω・`)", false, now))
        default.add(Emoji("(`・ω・´)", "(`・ω・´)", false, now))
        default.add(Emoji("( `_っ´)", "( `_っ´)", false, now))
        default.add(Emoji("( `ー´)", "( `ー´)", false, now))
        default.add(Emoji("( ´_っ`)", "( ´_っ`)", false, now))
        default.add(Emoji("( ´ρ`)", "( ´ρ`)", false, now))
        default.add(Emoji("( ﾟωﾟ)", "( ﾟωﾟ)", false, now))
        default.add(Emoji("(oﾟωﾟo)", "(oﾟωﾟo)", false, now))
        default.add(Emoji("(　^ω^)", "(　^ω^)", false, now))
        default.add(Emoji("(｡◕∀◕｡)", "(｡◕∀◕｡)", false, now))
        default.add(Emoji("/( ◕‿‿◕ )\\", "/( ◕‿‿◕ )\\", false, now))
        default.add(Emoji("ヾ(´ε`ヾ)", "ヾ(´ε`ヾ)", false, now))
        default.add(Emoji("(ノﾟ∀ﾟ)ノ", "(ノﾟ∀ﾟ)ノ", false, now))
        default.add(Emoji("(σﾟдﾟ)σ", "(σﾟдﾟ)σ", false, now))
        default.add(Emoji("(σﾟ∀ﾟ)σ", "(σﾟ∀ﾟ)σ", false, now))
        default.add(Emoji("|дﾟ )", "|дﾟ )", false, now))
        default.add(Emoji("┃電柱┃", "┃電柱┃", false, now))
        default.add(Emoji("ﾟ(つд`ﾟ)", "ﾟ(つд`ﾟ)", false, now))
        default.add(Emoji("ﾟÅﾟ )　", "ﾟÅﾟ )　", false, now))
        default.add(Emoji("⊂彡☆))д`)", "⊂彡☆))д`)", false, now))
        default.add(Emoji("⊂彡☆))д´)", "⊂彡☆))д´)", false, now))
        default.add(Emoji("⊂彡☆))∀`)", "⊂彡☆))∀`)", false, now))
        default.add(Emoji("(´∀((☆ミつ", "(´∀((☆ミつ", false, now))
        default.add(Emoji("༼;´༎ຶ ۝ ༎ຶ༽", "༼;´༎ຶ ۝ ༎ຶ༽", false, now))
        default.add(Emoji("(　˘ω˘)", "(　˘ω˘)", false, now))
        default.add(Emoji("ᕕ( ᐛ )ᕗ", "ᕕ( ᐛ )ᕗ", false, now))
        default.add(Emoji("( *・ω・)✄╰ひ╯", "( *・ω・)✄╰ひ╯", false, now))
        default.add(Emoji("༼　•͟ ͜ •　༽\n༽ つд⊂ ༼", "༼　•͟ ͜ •　༽\n༽ つд⊂ ༼", false, now))
        default.add(Emoji("(ᯣ ̶̵̵̵̶̶̶̶̵̫̋̋̅̅̅ᯣ )", "(ᯣ ̶̵̵̵̶̶̶̶̵̫̋̋̅̅̅ᯣ )", false, now))
        default.add(Emoji("隐藏符", "[h][/h]", false, now))
        default.add(Emoji("(ﾉ)`ω´(ヾ)", "(ﾉ)`ω´(ヾ)", false, now))
        default.add(Emoji("( ´◔ ‸◔`)", "( ´◔ ‸◔`)", false, now))
        default.add(Emoji("( ﾟᯅ 。)", "( ﾟᯅ 。)", false, now))
        default.add(Emoji("全角空格", "　", false, now))

        insertAll(default)
    }
}