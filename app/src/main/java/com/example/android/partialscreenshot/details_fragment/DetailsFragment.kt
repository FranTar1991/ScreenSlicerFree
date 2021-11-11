package com.example.android.partialscreenshot.details_fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.database.ScreenshotsDatabase
import com.example.android.partialscreenshot.databinding.FragmentDetailsBinding
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.android.partialscreenshot.MainActivity
import com.example.android.partialscreenshot.database.ScreenshotsDAO
import com.example.android.partialscreenshot.utils.*


class DetailsFragment : Fragment() {

    private lateinit var toolbar: Toolbar
    private lateinit var myItemPassed: DetailsFragmentArgs
    private lateinit var dataSource: ScreenshotsDAO
    private lateinit var optionsView: ConstraintLayout
    private lateinit var screenshotDetailViewModel: DetailsViewModel
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentDetailsBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_details, container, false
        )

        val application = requireNotNull(this.activity).application

        myItemPassed = DetailsFragmentArgs.fromBundle(requireArguments())

        // Create an instance of the ViewModel Factory.
        dataSource = ScreenshotsDatabase.getInstance(application).screenshotsDAO
        val viewModelFactory = SleepDetailViewModelFactory(myItemPassed.uri, dataSource)

        optionsView = binding.optionsViewContraint

        // Get a reference to the ViewModel associated with this fragment.
        screenshotDetailViewModel =
            ViewModelProvider(
                this, viewModelFactory
            ).get(DetailsViewModel::class.java)

        // To use the View Model with data binding, you have to explicitly
        // give the binding object a reference to it.
        binding.screenshotDetailViewModel = screenshotDetailViewModel


        binding.mainImageDetails.setOnClickListener(View.OnClickListener {
            setUpVisibility()
        })


        setUpDeleteListeners(binding)
        setUpEditListeners(binding)
        setupShareListeners(binding)
        setUpExtractImageListeners(binding)

        binding.lifecycleOwner = activity

        toolbar = binding.myToolbar

        toolbar.apply {
            setNavigationOnClickListener(View.OnClickListener { activity?.onBackPressed() })
        }

        Log.i("MyUri","here: ${myItemPassed.uri}")
        return binding.root
    }


    private fun setUpExtractImageListeners(binding: FragmentDetailsBinding){
        binding.extractOptions.setOnClickListener(View.OnClickListener {
            getUserAuthorizationToTakeAction(it.id){
                mainActivityViewModel.setFloatingImageViewUri(myItemPassed.uri)
            }
        })
    }
    private fun setupShareListeners(binding: FragmentDetailsBinding) {
        binding.shareOptions.setOnClickListener(View.OnClickListener {
            getUserAuthorizationToTakeAction(it.id){
                shareScreenShot(context,Uri.parse(myItemPassed.uri),activity as MainActivity)
            }
        })
    }

    private fun setUpEditListeners(binding: FragmentDetailsBinding) {

        binding.editOptions.setOnClickListener(View.OnClickListener {
            getUserAuthorizationToTakeAction(it.id){
                editScreenShot(Uri.parse(myItemPassed.uri), activity as MainActivity)
            }
        })
    }

    private fun setUpDeleteListeners(binding: FragmentDetailsBinding) {

        binding.deleteOptions.setOnClickListener(View.OnClickListener {
            getUserAuthorizationToTakeAction(it.id){
                val list = listOf(myItemPassed.uri)
                screenshotDetailViewModel.onDeleteListWithUri(list)
                deleteItemFromGallery(list,context?.contentResolver)
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

    private fun getUserAuthorizationToTakeAction(id: Int, actionToTake: ()->Unit) {

        val (title, message) = when (id){
            R.id.edit_options -> Pair(getString(R.string.edit_this),getString(R.string.edit_this_message))
            R.id.delete_options -> Pair(getString(R.string.delete_this), getString(R.string.delete_this_message))
            R.id.share_options -> Pair(getString(R.string.share_this), getString(R.string.share_this_message))
            R.id.extract_options -> Pair(getString(R.string.extract_this), getString(R.string.extract_this_message))
            else -> Pair("","")
        }

        createActionDialog(actionToTake, activity as MainActivity,title, message, null)
    }

    private fun setUpVisibility() {
        if (optionsView.isVisible){
            optionsView.visibility = View.GONE
            toolbar.visibility = View.GONE

        } else {
            optionsView.visibility = View.VISIBLE
            toolbar.visibility = View.VISIBLE
        }
        Log.i("MyOptionsView","setting view to: ${optionsView.visibility}")
    }


}