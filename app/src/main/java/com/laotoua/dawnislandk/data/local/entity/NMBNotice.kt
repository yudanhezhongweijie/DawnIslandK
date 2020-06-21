package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
@Entity
data class NMBNotice(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null, // only used to keep track of versions
    val content: String,
    val date: Long,
    val enable: Boolean,
    var read: Boolean = false,
    var lastUpdatedAt: Long = 0
){
    override fun equals(other: Any?) =
        if (other is NMBNotice) {
            content == other.content && date == other.date
                    && enable == other.enable && read == other.read
        } else false

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + content.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + enable.hashCode()
        result = 31 * result + read.hashCode()
        return result
    }

    fun setUpdatedTimestamp(time: Long? = null) {
        lastUpdatedAt = time ?: Date().time
    }
}