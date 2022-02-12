package com.screenslicerfree.gestures.action.database

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
@Database(entities = [AppItem::class], version = 1, exportSchema = false)
abstract class AllAppsDatabase : RoomDatabase() {

    abstract val appsDAO: AppsDAO
    companion object {
        @Volatile
        private var INSTANCE: AllAppsDatabase? = null

        fun getInstance(context: Context): AllAppsDatabase {
            synchronized(this) {

                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AllAppsDatabase::class.java,
                        "all_apps_database"
                    ).fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}