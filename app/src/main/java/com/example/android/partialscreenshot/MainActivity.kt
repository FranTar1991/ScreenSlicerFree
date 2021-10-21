package com.example.android.partialscreenshot

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.Intent.*

import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.android.partialscreenshot.database.ScreenshotItem
import com.example.android.partialscreenshot.database.ScreenshotsDatabase
import com.example.android.partialscreenshot.floatingCropWindow.CropViewFloatingWindowService
import com.example.android.partialscreenshot.main_fragment.MainFragment
import com.example.android.partialscreenshot.main_fragment.MainFragmentViewModel
import com.example.android.partialscreenshot.main_fragment.MainFragmentViewmodelFactory
import com.example.android.partialscreenshot.utils.*
import kotlin.properties.Delegates




//Use this variables instead of OnActivityResult
private lateinit var permissionToShowFloatingWidgetLauncher: ActivityResultLauncher<Intent>
private lateinit var permissionToRecordLauncher: ActivityResultLauncher<Intent>
private lateinit var requestPermissionToSaveLauncher: ActivityResultLauncher<String>


//These are used to connect the FloatingWindowService with it´s calling activity
private var bound by Delegates.notNull<Boolean>()
private lateinit var cropServiceViewFloatingWindowService: CropViewFloatingWindowService

//Variable that holds all to take the screenshots
private var mData: Intent? = null


class MainActivity : AppCompatActivity(), FloatingWindowListener, PermissionsDialog.NoticeDialogListener{

    private lateinit var screenshotViewModel: MainFragmentViewModel
    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var permissionsDialog: PermissionsDialog
    private var receiver: BroadcastReceiver? = null
    private val cropViewFloatingWindowServiceConnection: ServiceConnection = object :
        ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // cast the IBinder and get FloatingWindowService instance
            val binder = service as CropViewFloatingWindowService.LocalBinder
            cropServiceViewFloatingWindowService = binder.getService()
            bound = true
            cropServiceViewFloatingWindowService.setServiceCallBacks(this@MainActivity) // register
            permissionsDialog = PermissionsDialog()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cancelOnCloseBtn()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        viewModel.overlayCall.observe(this, Observer { call ->
            if (call){
                checkIfPermissionToShowOverlay()
            }
        })

        val application = requireNotNull(this).application
        val dataSource = ScreenshotsDatabase.getInstance(application).screenshotsDAO
        val viewModelFactory = MainFragmentViewmodelFactory(dataSource, application)


        screenshotViewModel =
            ViewModelProvider(this, viewModelFactory).get(MainFragmentViewModel::class.java)
        permissionToRecordLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    mData = data

                    callFloatingWindow()

                } else {
                    //show toast explaining that without this permission the app can´t work
                    Toast.makeText(this, "Without this permission we won´t be able to take the screenshots",
                        Toast.LENGTH_SHORT).show()
                }
            }

        permissionToShowFloatingWidgetLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
                checkIfPermissionToShowOverlay()
            }

        requestPermissionToSaveLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted: Boolean ->
                if (isGranted){
                   cropServiceViewFloatingWindowService.screenShotTaker?.saveScreenshot()

                } else {

                    Toast.makeText(this,getString(R.string.cant_save), Toast.LENGTH_SHORT).show()
                }
                cropServiceViewFloatingWindowService.hideCropView(View.VISIBLE)
            }

        //Used to connect this activity with the FloatingWindowService
        val intent = Intent(this, CropViewFloatingWindowService::class.java)
        bindService(intent, cropViewFloatingWindowServiceConnection, BIND_AUTO_CREATE)

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionsDialog()
                .show(supportFragmentManager, PERMISSION_TO_SAVE)
        }


    }

    /**
     * Method used to set up the broadcast receiver that will stop the activity when the "close btn" is
     * called from the notification panel
     */
    private fun cancelOnCloseBtn() {

            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    finishAndRemoveTask()
                }
            }
            val filter = IntentFilter()
            filter.addAction(STOP_INTENT)
            registerReceiver(receiver, filter)


    }


    private fun getPermissionToRecord() {

        val mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager

        permissionToRecordLauncher.launch(mProjectionManager.createScreenCaptureIntent())

    }

    /**
     * If Marshmallow or greater call the floating window service
     * else ask for overlay permission
     */
    private fun checkIfPermissionToShowOverlay(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)) {

            callPermissionToOverlayDialog()

        } else{
            getPermissionToRecord()
        }


    }



    /**
     * make sure to unregister the service when this activity is destroy
     */

    override fun onDestroy() {

            unregisterReceiver(receiver)
            super.onDestroy()
            if (bound) {
                cropServiceViewFloatingWindowService.stopForeground(true)
                cropServiceViewFloatingWindowService.setServiceCallBacks(null) // unregister
                unbindService(cropViewFloatingWindowServiceConnection)
                bound = false
            }

            stopService(Intent(this, CropViewFloatingWindowService::class.java))

    }

    /**
     * This method should be called by the floating window service
     */
    override fun checkIfPermissionToSave(): Boolean {
        return PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(
                   this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    /**
     * @return mData, which contains the permission used to take the screenshot
     */
    override fun getDataToRecordScreen(): Intent? {
        return mData
    }

    private fun callPermissionToOverlayDialog() {
            permissionsDialog.show(supportFragmentManager, PERMISSION_TO_OVERLAY)

    }

    fun callPermissionToSaveDialog(){
        requestPermissionToSaveLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    }


    private fun callFloatingWindow() {

            val intent = Intent(this, CropViewFloatingWindowService::class.java)
            startService(intent)

    }

    override fun onOverlayPositiveClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val myIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${packageName}")
            )
            permissionToShowFloatingWidgetLauncher.launch(myIntent)
        }

    }

    override fun onSavePositiveClick() {
        // You can directly ask for the permission.
        // The registered ActivityResultCallback gets the result of this request.

        requestPermissionToSaveLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    }

    fun saveScreenshotWIthPermission(uriToSave: String){
     screenshotViewModel.onSaveScreenshot(ScreenshotItem(screenshotURI= uriToSave))

    }


}