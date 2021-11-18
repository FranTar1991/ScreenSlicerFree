package com.example.android.partialscreenshot.details_fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.database.ScreenshotsDatabase
import com.example.android.partialscreenshot.databinding.FragmentDetailsBinding
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.android.partialscreenshot.MainActivity
import com.example.android.partialscreenshot.database.ScreenshotsDAO
import com.example.android.partialscreenshot.utils.*

import android.view.*
import androidx.core.app.SharedElementCallback
import androidx.core.view.GestureDetectorCompat
import com.example.android.partialscreenshot.database.ScreenshotItem
import kotlin.math.abs
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.transition.TransitionInflater













class DetailsFragment : Fragment(), GestureDetector.OnGestureListener,View.OnTouchListener{

    private lateinit var navBuilder: NavOptions.Builder
    private var myNewItemUri: String? = ""
    private var indexOfCurrent: Int = -1
    private var listOfAllScreenshots: List<ScreenshotItem> = listOf()
    private lateinit var toolbar: Toolbar
    private lateinit var myItemPassed: DetailsFragmentArgs
    private lateinit var dataSource: ScreenshotsDAO
    private lateinit var optionsView: ConstraintLayout
    private lateinit var screenshotDetailViewModel: DetailsViewModel
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var mDetector: GestureDetectorCompat
    private val swipeThreshold = 100
    private val swipeVelocityThreshold = 100

    @SuppressLint("ClickableViewAccessibility")
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

        screenshotDetailViewModel.screenshots.observe(viewLifecycleOwner, Observer {
            listOfAllScreenshots = it
        })

        navBuilder = NavOptions.Builder()

        screenshotDetailViewModel.navigateToSelf.observe(viewLifecycleOwner, Observer {
            it?.let {
                this.findNavController()
                    .navigate(DetailsFragmentDirections.actionDetailsFragmentSelf(it),navBuilder.build())
                screenshotDetailViewModel.onScreenshotNavigatedToSelfDone()
            }

        })

        binding.root.setOnTouchListener(this)

        mDetector = GestureDetectorCompat(activity?.applicationContext, this)



        setUpDeleteListeners(binding)
        setUpEditListeners(binding)
        setupShareListeners(binding)
        setUpExtractImageListeners(binding)

        binding.lifecycleOwner = activity

        toolbar = binding.myToolbar

        toolbar.apply {
            setNavigationOnClickListener(View.OnClickListener {
                this.findNavController().navigateUp()
            })
        }

        setEnterSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String?>, sharedElements: MutableMap<String?, View?>
                ) {


                }
            })

        return binding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
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
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
       (mDetector.onTouchEvent(p1))
        return true

    }

    override fun onDown(p0: MotionEvent?): Boolean {
      return true
    }

    override fun onShowPress(p0: MotionEvent?) {

    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        setUpVisibility()
        return true
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return true
    }

    override fun onLongPress(p0: MotionEvent?) {

    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        try {
            val diffY = (p1?.y ?: 0f) - (p0?.y ?: 0f)
            val diffX = (p1?.x ?: 0f) - (p0?.x ?: 0f)
            if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > swipeThreshold && abs(p2) > swipeVelocityThreshold) {
                    if (diffX > 0) {
                        Log.i("MyNextUri","show previous")
                        showPreviousScreenshot()
                    }
                    else {
                        showNextScreenshot()
                    }
                }
            }
        }
        catch (exception: Exception) {
            exception.printStackTrace()
        }
        return true
    }


    private fun showNextScreenshot() {
        navBuilder.apply {
                setPopUpTo(R.id.mainFragment,false)
                setEnterAnim(R.anim.from_right)
                setExitAnim(R.anim.to_left)
                setPopEnterAnim(android.R.anim.fade_in)
                setPopExitAnim(android.R.anim.fade_out)
        }


        indexOfCurrent = listOfAllScreenshots.indexOfFirst{it.uri == myItemPassed.uri}
        myNewItemUri = listOfAllScreenshots.elementAt(indexOfCurrent+1).uri
        screenshotDetailViewModel.onNavigateToSelf(myNewItemUri)
    }

    private fun showPreviousScreenshot() {

        navBuilder.apply {
            setPopUpTo(R.id.mainFragment,false)
            setEnterAnim(R.anim.from_left)
            setExitAnim(R.anim.to_right)
            setPopEnterAnim(android.R.anim.fade_in)
            setPopExitAnim(android.R.anim.fade_out)
        }


        indexOfCurrent = listOfAllScreenshots.indexOfFirst{it.uri == myItemPassed.uri}
        myNewItemUri = listOfAllScreenshots.elementAtOrNull(indexOfCurrent-1)?.uri
        screenshotDetailViewModel.onNavigateToSelf(myNewItemUri)
    }


}