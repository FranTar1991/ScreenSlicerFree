package com.screenslicerpro.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * A database that stores SleepNight information.
 * And a global method to get access to the database.
 *
 * This pattern is pretty much the same for any database,
 * so you can reuse it.
 */
@Database(entities = [ScreenshotItem::class], version = 1, exportSchema = false)
abstract class ScreenshotsDatabase : RoomDatabase() {

    abstract val screenshotsDAO: ScreenshotsDAO
    companion object {
        @Volatile
        private var INSTANCE: ScreenshotsDatabase? = null

        fun getInstance(context: Context): ScreenshotsDatabase {
            synchronized(this) {

                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        ScreenshotsDatabase::class.java,
                        "all_screenshots_database"
                    ).fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}