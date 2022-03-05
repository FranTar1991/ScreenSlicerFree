package com.screenslicerfree.main_fragment


import android.app.Activity
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.View.OnLayoutChangeListener
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.SharedElementCallback
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.Selection
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.screenslicerfree.MainActivity
import com.screenslicerfree.MainActivity.Companion.currentPosition

import com.screenslicerfree.database.ScreenshotsDatabase

import com.screenslicerfree.main_fragment.adapter.MyItemDetailsLookup
import com.screenslicerfree.main_fragment.adapter.MyItemKeyProvider
import com.screenslicerfree.main_fragment.adapter.ScreenshotsAdapter
import com.screenslicerfree.utils.*
import com.screenslicerfree.R
import com.screenslicerfree.adds.*
import com.screenslicerfree.databinding.FragmentMainBinding

import tourguide.tourguide.TourGuide


class MainFragment : Fragment() {


    private lateinit var passsedScreenshotUri: String
    private lateinit var extras: FragmentNavigator.Extras

    private lateinit var proIconOnToolbar: ImageView
    private var myTourGuide: TourGuide?= null
    private lateinit var floatingButton: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewClicked: ImageView
    private var allSelected: Boolean = false

    private lateinit var myInterstitialAdManager: MyInterstitialAd

    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable

    private var sharedPreferences: SharedPreferences? = null
    private var SHOW_FIRST_TOUR: String = "show_first_tour"

