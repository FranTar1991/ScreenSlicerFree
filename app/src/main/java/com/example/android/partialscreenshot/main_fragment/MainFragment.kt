package com.example.android.partialscreenshot.main_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.database.ScreenshotItem
import com.example.android.partialscreenshot.database.ScreenshotsDatabase
import com.example.android.partialscreenshot.databinding.FragmentMainBinding
import com.example.android.partialscreenshot.main_fragment.adapter.ScreenshotListener
import com.example.android.partialscreenshot.main_fragment.adapter.ScreenshotsAdapter
import com.example.android.partialscreenshot.utils.MainActivityViewModel


class MainFragment : Fragment() {

    private lateinit var screenshotViewModel: MainFragmentViewModel
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentMainBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_main, container, false)

        binding.callFloatingWindow.setOnClickListener(View.OnClickListener {
            mainActivityViewModel.checkIfHasOverlayPermission(true)
        })

        val application = requireNotNull(this.activity).application
        val dataSource = ScreenshotsDatabase.getInstance(application).screenshotsDAO
        val viewModelFactory = MainFragmentViewmodelFactory(dataSource, application)


        screenshotViewModel =
            ViewModelProvider(this, viewModelFactory).get(MainFragmentViewModel::class.java)


        binding.allScreenshotsViewModel = screenshotViewModel
        binding.lifecycleOwner = this

        val adapter = ScreenshotsAdapter(ScreenshotListener(::clickListener))


        binding.allPictures.adapter = adapter

        val manager = GridLayoutManager(activity,3)
        binding.allPictures.layoutManager = manager


        screenshotViewModel.screenshots.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })

        screenshotViewModel.navigateToScreenshot.observe(viewLifecycleOwner, Observer {screenshot ->
            screenshot?.let {
                this.findNavController().navigate(MainFragmentDirections.actionMainFragmentToDetailsFragment(screenshot))
                screenshotViewModel.onScreenshotNavigated()
            }
        })


        return binding.root
    }

    private fun clickListener(screenshotId: Long){
        screenshotViewModel.onScreenshotClicked(screenshotId)
    }

}