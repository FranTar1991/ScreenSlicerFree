package com.screenslicerfree.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DAOScrenshots {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenshot(screenshot: ScreenshotItem): Long

    @Query("SELECT * FROM all_screenshots_database_table ORDER BY screenshotID DESC")
    fun getAllScreenshots(): LiveData<List<ScreenshotItem>>

    @Query("SELECT * from all_screenshots_database_table WHERE uri in (:key)")
    fun getByUri(key: String): LiveData<ScreenshotItem>


    @Query("DELETE FROM all_screenshots_database_table WHERE uri in (:uriList)")
    suspend fun clearAllByUri(uriList: List<String>)

}