package com.example.android.partialscreenshot.details_fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.database.ScreenshotsDatabase
import com.example.android.partialscreenshot.databinding.FragmentDetailsBinding


class DetailsFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentDetailsBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_details, container, false)

        val application = requireNotNull(this.activity).application
        val arguments = DetailsFragmentArgs.fromBundle(requireArguments())

        // Create an instance of the ViewModel Factory.
        val dataSource = ScreenshotsDatabase.getInstance(application).screenshotsDAO
        val viewModelFactory = SleepDetailViewModelFactory(arguments.screenshotId, dataSource)

        // Get a reference to the ViewModel associated with this fragment.
        val screenshotDetailViewModel =
            ViewModelProvider(
                this, viewModelFactory).get(ScreenshotDetailViewModel::class.java)

        // To use the View Model with data binding, you have to explicitly
        // give the binding object a reference to it.
        binding.screenshotDetailViewModel = screenshotDetailViewModel

        binding.lifecycleOwner = this

//        // Add an Observer to the state variable for Navigating when a Quality icon is tapped.
//        screenshotDetailViewModel.navigateToSleepTracker.observe(viewLifecycleOwner, Observer {
//            if (it == true) { // Observed state is true.
//                this.findNavController().navigate(
//                    SleepDetailFragmentDirections.actionSleepDetailFragmentToSleepTrackerFragment())
//                // Reset state to make sure we only navigate once, even if the device
//                // has a configuration change.
//                sleepDetailViewModel.doneNavigating()
//            }
//        })


        return binding.root
    }


}