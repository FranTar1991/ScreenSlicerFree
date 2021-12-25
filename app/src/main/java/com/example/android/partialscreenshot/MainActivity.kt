package com.example.android.partialscreenshot

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.Intent.*

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.android.partialscreenshot.database.ScreenshotItem
import com.example.android.partialscreenshot.database.ScreenshotsDatabase
import com.example.android.partialscreenshot.floatingCropWindow.CropViewFloatingWindowService
import com.example.android.partialscreenshot.floatingImageView.FloatingImageViewService
import com.example.android.partialscreenshot.main_fragment.MainFragmentViewModel
import com.example.android.partialscreenshot.main_fragment.MainFragmentViewmodelFactory
import com.example.android.partialscreenshot.utils.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.properties.Delegates

import android.content.ClipData
import com.example.android.partialscreenshot.floatingCropWindow.optionsWindow.OptionsWindowView


//Use this variables instead of OnActivityResult
private lateinit var permissionToShowFloatingWidgetLauncher: ActivityResultLauncher<Intent>
private lateinit var permissionToRecordLauncher: ActivityResultLauncher<Intent>
private lateinit var requestPermissionToSaveLauncher: ActivityResultLauncher<String>




//Variable that holds all to take the screenshots
private var mData: Intent? = null


class MainActivity : AppCompatActivity(), FloatingWindowListener, PermissionsDialog.NoticeDialogListener{


    private lateinit var screenshotViewModel: MainFragmentViewModel
    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var permissionsDialog: PermissionsDialog
    private var myBroadcastReceiverToClose: BroadcastReceiver? = null

    //These are used to connect the Crop window service with it´s calling activity
    private var bound by Delegates.notNull<Boolean>()
    private lateinit var cropServiceViewFloatingWindowService: CropViewFloatingWindowService
    private val cropViewFloatingWindowServiceConnection: ServiceConnection = object :
        ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // cast the IBinder and get FloatingWindowService instance
            val binder = service as CropViewFloatingWindowService.LocalBinder
            cropServiceViewFloatingWindowService = binder.getService()
            bound = true

