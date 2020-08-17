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
import java.util.*

@Dao
interface EmojiDao {
    @Query("SELECT * From Emoji ORDER BY lastUsedAt DESC, id ASC")
    suspend fun getAllEmoji(): List<Emoji>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(emoji: Emoji)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(emojiList: List<Emoji>)

    @Transaction
    suspend fun updateLastUsedEmoji(emoji: Emoji) {
        emoji.lastUsedAt = Date().time
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
        default.add(Emoji("|∀ﾟ", "|∀ﾟ", false, 0))
        default.add(Emoji("(´ﾟДﾟ`)", "(´ﾟДﾟ`)", false, 0))
        default.add(Emoji("(;´Д`)", "(;´Д`)", false, 0))
        default.add(Emoji("(｀･ω･)", "(｀･ω･)", false, 0))
        default.add(Emoji("(=ﾟωﾟ)=", "(=ﾟωﾟ)=", false, 0))
        default.add(Emoji("| ω・´)", "| ω・´)", false, 0))
        default.add(Emoji("|-` )", "|-` )", false, 0))
        default.add(Emoji("|д` )", "|д` )", false, 0))
        default.add(Emoji("|ー` )", "|ー` )", false, 0))
        default.add(Emoji("|∀` )", "|∀` )", false, 0))
        default.add(Emoji("(つд⊂)", "(つд⊂)", false, 0))
        default.add(Emoji("(ﾟДﾟ≡ﾟДﾟ)", "(ﾟДﾟ≡ﾟДﾟ)", false, 0))
        default.add(Emoji("(＾o＾)ﾉ", "(＾o＾)ﾉ", false, 0))
        default.add(Emoji("(|||ﾟДﾟ)", "(|||ﾟДﾟ)", false, 0))
        default.add(Emoji("( ﾟ∀ﾟ)", "( ﾟ∀ﾟ)", false, 0))
        default.add(Emoji("( ´∀`)", "( ´∀`)", false, 0))
        default.add(Emoji("(*´∀`)", "(*´∀`)", false, 0))
        default.add(Emoji("(*ﾟ∇ﾟ)", "(*ﾟ∇ﾟ)", false, 0))
        default.add(Emoji("(*ﾟーﾟ)", "(*ﾟーﾟ)", false, 0))
        default.add(Emoji("(　ﾟ 3ﾟ)", "(　ﾟ 3ﾟ)", false, 0))
        default.add(Emoji("( ´ー`)", "( ´ー`)", false, 0))
        default.add(Emoji("( ・_ゝ・)", "( ・_ゝ・)", false, 0))
        default.add(Emoji("( ´_ゝ`)", "( ´_ゝ`)", false, 0))
        default.add(Emoji("(*´д`)", "(*´д`)", false, 0))
        default.add(Emoji("(・ー・)", "(・ー・)", false, 0))
        default.add(Emoji("(・∀・)", "(・∀・)", false, 0))
        default.add(Emoji("(ゝ∀･)", "(ゝ∀･)", false, 0))
        default.add(Emoji("(〃∀〃)", "(〃∀〃)", false, 0))
        default.add(Emoji("(*ﾟ∀ﾟ*)", "(*ﾟ∀ﾟ*)", false, 0))
        default.add(Emoji("( ﾟ∀。)", "( ﾟ∀。)", false, 0))
        default.add(Emoji("( `д´)", "( `д´)", false, 0))
        default.add(Emoji("(`ε´ )", "(`ε´ )", false, 0))
        default.add(Emoji("(`ヮ´ )", "(`ヮ´ )", false, 0))
        default.add(Emoji("σ`∀´)", "σ`∀´)", false, 0))
        default.add(Emoji(" ﾟ∀ﾟ)σ", " ﾟ∀ﾟ)σ", false, 0))
        default.add(Emoji("ﾟ ∀ﾟ)ノ", "ﾟ ∀ﾟ)ノ", false, 0))
        default.add(Emoji("(╬ﾟдﾟ)", "(╬ﾟдﾟ)", false, 0))
        default.add(Emoji("(|||ﾟдﾟ)", "(|||ﾟдﾟ)", false, 0))
        default.add(Emoji("( ﾟдﾟ)", "( ﾟдﾟ)", false, 0))
        default.add(Emoji("Σ( ﾟдﾟ)", "Σ( ﾟдﾟ)", false, 0))
        default.add(Emoji("( ;ﾟдﾟ)", "( ;ﾟдﾟ)", false, 0))
        default.add(Emoji("( ;´д`)", "( ;´д`)", false, 0))
        default.add(Emoji("(　д ) ﾟ ﾟ", "(　д ) ﾟ ﾟ", false, 0))
        default.add(Emoji("( ☉д⊙)", "( ☉д⊙)", false, 0))
        default.add(Emoji("(((　ﾟдﾟ)))", "(((　ﾟдﾟ)))", false, 0))
        default.add(Emoji("( ` ・´)", "( ` ・´)", false, 0))
        default.add(Emoji("( ´д`)", "( ´д`)", false, 0))
        default.add(Emoji("( -д-)", "( -д-)", false, 0))
        default.add(Emoji("(>д&lt;)", "(>д&lt;)", false, 0))
        default.add(Emoji("･ﾟ( ﾉд`ﾟ)", "･ﾟ( ﾉд`ﾟ)", false, 0))
        default.add(Emoji("( TдT)", "( TдT)", false, 0))
        default.add(Emoji("(￣∇￣)", "(￣∇￣)", false, 0))
        default.add(Emoji("(￣3￣)", "(￣3￣)", false, 0))
        default.add(Emoji("(￣ｰ￣)", "(￣ｰ￣)", false, 0))
        default.add(Emoji("(￣ . ￣)", "(￣ . ￣)", false, 0))
        default.add(Emoji("(￣皿￣)", "(￣皿￣)", false, 0))
        default.add(Emoji("(￣艸￣)", "(￣艸￣)", false, 0))
        default.add(Emoji("(￣︿￣)", "(￣︿￣)", false, 0))
        default.add(Emoji("(￣︶￣)", "(￣︶￣)", false, 0))
        default.add(Emoji("ヾ(´ωﾟ｀)", "ヾ(´ωﾟ｀)", false, 0))
        default.add(Emoji("(*´ω`*)", "(*´ω`*)", false, 0))
        default.add(Emoji("(・ω・)", "(・ω・)", false, 0))
        default.add(Emoji("( ´・ω)", "( ´・ω)", false, 0))
        default.add(Emoji("(｀・ω)", "(｀・ω)", false, 0))
        default.add(Emoji("(´・ω・`)", "(´・ω・`)", false, 0))
        default.add(Emoji("(`・ω・´)", "(`・ω・´)", false, 0))
        default.add(Emoji("( `_っ´)", "( `_っ´)", false, 0))
        default.add(Emoji("( `ー´)", "( `ー´)", false, 0))
        default.add(Emoji("( ´_っ`)", "( ´_っ`)", false, 0))
        default.add(Emoji("( ´ρ`)", "( ´ρ`)", false, 0))
        default.add(Emoji("( ﾟωﾟ)", "( ﾟωﾟ)", false, 0))
        default.add(Emoji("(oﾟωﾟo)", "(oﾟωﾟo)", false, 0))
        default.add(Emoji("(　^ω^)", "(　^ω^)", false, 0))
        default.add(Emoji("(｡◕∀◕｡)", "(｡◕∀◕｡)", false, 0))
        default.add(Emoji("/( ◕‿‿◕ )\\", "/( ◕‿‿◕ )\\", false, 0))
        default.add(Emoji("ヾ(´ε`ヾ)", "ヾ(´ε`ヾ)", false, 0))
        default.add(Emoji("(ノﾟ∀ﾟ)ノ", "(ノﾟ∀ﾟ)ノ", false, 0))
        default.add(Emoji("(σﾟдﾟ)σ", "(σﾟдﾟ)σ", false, 0))
        default.add(Emoji("(σﾟ∀ﾟ)σ", "(σﾟ∀ﾟ)σ", false, 0))
        default.add(Emoji("|дﾟ )", "|дﾟ )", false, 0))
        default.add(Emoji("┃電柱┃", "┃電柱┃", false, 0))
        default.add(Emoji("ﾟ(つд`ﾟ)", "ﾟ(つд`ﾟ)", false, 0))
        default.add(Emoji("ﾟÅﾟ )　", "ﾟÅﾟ )　", false, 0))
        default.add(Emoji("⊂彡☆))д`)", "⊂彡☆))д`)", false, 0))
        default.add(Emoji("⊂彡☆))д´)", "⊂彡☆))д´)", false, 0))
        default.add(Emoji("⊂彡☆))∀`)", "⊂彡☆))∀`)", false, 0))
        default.add(Emoji("(´∀((☆ミつ", "(´∀((☆ミつ", false, 0))
        default.add(Emoji("༼;´༎ຶ ۝ ༎ຶ༽", "༼;´༎ຶ ۝ ༎ຶ༽", false, 0))
        default.add(Emoji("(　˘ω˘)", "(　˘ω˘)", false, 0))
        default.add(Emoji("ᕕ( ᐛ )ᕗ", "ᕕ( ᐛ )ᕗ", false, 0))
        default.add(Emoji("( *・ω・)✄╰ひ╯", "( *・ω・)✄╰ひ╯", false, 0))
        default.add(Emoji("༼　•͟ ͜ •　༽\n༽ つд⊂ ༼", "༼　•͟ ͜ •　༽\n༽ つд⊂ ༼", false, 0))
        default.add(Emoji("(ᯣ ̶̵̵̵̶̶̶̶̵̫̋̋̅̅̅ᯣ )", "(ᯣ ̶̵̵̵̶̶̶̶̵̫̋̋̅̅̅ᯣ )", false, 0))
        default.add(Emoji("[h][/h]", "[h][/h]", false, 0))
        default.add(Emoji("(ﾉ)`ω´(ヾ)", "(ﾉ)`ω´(ヾ)", false, 0))
        default.add(Emoji("( ´◔ ‸◔`)", "( ´◔ ‸◔`)", false, 0))
        default.add(Emoji("( ﾟᯅ 。)", "( ﾟᯅ 。)", false, 0))
        default.add(Emoji("全角空格", "　", false, 0))

        insertAll(default)
    }
}