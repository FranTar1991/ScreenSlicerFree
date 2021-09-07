package com.example.android.partialscreenshot

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.example.android.partialscreenshot.databinding.ActivityMainBinding
import com.example.android.partialscreenshot.floatingCropWindow.FloatingWindowService
import com.example.android.partialscreenshot.utils.FloatingWindowListener
import kotlin.properties.Delegates

//Use this variables instead of OnActivityResult
private lateinit var permissionToShowFloatingWidgetLauncher: ActivityResultLauncher<Intent>
private lateinit var permissionToRecordLauncher: ActivityResultLauncher<Intent>

//These are used to connect the FloatingWindowService with it´s calling activity
private var bound by Delegates.notNull<Boolean>()
private lateinit var floatingWindowServiceService: FloatingWindowService

//Variable that holds all to take the screenshots
private var mData: Intent? = null

class MainActivity : AppCompatActivity(), FloatingWindowListener {

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // cast the IBinder and get FloatingWindowService instance
            val binder = service as FloatingWindowService.LocalBinder
            floatingWindowServiceService = binder.getService()
            bound = true
            floatingWindowServiceService.setServiceCallBacks(this@MainActivity) // register
        }



        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this,R.layout.activity_main)

         permissionToRecordLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    mData = data
                }
            }

        permissionToShowFloatingWidgetLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            startService(Intent(this, FloatingWindowService::class.java))
        }

        binding.callFloatingWindow.setOnClickListener(View.OnClickListener {
            callFloatingWindow()
        })

        //Used to connect this activity with the FloatingWindowService
        val intent = Intent(this, FloatingWindowService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

        //This method should be deleted once we can the get permission from the floating window service
        getPermission()

    }

    private fun getPermission() {

        val mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager

        permissionToRecordLauncher.launch(mProjectionManager.createScreenCaptureIntent())

    }

    /**
     * If Marshmallow or greater call the floating window service
     * else ask for overlay permission
     */
    private fun callFloatingWindow() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            permissionToRecordLauncher.launch(intent)

        } else {

            startService(Intent(this, FloatingWindowService::class.java))

        }
    }

    /**
     * make sure to unregister the service when this activity is destroy
     */

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            floatingWindowServiceService.setServiceCallBacks(null) // unregister
            unbindService(serviceConnection)
            bound = false
        }
    }


    /**
     * This method should be called by the floating window service
     */
    override fun getPermissionToRecordScreen() {
        val mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager

        permissionToRecordLauncher.launch(mProjectionManager.createScreenCaptureIntent())
    }

    /**
     * @return mData, which contains the permission used to take the screenshot
     */
    override fun getDataToRecordScreen(): Intent? {
        return mData;
    }
}