package com.screenslicerpro.gestures.view.viewmodel

import android.Manifest
import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.screenslicerpro.gestures.action.database.AppItem
import com.screenslicerpro.gestures.action.database.AppsDAO
import com.screenslicerpro.utils.MY_VIEW_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GestureSettingsViewModel  (private val database: AppsDAO,
                                 private val sharedPreferences: SharedPreferences?,
                                 private val app: Application) : AndroidViewModel(app) {



    private val _apps= database.getAllApps()
    val apps
        get() = _apps

    private val _shouldBeChecked = MutableLiveData<Boolean>()
    val shouldBeChecked
        get() = _shouldBeChecked

    private val _hasPermission = MutableLiveData<Boolean>()
    val hasPermission
        get() = _hasPermission

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading
        get() = _isLoading

    private val _appCount = MutableLiveData<Int>()
    val appCount
        get() = _appCount

    private val _appItem = MutableLiveData<AppItem>()
    val appItem
        get() = _appItem

    private val editor: SharedPreferences.Editor? = sharedPreferences?.edit()

    init {
       shouldBeChecked()
    }

    fun setIsLoading(isLoading: Boolean){
        _isLoading.value = isLoading
    }

    fun setAppCount(count: Int) {
        _appCount.value = count
    }

    fun shouldBeChecked(){
        checkIfHasPermission(app.applicationContext)
        _shouldBeChecked.value = (sharedPreferences?.getBoolean(MY_VIEW_ID, false) == true
                && _hasPermission.value == true)

    }

    fun setSwitchToChecked(){

        editor?.apply() {
            putBoolean(MY_VIEW_ID, true)
            apply()
        }

        shouldBeChecked()
    }

    fun setSwitchToNotChecked(){
        editor?.apply() {
            putBoolean(MY_VIEW_ID, false)
            apply()
        }

        shouldBeChecked()
    }

     fun checkIfHasPermission(context: Context?):Boolean? {
        val appOps = context?.getSystemService(AppCompatActivity.APP_OPS_SERVICE) as AppOpsManager

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.packageName ?: "com.")
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.packageName ?: "null")
        }


        _hasPermission.value = if (mode == AppOpsManager.MODE_DEFAULT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                PermissionChecker.checkCallingOrSelfPermission(
                    context,
                    Manifest.permission.PACKAGE_USAGE_STATS
                ) == PermissionChecker.PERMISSION_GRANTED
            } else {
                //the permission was granted before the app was installed
                true
            }

        } else {
            (mode == AppOpsManager.MODE_ALLOWED);
        }

         return hasPermission.value

    }

    fun updateAppItem(app: AppItem) {
        viewModelScope.launch {
            onUpdateAppItem(app)
        }
    }



    fun onInsertNewApp(newItem: AppItem){
            viewModelScope.launch {
                if (getAppByPackageName(newItem.packageName) == null){
                    insert(newItem)
                }
            }
    }

    fun onGetAppItemByPackageName(packageName: String) {

        viewModelScope.launch {
           _appItem.value =  getAppByPackageName(packageName)
        }

    }


    private fun onDeleteAll(){
        viewModelScope.launch {
            deleteAll()
        }
    }


    private suspend fun insert(appItem: AppItem) {
        withContext(Dispatchers.IO) {
            database.insertNewApp(appItem)
        }
    }

    private suspend fun onUpdateAppItem(app: AppItem) {
       withContext(Dispatchers.IO){
           database.updateAppItem(app)
       }
    }

    private suspend fun getAppByPackageName(packageName: String): AppItem?{
      return  withContext(Dispatchers.IO) {
            database.getAppByAppPackageName(packageName)
        }
    }


    private suspend fun deleteAll(){
        withContext(Dispatchers.IO){
            database.clearAll()
        }
    }

    fun onDelete(item: AppItem) {
        viewModelScope.launch{
            delete(item)
        }
    }

    private suspend fun delete(appItem: AppItem){
        withContext(Dispatchers.IO){
            database.delete(appItem)
        }
    }


}

