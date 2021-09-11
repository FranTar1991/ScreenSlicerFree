package com.example.android.partialscreenshot.floatingCropWindow.optionsWindow
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.Bitmap

import android.graphics.PixelFormat

import android.view.*
import android.widget.ImageView
import com.example.android.partialscreenshot.R
import android.view.WindowManager

import android.view.Gravity

import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.android.partialscreenshot.databinding.OptionsViewBinding
import com.example.android.partialscreenshot.floatingCropWindow.cropWindow.CropView
import com.example.android.partialscreenshot.utils.OnOptionsWindowSelectedListener


class OptionsWindowView (private val context: Context,
                         private val bitmap: Bitmap,
                         private val cropView: CropView) : View.OnTouchListener {
    private var mWindowManager: WindowManager? = null
    private var mFloatingView: OptionsViewBinding? = null
    private var saveButton: ImageView? = null
    private var deleteButton: ImageView? = null
    private var shareButton: ImageView? = null
    private var addNoteButton: ImageView? = null
    private var editButton: ImageView? = null
    private var saveButtonBc: ImageView? = null
    private var deleteButtonBc: ImageView? = null
    private var shareButtonBc: ImageView? = null
    private var addNoteButtonBc: ImageView? = null
    private var editButtonBc: ImageView? = null
    private var onOptionsWindowSelectedListener: OnOptionsWindowSelectedListener? = null

    fun OptionsWindowView(){}

    fun createView() {

        mFloatingView = OptionsViewBinding.inflate(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                as LayoutInflater)

        //Add the view to the window.

        //Add the view to the window.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        //Specify the view position

        //Specify the view position
        params.gravity =
            Gravity.BOTTOM or Gravity.END //Initially view will be added to top-left corner

        //Add the view to the window

        //Add the view to the window
        mWindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        mWindowManager?.addView(mFloatingView?.root, params)

       saveButton = mFloatingView?.save?.apply { setOnTouchListener(this@OptionsWindowView) }
       deleteButton =  mFloatingView?.delete?.apply { setOnTouchListener(this@OptionsWindowView) }
       shareButton = mFloatingView?.share?.apply { setOnTouchListener(this@OptionsWindowView) }
       addNoteButton = mFloatingView?.addNote?.apply { setOnTouchListener(this@OptionsWindowView) }
       editButton = mFloatingView?.edit?.apply { setOnTouchListener(this@OptionsWindowView) }

        saveButtonBc = mFloatingView?.saveBk
        deleteButtonBc =  mFloatingView?.deleteBk
        shareButtonBc = mFloatingView?.shareBk
        addNoteButtonBc = mFloatingView?.addNoteBk
        editButtonBc= mFloatingView?.editBk

        cropView.thisOptionsView = this
    }

    fun onDestroy() {
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

        val downColor = R.color.teal_700
        val upColor = R.color.teal_200

        if (action == ACTION_DOWN){
            when(id){
                R.id.save ->{ setTint(saveButtonBc!!, downColor) }
                R.id.delete ->{   setTint(deleteButtonBc!!, downColor)}
                R.id.share ->{  setTint(shareButtonBc!!, downColor)}
                R.id.add_note ->{  setTint(addNoteButtonBc!!, downColor)}
                R.id.edit ->{   setTint(editButtonBc!!, downColor)}
            }
        } else{
            when(id){
                R.id.save ->{ setTint(saveButtonBc!!, upColor) }
                R.id.delete ->{   setTint(deleteButtonBc!!, upColor)}
                R.id.share ->{  setTint(shareButtonBc!!, upColor)}
                R.id.add_note ->{  setTint(addNoteButtonBc!!, upColor)}
                R.id.edit ->{   setTint(editButtonBc!!, upColor)}
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
            R.id.save ->{ onOptionsWindowSelectedListener?.onSaveScreenshot(); onDestroy() }
            R.id.delete ->{onOptionsWindowSelectedListener?.onDeleteScreenshot()}
            R.id.share ->{onOptionsWindowSelectedListener?.onShareScreenshot()}
            R.id.add_note ->{onOptionsWindowSelectedListener?.onAddNoteToScreenshot()}
            R.id.edit ->{onOptionsWindowSelectedListener?.onEditScreenshot()}
        }
    }
}