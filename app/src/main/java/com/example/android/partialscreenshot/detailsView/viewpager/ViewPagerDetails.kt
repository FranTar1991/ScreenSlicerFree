package com.example.android.partialscreenshot.detailsView.viewpager

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.SharedElementCallback
import androidx.core.view.doOnPreDraw
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import androidx.viewpager2.widget.ViewPager2
import com.example.android.partialscreenshot.MainActivity
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.database.ScreenshotItem
import com.example.android.partialscreenshot.database.ScreenshotsDAO
import com.example.android.partialscreenshot.database.ScreenshotsDatabase
import com.example.android.partialscreenshot.databinding.FragmentViewPagerDetailsBinding
import com.example.android.partialscreenshot.detailsView.DetailsViewModel
import com.example.android.partialscreenshot.detailsView.DetailsViewModelFactory
import com.example.android.partialscreenshot.utils.*
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.example.android.partialscreenshot.MainActivity.Companion.currentPosition


class ViewPagerDetails : Fragment() {

    private var indexOfItemToSet: Int = 0
    private  lateinit var currentItem: ScreenshotItem
    private lateinit var viewPagerOnPageChangeCallback: OnPageChangeCallback
    private var listOfAllScreenshots: List<ScreenshotItem> = listOf()


    private lateinit var dataSource: ScreenshotsDAO

    private lateinit var screenshotDetailViewModel: DetailsViewModel
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var viewPager: ViewPager2
    private lateinit var optionsView: ConstraintLayout
    private lateinit var toolbar: Toolbar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val binding : FragmentViewPagerDetailsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_view_pager_details, container, false)

        viewPager = binding.pager
        optionsView = binding.optionsViewContraint

        val application = requireNotNull(this.activity).application

       val  myItemPassed = ViewPagerDetailsArgs.fromBundle(requireArguments())


        // Create an instance of the ViewModel Factory.
        dataSource = ScreenshotsDatabase.getInstance(application).screenshotsDAO
        val viewModelFactory = DetailsViewModelFactory(myItemPassed.uri, dataSource)


        // Get a reference to the ViewModel associated with this fragment.
        screenshotDetailViewModel = ViewModelProvider(this, viewModelFactory).get(DetailsViewModel::class.java)

        binding.lifecycleOwner = activity

        // To use the View Model with data binding, you have to explicitly
        // give the binding object a reference to it.
        binding.screenShotDetailVM = screenshotDetailViewModel

        setUpDeleteListeners(binding)
        setUpEditListeners(binding)
        setupShareListeners(binding)
        setUpExtractImageListeners(binding)
        setUpExtractTextListeners(binding)

        toolbar = binding.toolBarDetails

        toolbar.apply {
            setNavigationOnClickListener(View.OnClickListener {
                this.findNavController().navigateUp()
            })
        }


        val adapter = ViewPagerDetailsAdapter(ScreenshotListener(::clickListener))
        viewPager.adapter = adapter

        screenshotDetailViewModel.screenshots.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (it.isEmpty()) {
                    this.findNavController().navigateUp()
                }

                listOfAllScreenshots = it
                adapter.submitList(it)

                indexOfItemToSet = if (listOfAllScreenshots.indexOfFirst { itemFound -> itemFound.uri == myItemPassed.uri } == -1){
                    +1
                } else {
                    listOfAllScreenshots.indexOfFirst { itemFound -> itemFound.uri == myItemPassed.uri }
                }

                viewPager.setCurrentItem( indexOfItemToSet, false)
                (view?.parent as? ViewGroup)?.doOnPreDraw {
                    startPostponedEnterTransition()
                }
            }


        })

        screenshotDetailViewModel.screenshot.observe(viewLifecycleOwner, Observer {
            it?.let {
                currentItem = it
            }

        })


        viewPagerOnPageChangeCallback = object : OnPageChangeCallback() {


            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                currentPosition = position
                if (listOfAllScreenshots.isNotEmpty()){
                    screenshotDetailViewModel.setNewScreenshot(listOfAllScreenshots[position])

                }
            }
        }

        viewPager.registerOnPageChangeCallback(viewPagerOnPageChangeCallback)

        setEnterSharedElementCallback(object : SharedElementCallback() {
                override fun onMapSharedElements(names: List<String>, sharedElements: MutableMap<String, View>) {
                    val view: View = (viewPager[0] as RecyclerView).findViewHolderForAdapterPosition(currentPosition)?.itemView ?: return
                    // Map the first shared element name to the child ImageView.
                    sharedElements[names[0]] = view.findViewById(R.id.main_image_details)
                }
            })

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager.unregisterOnPageChangeCallback(viewPagerOnPageChangeCallback)
    }

    private fun setUpExtractImageListeners(binding: FragmentViewPagerDetailsBinding){
        binding.extractOptions.setOnClickListener(View.OnClickListener {
            getUserAuthorizationToTakeAction(it.id){
                mainActivityViewModel.setFloatingImageViewUri(currentItem.uri)
            }
        })
    }

    private fun setUpExtractTextListeners(binding: FragmentViewPagerDetailsBinding){
        binding.extractTextOptions.setOnClickListener(View.OnClickListener {
            getUserAuthorizationToTakeAction(it.id){
                context?.let { it ->
                    screenshotDetailViewModel.extractTextFromImage(it,Uri.parse(currentItem.uri))

                }
            }
        })
    }

    private fun setupShareListeners(binding: FragmentViewPagerDetailsBinding) {
        binding.shareOptions.setOnClickListener(View.OnClickListener {
            getUserAuthorizationToTakeAction(it.id){
                shareScreenShot(context, Uri.parse(currentItem.uri),activity as MainActivity)
            }
        })
    }

    private fun setUpEditListeners(binding: FragmentViewPagerDetailsBinding) {

        binding.editOptions.setOnClickListener(View.OnClickListener {
            getUserAuthorizationToTakeAction(it.id){
                editScreenShot(Uri.parse(currentItem.uri), activity as MainActivity)
            }
        })
    }

    private fun setUpDeleteListeners(binding: FragmentViewPagerDetailsBinding) {

        binding.deleteOptions.setOnClickListener(View.OnClickListener {
            getUserAuthorizationToTakeAction(it.id){
                val list = listOf(currentItem.uri)
                screenshotDetailViewModel.onDeleteListWithUri(list)
                deleteItemFromGallery(list,context?.contentResolver)
            }
        })
    }

    private fun getUserAuthorizationToTakeAction(id: Int, actionToTake: ()->Unit) {

        val (title, message) = when (id){
            R.id.edit_options -> Pair(getString(R.string.edit_this),getString(R.string.edit_this_message))
            R.id.delete_options -> Pair(getString(R.string.delete_this), getString(R.string.delete_this_message))
            R.id.share_options -> Pair(getString(R.string.share_this), getString(R.string.share_this_message))
            R.id.extract_options -> Pair(getString(R.string.extract_this), getString(R.string.extract_this_message))
            R.id.extract_text_options -> Pair(getString(R.string.extract_this_text), getString(R.string.extract_this_text_message))

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
    }

    private fun clickListener(view: View, uri: String){
       setUpVisibility()
    }

}