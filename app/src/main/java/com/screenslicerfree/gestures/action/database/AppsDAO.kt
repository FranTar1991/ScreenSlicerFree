package com.screenslicerfree.gestures.action.database

import androidx.lifecycle.LiveData
import androidx.room.*


@Dao
interface AppsDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewApp(newAppItem: AppItem)

    @Query("SELECT * FROM all_apps_database_table ORDER BY appId DESC")
    fun getAllApps(): LiveData<List<AppItem>>


    @Query("SELECT * FROM all_apps_database_table WHERE packageName IN (:packageName)")
    suspend fun getAppByAppPackageName(packageName: String): AppItem?

    @Update
    suspend fun updateAppItem(appItem: AppItem)

    @Query("DELETE FROM all_apps_database_table")
    suspend fun clearAll()

    @Delete
    suspend fun delete(appItem: AppItem)


}