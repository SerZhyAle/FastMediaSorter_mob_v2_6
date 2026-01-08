package com.sza.fastmediasorter.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for FastMediaSorter.
 */

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add pinCode and supportedMediaTypes columns to resources table
        database.execSQL("ALTER TABLE resources ADD COLUMN pinCode TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE resources ADD COLUMN supportedMediaTypes INTEGER NOT NULL DEFAULT 0")
    }
}
