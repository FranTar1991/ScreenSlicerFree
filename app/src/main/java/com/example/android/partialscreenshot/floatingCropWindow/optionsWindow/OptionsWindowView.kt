package com.example.android.partialscreenshot.floatingCropWindow.optionsWindow
import android.content.Context
import android.content.Context.WINDOW_SERVICE

import android.graphics.PixelFormat
import android.util.Log

import android.view.*
import android.widget.ImageView
import com.example.android.partialscreenshot.R
import android.view.WindowManager

import android.view.Gravity

import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.example.android.partialscreenshot.databinding.OptionsViewBinding
import com.example.android.partialscreenshot.floatingCropWindow.cropWindow.CropView
import com.example.android.partialscreenshot.utils.layoutFlag
import com.example.android.partialscreenshot.utils.OnOptionsWindowSelectedListener


class OptionsWindowView (private val context: Context,
                         private val cropView: CropView?) : View.OnTouchListener {
    private var mWindowManager: WindowManager? = null
    private var mFloatingView: OptionsViewBinding? = null
    private var saveButton: ImageView? = null
    private var deleteButton: ImageView? = null
    private var shareButton: ImageView? = null
    private var minMaxButton: ImageView? = null
    private var editButton: ImageView? = null
    private var saveButtonBc: ImageView? = null
    private var deleteButtonBc: ImageView? = null
    private var shareButtonBc: ImageView? = null
    private var minMaxButtonBk: ImageView? = null
    private var editButtonBc: ImageView? = null
    private var onOptionsWindowSelectedListener: OnOptionsWindowSelectedListener? = null
    private var mainContainer: ConstraintLayout? = null


    fun createView() {
    Log.i("MyView","creating view")
        mFloatingView = OptionsViewBinding.inflate(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                as LayoutInflater)

        //Add the view to the window.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        //Specify the view position

        //Specify the view position
        params.gravity =
            Gravity.CENTER or Gravity.END //Initially view will be added to top-left corner

        //Add the view to the window

        //Add the view to the window
        mWindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        mWindowManager?.addView(mFloatingView?.root, params)

       saveButton = mFloatingView?.save?.apply { setOnTouchListener(this@OptionsWindowView) }
       deleteButton =  mFloatingView?.delete?.apply { setOnTouchListener(this@OptionsWindowView) }
       shareButton = mFloatingView?.share?.apply { setOnTouchListener(this@OptionsWindowView) }
       minMaxButton = mFloatingView?.minMax?.apply { setOnTouchListener(this@OptionsWindowView) }
       editButton = mFloatingView?.edit?.apply { setOnTouchListener(this@OptionsWindowView) }
        mainContainer = mFloatingView?.mainBtnContainer

        saveButtonBc = mFloatingView?.saveBk
        deleteButtonBc =  mFloatingView?.deleteBk
        shareButtonBc = mFloatingView?.shareBk
        minMaxButtonBk = mFloatingView?.minMaxBk
        editButtonBc= mFloatingView?.editBk

        cropView?.thisOptionsView = this
    }

    fun destroyView() {
        mFloatingView?.root?.let {
            if(it.isAttachedToWindow){
                if (mFloatingView != null) mWindowManager?.removeView(mFloatingView?.root)
            }
        }


    }

    fun setOnOnOptionsWindowSelected(onOptionsWindowSelectedListener: OnOptionsWindowSelectedListener){
        this.onOptionsWindowSelectedListener = onOptionsWindowSelectedListener
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {

        when(event?.actionMasked){
            ACTION_DOWN -> { setChangeColor(v?.id, ACTION_DOWN)}
            ACTION_UP -> { setChangeColor(v?.id, ACTION_UP) }
        }

        return true
    }

    private fun setChangeColor(id: Int?, action: Int) {

        val downColor = R.color.teal_200
        val upColor = R.color.teal_700

        if (action == ACTION_DOWN){
            when(id){
                R.id.save ->{ setTint(saveButtonBc!!, downColor) }
                R.id.delete ->{   setTint(deleteButtonBc!!, downColor)}
                R.id.share ->{  setTint(shareButtonBc!!, downColor)}
                R.id.edit ->{  setTint(editButtonBc!!, downColor)}
                R.id.min_max ->{   setTint(minMaxButtonBk!!, downColor)}
            }
        } else{
            when(id){
                R.id.save ->{ setTint(saveButtonBc!!, upColor) }
                R.id.delete ->{   setTint(deleteButtonBc!!, upColor)}
                R.id.share ->{  setTint(shareButtonBc!!, upColor)}
                R.id.edit ->{  setTint(editButtonBc!!, upColor)}
                R.id.min_max ->{   setTint(minMaxButtonBk!!, upColor)}
            }
            onTouchButton(id)
        }
    }

    private fun setTint(button: ImageView, color: Int) {
        DrawableCompat.setTint(
            DrawableCompat.wrap(button.drawable),
            ContextCompat.getColor(context, color)
        )
    }

     private fun onTouchButton(id: Int?) {

        when(id){
            R.id.save ->{ onOptionsWindowSelectedListener?.onSaveScreenshotSelected()}
            R.id.delete ->{onOptionsWindowSelectedListener?.onDeleteScreenshotSelected()}
            R.id.share ->{onOptionsWindowSelectedListener?.onShareScreenshotSelected()}
            R.id.edit ->{onOptionsWindowSelectedListener?.onEditScreenshotSelected()}
            R.id.min_max  ->{setMinMax()}
        }
    }

    private fun setMinMax() {
        if (mainContainer?.isVisible == true){
            mainContainer?.visibility = View.GONE
            minMaxButton?.setImageResource(R.drawable.ic_max)
        } else {
            mainContainer?.visibility = View.VISIBLE
            minMaxButton?.setImageResource(R.drawable.ic_min)
        }
    }

    fun hideOptionsView(visibility: Int){
        mFloatingView?.root?.visibility = visibility
    }
}