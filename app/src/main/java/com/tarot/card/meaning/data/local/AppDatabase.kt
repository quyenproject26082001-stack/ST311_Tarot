package com.tarot.card.meaning.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tarot.card.meaning.data.local.dao.AppSettingDao
import com.tarot.card.meaning.data.local.dao.CharacterDao
import com.tarot.card.meaning.data.local.dao.JsonCacheDao
import com.tarot.card.meaning.data.local.dao.MediaItemDao
import com.tarot.card.meaning.data.local.dao.SavedAvatarDao
import com.tarot.card.meaning.data.local.entity.AppSettingEntity
import com.tarot.card.meaning.data.local.entity.CharacterColorEntity
import com.tarot.card.meaning.data.local.entity.CharacterEntity
import com.tarot.card.meaning.data.local.entity.CharacterLayerEntity
import com.tarot.card.meaning.data.local.entity.CharacterLayerGroupEntity
import com.tarot.card.meaning.data.local.entity.JsonCacheEntity
import com.tarot.card.meaning.data.local.entity.MediaItemEntity
import com.tarot.card.meaning.data.local.entity.SavedAvatarEntity

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
