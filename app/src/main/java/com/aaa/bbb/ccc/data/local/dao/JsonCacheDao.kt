package com.aaa.bbb.ccc.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aaa.bbb.ccc.data.local.entity.JsonCacheEntity

@Dao
interface JsonCacheDao {
    @Query("SELECT json FROM json_cache WHERE `key` = :key LIMIT 1")
    suspend fun getJson(key: String): String?

    @Query("SELECT COUNT(*) FROM json_cache WHERE `key` = :key")
    suspend fun countKey(key: String): Int

    @Upsert
    suspend fun upsert(cache: JsonCacheEntity)

    @Query("DELETE FROM json_cache WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
}
