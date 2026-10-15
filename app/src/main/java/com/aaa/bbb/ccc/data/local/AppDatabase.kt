package com.aaa.bbb.ccc.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aaa.bbb.ccc.data.local.dao.AppSettingDao
import com.aaa.bbb.ccc.data.local.dao.CharacterDao
import com.aaa.bbb.ccc.data.local.dao.JsonCacheDao
import com.aaa.bbb.ccc.data.local.dao.MediaItemDao
import com.aaa.bbb.ccc.data.local.dao.SavedAvatarDao
import com.aaa.bbb.ccc.data.local.entity.AppSettingEntity
import com.aaa.bbb.ccc.data.local.entity.CharacterColorEntity
import com.aaa.bbb.ccc.data.local.entity.CharacterEntity
import com.aaa.bbb.ccc.data.local.entity.CharacterLayerEntity
import com.aaa.bbb.ccc.data.local.entity.CharacterLayerGroupEntity
import com.aaa.bbb.ccc.data.local.entity.JsonCacheEntity
import com.aaa.bbb.ccc.data.local.entity.MediaItemEntity
import com.aaa.bbb.ccc.data.local.entity.SavedAvatarEntity

@Database(
    entities = [
        AppSettingEntity::class,
        JsonCacheEntity::class,
        SavedAvatarEntity::class,
        MediaItemEntity::class,
        CharacterEntity::class,
        CharacterLayerGroupEntity::class,
        CharacterLayerEntity::class,
        CharacterColorEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appSettingDao(): AppSettingDao
    abstract fun jsonCacheDao(): JsonCacheDao
    abstract fun savedAvatarDao(): SavedAvatarDao
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun characterDao(): CharacterDao
}
