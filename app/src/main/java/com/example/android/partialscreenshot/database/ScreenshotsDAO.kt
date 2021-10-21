package com.example.android.partialscreenshot.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScreenshotsDAO {
    @Insert
    suspend fun insertScreenshot(screenshot: ScreenshotItem)

    @Query("SELECT * FROM all_screenshots_database_table ORDER BY screenshotID DESC")
    fun getAllScreenshots(): LiveData<List<ScreenshotItem>>

    @Query("SELECT * from all_screenshots_database_table WHERE screenshotID = :key")
    fun get(key: Long): LiveData<ScreenshotItem>

    @Query("DELETE FROM all_screenshots_database_table")
    suspend fun clearAll()

    @Query("DELETE FROM all_screenshots_database_table WHERE screenshotID = :key")
    suspend fun clear(key: Long)

}