    private var uriList: MutableList<String> = mutableListOf()
    private lateinit var adapter: ScreenshotsAdapter
    private lateinit var screenshotsSelected: Selection<String>
    private lateinit var tracker: SelectionTracker<String>
    private var actionMode: ActionMode? = null
    private lateinit var mainFragmentViewModel: MainFragmentViewModel
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.menu_selection, menu)
            toolBar.visibility = View.GONE
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {

            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.delete_on_menu -> {
                    getUserAuthorizationToTakeAction(item.itemId, ::deleteThisItems)
                    true
                }
                R.id.share_on_menu -> {
                    getUserAuthorizationToTakeAction(item.itemId, ::shareThisItem)
                    true
                }
                R.id.select_all_on_menu -> {

                    allSelected = if (!allSelected){
                        uriList.forEach {
                            if (!tracker.isSelected(it)){
                                tracker.select(it)
                            }
                        }
                        true
                    } else {
                        uriList.forEach {
                            tracker.deselect(it)
                        }
                        false
                    }

                    R.id.pro_on_menu


                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {

            tracker.clearSelection()
            actionMode = null
            toolBar.visibility = View.VISIBLE
        }
    }
    private lateinit var toolBar: Toolbar

    private fun getUserAuthorizationToTakeAction(id: Int, actionToTake: () -> Unit) {

        val (title, message) = when (id){
            R.id.delete_on_menu-> Pair(getString(R.string.delete_this),getString(R.string.delete_this_message))
            R.id.share_on_menu -> Pair(getString(R.string.share_this), getString(R.string.share_this_message))
            else -> Pair("","")
        }

        createActionDialog( activity as MainActivity,title, message, actionMode,actionToTake)
    }

    private fun deleteThisItems(){
        val list = screenshotsSelected.toList()

        mainFragmentViewModel.onDeleteListWithUri(list)
        deleteItemFromGallery(screenshotsSelected.toList(), context?.contentResolver)
        Toast.makeText(context, getString(R.string.delete,screenshotsSelected.size()), Toast.LENGTH_SHORT).show()
    }

    private fun shareThisItem(){
        val files: ArrayList<Uri> = ArrayList<Uri>()
        screenshotsSelected.toList().forEach {
            files.add(Uri.parse(it))
        }
        Log.i("MyListToShare","$files")

        val sendIntent = Intent().apply {
            type = "image/jpeg"
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)
        }

        val shareIntent = Intent.createChooser(sendIntent, getText(R.string.share))

        startActivity(shareIntent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentMainBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_main, container, false)

        proIconOnToolbar = binding.proOnView
        proIconOnToolbar.setOnClickListener {
            launchProVersionInGooglePlay()
        }



        floatingButton = binding.callFloatingWindow
        floatingButton.setOnClickListener(View.OnClickListener {
           closeTourGuide()
            mainActivityViewModel.checkIfHasOverlayPermission(true)
        })

        val application = requireNotNull(this.activity).application
        val dataSource = ScreenshotsDatabase.getInstance(application).screenshotsDAO
        val viewModelFactory = MainFragmentViewmodelFactory(dataSource)

        setHandler()
        mainFragmentViewModel =
            ViewModelProvider(this, viewModelFactory).get(MainFragmentViewModel::class.java)



        binding.allScreenshotsViewModel = mainFragmentViewModel
        binding.lifecycleOwner = activity
        toolBar = binding.myToolbar

        toolBar.setOnMenuItemClickListener {
            // Handle item selection
             when (it.itemId) {
                R.id.launch_gesture_settings_on_menu -> {
                    launchGestureSettings()

                    true
                }
                R.id.privacy_policy_on_menu -> {

                    launchInBrowser(context, Uri.parse(getString(R.string.privacy_policy_url)))
                    true
                }
                 R.id.pro_on_menu ->{
                     launchProVersionInGooglePlay()
                     true
                 }
                else -> super.onOptionsItemSelected(it)
            }
        }

        adapter = ScreenshotsAdapter(ScreenshotListener(::clickListener), mainFragmentViewModel)
        val manager = GridLayoutManager(activity,4)
        recyclerView = binding.allPictures
        recyclerView.adapter = adapter
        recyclerView.layoutManager = manager
        mainFragmentViewModel.setIsLoading(true)
        mainFragmentViewModel.screenshots.observe(viewLifecycleOwner, Observer {
            it?.let { newList ->

                    newList.forEach { list->
                        uriList.add(list.uri)
                    }

                    adapter.submitList(newList)

                    mainFragmentViewModel.setScreenShotCount(newList.size)
                    (view?.parent as? ViewGroup)?.doOnPreDraw {
                        startPostponedEnterTransition()
                    }

                mainFragmentViewModel.setIsLoading(false)
            }
        })

        myInterstitialAdManager = MyInterstitialAd(activity as MainActivity,
            launchToDetailsInterstitialUnitID,
            true,
            null,
            ::launchDetailsView)

        mainFragmentViewModel.navigateToScreenshot.observe(viewLifecycleOwner, Observer { screenshotUri ->
            screenshotUri?.let {
                actionMode?.finish()
                extras = FragmentNavigatorExtras(viewClicked to "large_image_$screenshotUri")
                passsedScreenshotUri = screenshotUri
                myInterstitialAdManager.showInterstitial()
            }
        })

        mainFragmentViewModel.navigateToGSettings.observe(viewLifecycleOwner, Observer { booleanVariable ->
            booleanVariable?.let {

                this.findNavController()
                    .navigate(MainFragmentDirections.actionMainFragmentToGesturesFragmentSettings())
                mainFragmentViewModel.onNavigateToGestureSettingsNavigated()
            }
        })

        mainFragmentViewModel.navigateToProFragment.observe(viewLifecycleOwner, Observer { booleanVariable ->
            booleanVariable?.let {

                this.findNavController()
                    .navigate(MainFragmentDirections.actionMainFragmentToProVersionFragment())
                mainFragmentViewModel.onNavigateToProNavigated()
            }
        })

        tracker = SelectionTracker.Builder<String>(
            "mySelection",
            recyclerView,
           MyItemKeyProvider(adapter),
            MyItemDetailsLookup(binding.allPictures),
           StorageStrategy.createStringStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        )
           .build()
        adapter.tracker = tracker

        tracker.addObserver(object : SelectionTracker.SelectionObserver<String>() {
                override fun onSelectionRestored() {
                    super.onSelectionRestored()
                  setActionMode()
                }

                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    setActionMode()
                }

            })

        setExitSharedElementCallback(object : SharedElementCallback() {
                override fun onMapSharedElements(names: List<String?>, sharedElements: MutableMap<String?, View?>) {

                    val selectedViewHolder = recyclerView.findViewHolderForAdapterPosition(currentPosition)
                    if (selectedViewHolder?.itemView == null) {
                        return
                    }
                    // Map the first shared element name to the child ImageView.
                    sharedElements[names[0]] = selectedViewHolder.itemView.findViewById(R.id.imageView2)
                }
            })

        context?.let {
            exitTransition = TransitionInflater.from(it)
                .inflateTransition(R.transition.exit_transition)
        }



        mainActivityViewModel.permissionToSaveCalled.observe(viewLifecycleOwner, Observer {
            if (startTourGuide()){
                myTourGuide =  setMyTourGuide(activity as Activity, getString(R.string.title_welcome),
                    getString(R.string.description_first_tour),
                    Gravity.START or Gravity.TOP,
                    floatingButton as View )
            }
        })

        loadBanner(binding.adView)

        return binding.root
    }



    private fun launchProVersionInGooglePlay() {
        mainFragmentViewModel.onNavigateToProClicked()
    }

    override fun onStop() {
        super.onStop()
        closeTourGuide()
    }

    private fun startTourGuide(): Boolean {
        sharedPreferences = context?.getSharedPreferences(MY_PREFS_NAME, Service.MODE_PRIVATE)
       return  sharedPreferences?.getBoolean(SHOW_FIRST_TOUR, true) ?: true
    }

    fun closeTourGuide() {
        myTourGuide?.cleanUp()
        val editor: SharedPreferences.Editor? = sharedPreferences?.edit()
        editor?.putBoolean(SHOW_FIRST_TOUR, false)
        editor?.apply()
    }


    private fun setActionMode() {
        screenshotsSelected = tracker.selection
        val items = screenshotsSelected.size()

        when (actionMode) {
            null -> {
                // Start the CAB using the ActionMode.Callback defined above
                actionMode = activity?.startActionMode(actionModeCallback)


            }
        }
        actionMode?.title = getString(R.string.items_selected, items)


    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (this::tracker.isInitialized){
            tracker.onSaveInstanceState(outState)
        }

    }

    override fun onPause() {
        super.onPause()
        mainActivityViewModel.setCleanTourGuide(SHOW_FIRST_TOUR)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()

        tracker.onRestoreInstanceState(savedInstanceState)

        recyclerView.addOnLayoutChangeListener(
            object : OnLayoutChangeListener {
                override fun onLayoutChange(
                    view: View?,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int
                ) {
                    recyclerView.removeOnLayoutChangeListener(this)
                    val layoutManager: RecyclerView.LayoutManager = recyclerView.layoutManager!!
                    val viewAtPosition = layoutManager.findViewByPosition(currentPosition)
                    // Scroll to position if the view for the current position is null (not
                    // currently part of layout manager children), or it's not completely
                    // visible.
                    if (viewAtPosition == null
                        || layoutManager.isViewPartiallyVisible(viewAtPosition, false, true)
                    ) {
                        recyclerView.post { layoutManager.scrollToPosition(currentPosition) }
                    }
                }
            })

    }

    override fun onResume() {
        super.onResume()
       myInterstitialAdManager.loadInterstitial()
        startHandler()
    }

    private fun setHandler(){
        // Initializing the handler and the runnable
        mHandler = Handler(Looper.getMainLooper())
        mRunnable = Runnable {
            rotateImage(proIconOnToolbar, proIconOnToolbar)
        }
    }

    // start handler function
    private fun startHandler(){
        mHandler.postDelayed(mRunnable, TIME_TO_RUN_ANIMATION)
    }

    // stop handler function
    private fun stopHandler(){
        mHandler.removeCallbacks(mRunnable)
    }



    private fun launchGestureSettings() {
       mainFragmentViewModel.onNavigateToGestureSettingsClicked()

    }

    private fun clickListener(view: View, uri: String){
        viewClicked = view as ImageView
        currentPosition = uriList.indexOfFirst { it == uri }
        mainFragmentViewModel.onScreenshotClicked(uri)
    }


    private fun launchDetailsView() {
        this.findNavController()
            .navigate(MainFragmentDirections.actionMainFragmentToViewPagerDetails(passsedScreenshotUri),
                extras)
        mainFragmentViewModel.onScreenshotNavigated()
    }
}


