package com.example.android.partialscreenshot

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
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
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.android.partialscreenshot.databinding.ActivityMainBinding
import com.example.android.partialscreenshot.floatingCropWindow.CropViewFloatingWindowService
import com.example.android.partialscreenshot.utils.FloatingWindowListener
import kotlin.properties.Delegates

import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import com.example.android.partialscreenshot.utils.PERMISSION_TO_OVERLAY
import com.example.android.partialscreenshot.utils.PERMISSION_TO_SAVE
import com.example.android.partialscreenshot.utils.PermissionsDialog


//Use this variables instead of OnActivityResult
private lateinit var permissionToShowFloatingWidgetLauncher: ActivityResultLauncher<Intent>
private lateinit var permissionToRecordLauncher: ActivityResultLauncher<Intent>
private lateinit var requestPermissionToSaveLauncher: ActivityResultLauncher<String>


//These are used to connect the FloatingWindowService with it´s calling activity
private var bound by Delegates.notNull<Boolean>()
private lateinit var cropServiceViewFloatingWindowService: CropViewFloatingWindowService

//Variable that holds all to take the screenshots
private var mData: Intent? = null
public var hasPermissionToSave = false
val permissionsDialog = PermissionsDialog()

class MainActivity : AppCompatActivity(), FloatingWindowListener, PermissionsDialog.NoticeDialogListener {

    private val cropViewFloatingWindowServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // cast the IBinder and get FloatingWindowService instance
            val binder = service as CropViewFloatingWindowService.LocalBinder
            cropServiceViewFloatingWindowService = binder.getService()
            bound = true
            cropServiceViewFloatingWindowService.setServiceCallBacks(this@MainActivity) // register
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this,R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
         permissionToRecordLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    mData = data

                    callFloatingWindow()

                } else {
                    //show toast explaining that without this permission the app can´t work
                    Log.i("NotGranted", "permission to record not granted")
                }
            }

         permissionToShowFloatingWidgetLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result ->
                checkIfPermissionToShowOverlay()
            }

        requestPermissionToSaveLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
                if (isGranted){
                    cropServiceViewFloatingWindowService.screenShotTaker.onSaveScreenshot()
                } else {
                    Toast.makeText(this,"Sorry, you won´t be able to save any screenshots then",Toast.LENGTH_SHORT).show()
                }
        }


        binding.callFloatingWindow.setOnClickListener(View.OnClickListener {
            checkIfPermissionToShowOverlay()
        })

        //Used to connect this activity with the FloatingWindowService
        val intent = Intent(this, CropViewFloatingWindowService::class.java)
        bindService(intent, cropViewFloatingWindowServiceConnection, BIND_AUTO_CREATE)

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
        super.onDestroy()
        if (bound) {
            cropServiceViewFloatingWindowService.setServiceCallBacks(null) // unregister
            unbindService(cropViewFloatingWindowServiceConnection)
            bound = false
        }
    }

    /**
     * This method should be called by the floating window service
     */
    override fun checkIfPermissionToSave(): Boolean {
      return  if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          true
        }
        else {
          callPermissionToSaveDialog()
          false
        }
    }

    /**
     * @return mData, which contains the permission used to take the screenshot
     */
    override fun getDataToRecordScreen(): Intent? {
        return mData;
    }

    private fun callPermissionToOverlayDialog() {
        permissionsDialog.show(supportFragmentManager, PERMISSION_TO_OVERLAY)
    }

    private fun callPermissionToSaveDialog(){
        permissionsDialog.show(supportFragmentManager, PERMISSION_TO_SAVE)
    }


    private fun callFloatingWindow() {
        val intent = Intent(this@MainActivity, CropViewFloatingWindowService::class.java)
        startService(intent)
    }

    override fun onOverlayPositiveClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val myIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            permissionToShowFloatingWidgetLauncher.launch(myIntent)
        }

    }

    override fun onSavePositiveClick() {
        // You can directly ask for the permission.
        // The registered ActivityResultCallback gets the result of this request.
        requestPermissionToSaveLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    }

}