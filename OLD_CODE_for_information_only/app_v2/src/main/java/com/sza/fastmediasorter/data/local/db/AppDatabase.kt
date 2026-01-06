package com.sza.fastmediasorter.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ResourceEntity::class,
        NetworkCredentialsEntity::class,
        ResourceFtsEntity::class,
        FavoritesEntity::class,
        PlaybackPositionEntity::class,
        ThumbnailCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resourceDao(): ResourceDao
    abstract fun networkCredentialsDao(): NetworkCredentialsDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun playbackPositionDao(): PlaybackPositionDao
    abstract fun thumbnailCacheDao(): ThumbnailCacheDao
}
