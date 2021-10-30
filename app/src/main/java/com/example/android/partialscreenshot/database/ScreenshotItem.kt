package com.example.android.partialscreenshot.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "all_screenshots_database_table")
data class ScreenshotItem(
    @PrimaryKey(autoGenerate = true)
    var screenshotID: Long = 0L,
    val uri: String
)