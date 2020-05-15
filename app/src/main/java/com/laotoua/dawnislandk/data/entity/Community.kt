package com.laotoua.dawnislandk.data.entity

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

@Entity(tableName = "community")
data class Community(

    @PrimaryKey
    @SerializedName("id")
    val id: String,

    @SerializedName("sort")
    val sort: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("forums")
    val forums: List<Forum>
)

@Dao
interface CommunityDao {
    @Query("SELECT * FROM community")
    suspend fun getCommunities(): List<Community>

    @Query("SELECT * FROM community WHERE id==:id")
    suspend fun getCommunityById(id: String): Community

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(communityList: List<Community>)

    @Delete
    suspend fun delete(community: Community)

    @Query("DELETE FROM community")
    fun nukeTable()
}

class ForumListConverter {
    @TypeConverter
    fun listToJson(value: String): List<Forum> {
        return Gson().fromJson(value, object : TypeToken<List<Forum>>() {}.type)
    }

    @TypeConverter
    fun jsonToList(list: List<Forum>): String {
        return Gson().toJson(list)
    }

}