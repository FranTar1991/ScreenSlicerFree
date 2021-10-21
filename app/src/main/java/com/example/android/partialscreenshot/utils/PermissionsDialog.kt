package com.example.android.partialscreenshot.utils

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.floatingCropWindow.CropViewFloatingWindowService
import com.example.android.partialscreenshot.floatingCropWindow.cropWindow.CropView

class PermissionsDialog(): DialogFragment() {

    // Use this instance of the interface to deliver action events
    private lateinit var listener: NoticeDialogListener

    interface NoticeDialogListener {
        fun onOverlayPositiveClick()
        fun onSavePositiveClick()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            val root = inflater.inflate(R.layout.dialog_permission, null)
            val text = root.findViewById<TextView>(R.id.text_show)
            if (tag.equals(PERMISSION_TO_OVERLAY)){
                text.text = getString(R.string.permission_overlay)
            } else if (tag.equals(PERMISSION_TO_SAVE)){

                text.text = getString(R.string.permission_save)
            }

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(root)
                .setTitle(R.string.title_dialog)
                // Add action buttons
                .setPositiveButton(R.string.gotIt) { _, _ ->
                    if (tag.equals(PERMISSION_TO_OVERLAY)){
                        listener.onOverlayPositiveClick()
                    } else if (tag.equals(PERMISSION_TO_SAVE)){
                        listener.onSavePositiveClick()
                    }

                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }


    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as  NoticeDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() +
                    " must implement NoticeDialogListener"))
        }
    }
}