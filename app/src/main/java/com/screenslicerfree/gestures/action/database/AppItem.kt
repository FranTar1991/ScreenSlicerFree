package com.screenslicerfree.gestures.action.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "all_apps_database_table")
data class AppItem(
    @PrimaryKey(autoGenerate = true)
    var appId: Long = 0L,
    var isAllowed: Boolean = true,
    val packageName: String,
    val appIconUri: String?,
    val appName: String,
)