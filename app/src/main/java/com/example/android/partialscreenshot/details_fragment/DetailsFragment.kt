package com.example.android.partialscreenshot.details_fragment

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.database.ScreenshotsDatabase
import com.example.android.partialscreenshot.databinding.FragmentDetailsBinding
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.android.partialscreenshot.MainActivity
import com.example.android.partialscreenshot.database.ScreenshotsDAO
import com.example.android.partialscreenshot.utils.editScreenShot
import com.example.android.partialscreenshot.utils.shareScreenShot


class DetailsFragment : Fragment() {

    private lateinit var myItemPassed: DetailsFragmentArgs
    private lateinit var dataSource: ScreenshotsDAO
    private lateinit var optionsView: ConstraintLayout
    private lateinit var screenshotDetailViewModel: DetailsViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentDetailsBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_details, container, false)

        val application = requireNotNull(this.activity).application

        myItemPassed = DetailsFragmentArgs.fromBundle(requireArguments())

        // Create an instance of the ViewModel Factory.
        dataSource = ScreenshotsDatabase.getInstance(application).screenshotsDAO
        val viewModelFactory = SleepDetailViewModelFactory(myItemPassed.screenshotId, dataSource)

        optionsView = binding.optionsViewContraint

        // Get a reference to the ViewModel associated with this fragment.
        screenshotDetailViewModel =
            ViewModelProvider(
                this, viewModelFactory).get(DetailsViewModel::class.java)

        // To use the View Model with data binding, you have to explicitly
        // give the binding object a reference to it.
        binding.screenshotDetailViewModel = screenshotDetailViewModel


        binding.mainImageDetails.setOnClickListener(View.OnClickListener {
            setUpVisibility()
        })

        setUpDeleteListeners()
        setUpEditListeners()
        setupShareListeners()

        binding.lifecycleOwner = activity

        return binding.root
    }

    private fun setupShareListeners() {
        screenshotDetailViewModel.shareScreenshot.observe(viewLifecycleOwner, Observer {
            it?.let {
                shareScreenShot(context,Uri.parse(it.uri),activity as MainActivity)
                screenshotDetailViewModel.onActionFlagReceived()
            }
        })
    }

    private fun setUpEditListeners() {
        screenshotDetailViewModel.editScreenshot.observe(viewLifecycleOwner, Observer {
            it?.let {
                editScreenShot(Uri.parse(it.uri), activity as MainActivity)
                screenshotDetailViewModel.onActionFlagReceived()
            }
        })
    }

    private fun setUpDeleteListeners() {

        screenshotDetailViewModel.authorizationToMakeChanges.observe(viewLifecycleOwner, Observer {
            it?.let {
                getUserAuthorizationToDelete(it)
                screenshotDetailViewModel.onAskAuthorization(null)
            }


        })

        screenshotDetailViewModel.screenshot.observe(viewLifecycleOwner, Observer {
            if (it == null){
                screenshotDetailViewModel.onNavigateToMainFragment()
            }
        })

        screenshotDetailViewModel.navigateToMainFragment.observe(viewLifecycleOwner, Observer {
            it?.let {
                this.findNavController().navigate(DetailsFragmentDirections.actionDetailsFragmentToMainFragment())
                screenshotDetailViewModel.onNavigateToMainFragmentDone()
            }
        })
    }

    private fun getUserAuthorizationToDelete(id: Int) {

        val (title, message) = when (id){
            R.id.edit_options -> Pair(getString(R.string.edit_this),getString(R.string.edit_this_message))
            R.id.delete_options -> Pair(getString(R.string.delete_this), getString(R.string.delete_this_message))
            R.id.share_options -> Pair(getString(R.string.share_this), getString(R.string.share_this_message))
            else -> Pair("","")
        }

        val alertDialogBuilder: AlertDialog.Builder? = activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.apply {
                setNegativeButton(R.string.cancel,
                    DialogInterface.OnClickListener { dialog, _ ->
                        screenshotDetailViewModel.onConfirmToMakeAction(false, id, context.contentResolver)
                        dialog.dismiss()
                    })
                setPositiveButton(R.string.ok, DialogInterface.OnClickListener { dialog, _ ->
                    screenshotDetailViewModel.onConfirmToMakeAction(true, id, context.contentResolver)
                    dialog.dismiss()
                })

                setTitle(title)
                setMessage(message)
            }
        }

        alertDialogBuilder?.create()?.show()
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity?)?.supportActionBar?.show()
    }


    private fun extractImage() {

    }

    private fun setUpVisibility() {
        if (optionsView.isVisible){
            optionsView.visibility = View.GONE

        } else {
            optionsView.visibility = View.VISIBLE
        }
        Log.i("MyOptionsView","setting view to: ${optionsView.visibility}")
    }


}