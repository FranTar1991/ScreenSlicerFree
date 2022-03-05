package com.screenslicerfree.detailsView.viewpager

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.SharedElementCallback
import androidx.core.view.doOnPreDraw
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.material.snackbar.Snackbar
import com.screenslicerfree.MainActivity
import com.screenslicerfree.MainActivity.Companion.currentPosition
import com.screenslicerfree.R
import com.screenslicerfree.adds.*
import com.screenslicerfree.database.DAOScrenshots
import com.screenslicerfree.database.ScreenshotItem
import com.screenslicerfree.database.ScreenshotsDatabase
import com.screenslicerfree.databinding.FragmentViewPagerDetailsBinding
import com.screenslicerfree.detailsView.DetailsViewModel
import com.screenslicerfree.detailsView.DetailsViewModelFactory
import com.screenslicerfree.utils.*


class ViewPagerDetailsFragment : Fragment(), OnUserEarnedRewardListener {


    private var callingView: ImageView? = null
    private var snackbar: Snackbar?= null
    private lateinit var binding: FragmentViewPagerDetailsBinding
    private lateinit var myRewardedInterstitialAdManager: MyRewardedInterstitialAd
    private var currentId: Int = -1
    private lateinit var currentAction: () -> Unit
    private lateinit var myInterstitialAdManager: MyInterstitialAd

    private lateinit var myItemPassedUri: String
    private var indexOfItemToSet: Int = 0
    private  lateinit var currentItem: ScreenshotItem
    private lateinit var viewPagerOnPageChangeCallback: OnPageChangeCallback
    private var listOfAllScreenshots: List<ScreenshotItem> = listOf()


    private lateinit var dataSource: DAOScrenshots

    private lateinit var screenshotDetailViewModel: DetailsViewModel
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var viewPager: ViewPager2
    private lateinit var optionsView: ConstraintLayout
    private lateinit var toolbar: Toolbar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding  = DataBindingUtil.inflate(inflater, R.layout.fragment_view_pager_details, container, false)

        viewPager = binding.pager
        optionsView = binding.optionsViewContraint

        val application = requireNotNull(this.activity).application

        val myItemPassed = ViewPagerDetailsFragmentArgs.fromBundle(requireArguments())
         myItemPassedUri = myItemPassed.uri


        // Create an instance of the ViewModel Factory.
        dataSource = ScreenshotsDatabase.getInstance(application).screenshotsDAO
        val viewModelFactory = DetailsViewModelFactory(myItemPassedUri, dataSource)


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
                }else{
                    listOfAllScreenshots = it
                    adapter.submitList(it)

                    indexOfItemToSet =
                        listOfAllScreenshots.indexOfFirst { itemFound -> itemFound.uri == myItemPassedUri}

                    screenshotDetailViewModel.setNewScreenshot(listOfAllScreenshots[indexOfItemToSet])
                    viewPager.setCurrentItem( indexOfItemToSet, false)
                }


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


        myInterstitialAdManager = MyInterstitialAd(activity as MainActivity,
            launchToActionInterstitialUnitID, false, callingView){
            getUserAuthorizationToTakeAction(currentId, currentAction)
        }

        myRewardedInterstitialAdManager = MyRewardedInterstitialAd(activity as MainActivity)
        loadBanner(binding.detailsAdView)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        snackbar?.dismiss()
    }

    override fun onResume() {
        super.onResume()
        myInterstitialAdManager.loadInterstitial()
        myRewardedInterstitialAdManager.loadAd()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        snackbar = Snackbar.make(binding.root as ConstraintLayout,"",Snackbar.LENGTH_INDEFINITE)

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
            currentId = it.id
            currentAction = {
                mainActivityViewModel.setFloatingImageViewUri(currentItem.uri)
            }
            callingView = binding.extractOptions
            myInterstitialAdManager.showInterstitial()
        })
    }


    private fun setUpExtractTextListeners(binding: FragmentViewPagerDetailsBinding){
        binding.extractTextOptions.setOnClickListener(View.OnClickListener {

            getUserAuthorizationToTakeAction(it.id){
                myRewardedInterstitialAdManager.showRewardedAd(context, this)
            }

        })
    }

    private fun setupShareListeners(binding: FragmentViewPagerDetailsBinding) {
        binding.shareOptions.setOnClickListener(View.OnClickListener {
            currentId = it.id
            currentAction ={
                shareScreenShot(context, Uri.parse(currentItem.uri),activity as MainActivity)
            }

            callingView = binding.shareOptions
            myInterstitialAdManager.showInterstitial()
        })
    }

    private fun setUpEditListeners(binding: FragmentViewPagerDetailsBinding) {

        binding.editOptions.setOnClickListener(View.OnClickListener {
            currentId = it.id

            currentAction ={
                editScreenShot(Uri.parse(currentItem.uri), activity as MainActivity)
            }
            callingView = binding.editOptions
            myInterstitialAdManager.showInterstitial()
        })
    }

    private fun setUpDeleteListeners(binding: FragmentViewPagerDetailsBinding) {

        binding.deleteOptions.setOnClickListener(View.OnClickListener {
            currentId = it.id

            currentAction ={

                myItemPassedUri= getTheNextItemToPass()
                val list = listOf(currentItem.uri)
                screenshotDetailViewModel.onDeleteListWithUri(list)
                deleteItemFromGallery(list,context?.contentResolver)
            }
            callingView = binding.deleteOptions
            myInterstitialAdManager.showInterstitial()
        })
    }

    private fun getTheNextItemToPass(): String {
        val itemOfCurrentIndex = listOfAllScreenshots.indexOf(currentItem)
        val nextIndex =if (listOfAllScreenshots.size <= 1){
            0
        }else{
            if (itemOfCurrentIndex == 0){
                1
            }else{
                itemOfCurrentIndex-1
            }
        }

        return listOfAllScreenshots[nextIndex].uri
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


        createActionDialog(activity as MainActivity, title, message, null, actionToTake)
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

    override fun onUserEarnedReward(p0: RewardItem) {

            context?.let { it ->
             screenshotDetailViewModel
                 .extractTextFromImage(it,
                     Uri.parse(currentItem.uri),
                    snackbar)
            }

    }

}