            cropServiceViewFloatingWindowService.setServiceCallBacks(this@MainActivity, "connected") // register
            permissionsDialog = PermissionsDialog()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }

    }

    //These are used to connect the Floating image window service with it´s calling activity
    private lateinit var floatingImageViewService: FloatingImageViewService
    private var floatingImageViewServiceBound by Delegates.notNull<Boolean>()
    private val floatingImageViewServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as FloatingImageViewService.LocalBinder
            floatingImageViewService = binder.getService()
            floatingImageViewServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            floatingImageViewServiceBound = false
        }
    }

    companion object {
        var currentPosition: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.Theme_PartialScreenshot)

        setContentView(R.layout.activity_main)
        closeAppFromNotification()

        val application = requireNotNull(this).application
        val dataSource = ScreenshotsDatabase.getInstance(application).screenshotsDAO
        val viewModelFactory = MainFragmentViewmodelFactory(dataSource, application)




        screenshotViewModel =
            ViewModelProvider(this, viewModelFactory).get(MainFragmentViewModel::class.java)

        setLaunchers()

       bindTheServices()

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionsDialog()
                .show(supportFragmentManager, PERMISSION_TO_SAVE)
        }

        viewModel.overlayCall.observe(this, Observer { call ->
            if (call){
                decideWhatToShow()
                viewModel.checkIfOverlayPermissionDone()
            }
        })
        viewModel.imageInFloatingWindow.observe(this, Observer {

            it?.let{
                floatingImageViewService.setImageUri(Uri.parse(it))
                viewModel.checkIfHasOverlayPermission(true)

            }



        })

    }

    /**
     * bind the crop window service and the floating image window service so we can
     * show the floating image or take the partial screenshots
     */
    private fun bindTheServices() {

        //Used to connect this activity with the FloatingWindowService
        val intent = Intent(this, CropViewFloatingWindowService::class.java)
        bindService(intent, cropViewFloatingWindowServiceConnection, BIND_AUTO_CREATE)

        //Used to connect this activity with the FloatingImageViewService
        val floatingImageViewIntent = Intent(this, FloatingImageViewService::class.java)
        bindService(floatingImageViewIntent, floatingImageViewServiceConnection, BIND_AUTO_CREATE)
    }

    /**
     * These are the methods to be called when the permissions are accepted or denied
     */
    private fun setLaunchers() {

        //Checks weather we got the permission to take screenshots or not
        //if we did  get the permission we set the variable mData and then call crop window
        //if we don´t get the permission we just show a test
        permissionToRecordLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    mData = data

                    callCropWindow()

                } else {
                    //show toast explaining that without this permission the app can´t work
                    Toast.makeText(this, "Without this permission we won´t be able to take the screenshots",
                        Toast.LENGTH_SHORT).show()
                }
            }


        //If we got the permission to show the floating widget we go to the next method which will decide
        // if the user wants to show a floating image or if we should continue to get the authorization to take the scrrenshot
        permissionToShowFloatingWidgetLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
                decideWhatToShow()
            }

        //This is what gets called when we can the response back from the user´s choice, if the permission to save
        // is granted we continue if not a toast is shown to tell the user that they won´t be able to save any screenshot
        requestPermissionToSaveLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted: Boolean ->
                if (isGranted){
                    cropServiceViewFloatingWindowService.screenShotTaker?.saveScreenshot()

                } else {

                    Toast.makeText(this,getString(R.string.cant_save), Toast.LENGTH_SHORT).show()
                }
                viewModel.setPermissionToSaveCalled()
                cropServiceViewFloatingWindowService.hideCropView(View.VISIBLE)
            }
    }

    /**
     * if the imageUri is null then the user wants to show the crop window so we need to check if we have the proper
     * permissions for that, then if it is not null it means that the user wants to show an image in a floating window
     *
     */
    private fun decideWhatToShow() {

        val imageUri= viewModel.imageInFloatingWindow.value
        val permission = checkIfPermissionToShowOverlay()

        if(permission && imageUri == null){
            hasPermissionToTakeScreenshot()
        } else if(permission && imageUri != null){
            callFloatingImageView()
            viewModel.setFloatingImageViewUriDone()
        }
    }

    /**
     * checks whether we have the permission to take the screenshot to continue, or if not we ask for it.
     */
    private fun hasPermissionToTakeScreenshot() {

        if (mData==null){
            getPermissionToRecord()
        } else {
            callCropWindow()
        }

    }

    private fun callFloatingImageView() {
        Intent(this, FloatingImageViewService::class.java).also { intent ->
            startService(intent)
        }
    }

    /**
     * Method used to set up the broadcast receiver that will stop the activity when the "X btn" is
     * called from the notification panel
     */
    private fun closeAppFromNotification() {

            myBroadcastReceiverToClose = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    finishAndRemoveTask()
                }
            }
            val filter = IntentFilter()
            filter.addAction(STOP_INTENT)
            registerReceiver(myBroadcastReceiverToClose, filter)


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
    private fun checkIfPermissionToShowOverlay(): Boolean{

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            callPermissionToOverlayDialog()
            false

        } else{
            true
        }


    }



    /**
     * make sure to unregister the service when this activity is destroy
     */

    override fun onDestroy() {

            unregisterReceiver(myBroadcastReceiverToClose)
            super.onDestroy()
            if (bound) {
                cropServiceViewFloatingWindowService.stopForeground(true)
                cropServiceViewFloatingWindowService.setServiceCallBacks(null, "destroy") // unregister

                unbindService(cropViewFloatingWindowServiceConnection)
                bound = false
            }

            if (floatingImageViewServiceBound){
                unbindService(floatingImageViewServiceConnection)
                floatingImageViewServiceBound = false
            }

            stopService(Intent(this, CropViewFloatingWindowService::class.java))

    }

    override fun onBackPressed() {
        super.onBackPressed()
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

    /**
     * Call the custom dialog to first explain why we need
     * the permission to draw over other apps and then  ask for the permission.
     */

    private fun callPermissionToOverlayDialog() {
            permissionsDialog.show(supportFragmentManager, PERMISSION_TO_OVERLAY)

    }

    /**
     * Call the system dialog to ask for the permission to save
     */
    fun callPermissionToSaveDialog(){
        requestPermissionToSaveLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }


    /**
     * Call the crop window, this is the final step and before calling this we have to make sure that all the permissions
     * have been granted.
     */
    private fun callCropWindow() {
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

    fun saveScreenshotWIthPermission(uri: String, name: String?){
     screenshotViewModel.onSaveScreenshot(ScreenshotItem(uri = uri, name = name ?: "no name"))
    }

    fun getTextFromImage(image: Bitmap, optionsWindowView: OptionsWindowView?){
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(image, 0)
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                optionsWindowView?.showExtractedText(visionText.text)
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext,getString(R.string.could_not_extract_text), Toast.LENGTH_LONG).show()
            }

    }



}