package com.screenslicerfree.gestures.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.google.android.material.switchmaterial.SwitchMaterial

import com.screenslicerfree.gestures.action.database.AllAppsDatabase
import com.screenslicerfree.gestures.view.viewmodel.GesturesSettingsViewModelFactory
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.view.Gravity
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer

import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.screenslicerfree.gestures.view.adapter.AppsAdapter
import com.screenslicerfree.gestures.view.viewmodel.GestureSettingsViewModel
import com.screenslicerfree.utils.*
import com.screenslicerfree.R
import com.screenslicerfree.databinding.FragmentGesturesSettingsBinding

import tourguide.tourguide.TourGuide


class GesturesFragmentSettings : Fragment() {


    private var booleanFlag: Boolean = false
    private lateinit var gestureSettingsViewModel: GestureSettingsViewModel
    private lateinit var toolbar: Toolbar
    private lateinit var switchButton: SwitchMaterial
    private lateinit var allwedAppsTitle: TextView
    private lateinit var adapter: AppsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var proImage: ImageView
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val SHOW_SWITCH_TOUR: String ="show_switch_tour"
    private var showTourGuide: Boolean = true
    private var mTourGuideHandler: TourGuide? = null
    private var sharedPreferences: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding: FragmentGesturesSettingsBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_gestures_settings, container, false)
        toolbar = binding.toolBarSettings
        toolbar.apply {
            setNavigationOnClickListener(View.OnClickListener {
                this.findNavController().navigateUp()
            })
        }

        val application = requireNotNull(this.activity).application
        val dataSource = AllAppsDatabase.getInstance(application).appsDAO
        val sharedPreferences = context?.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)
        val viewModelFactory = GesturesSettingsViewModelFactory(dataSource, sharedPreferences, application)
        gestureSettingsViewModel = ViewModelProvider(this, viewModelFactory).get(GestureSettingsViewModel::class.java)

        proImage = binding.proImage

        binding.gestureSettingsViewModel = gestureSettingsViewModel
        binding.lifecycleOwner = activity


        switchButton = binding.switch1
        switchButton.setOnClickListener {
            closeTourGuide()
            rotateImage(proImage,switchButton)
            if (gestureSettingsViewModel.hasPermission.value == false && switchButton.isChecked){
//                createActionDialog(
//                    activity as MainActivity,
//                    getString(R.string.title_dialog),
//                    getString(R.string.permission_for_gestures),
//                    null){
//
//                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
//                    startActivity(intent)
//
//                }
            }

        }
        switchButton.setOnCheckedChangeListener { buttonView, isChecked ->

            if(isChecked){
                Toast.makeText(context,getString(R.string.only_pro,
                getString(R.string.gesture_feature)),Toast.LENGTH_LONG).show()
                if (booleanFlag){
                  //  mainActivityViewModel.checkIfHasOverlayPermission(true)

                    booleanFlag = false
                }
                gestureSettingsViewModel.setSwitchToChecked()

            }else{
                gestureSettingsViewModel.setSwitchToNotChecked()
                booleanFlag = true
            }

           // mainActivityViewModel.setDestroyService(true)
        }

        adapter = AppsAdapter( gestureSettingsViewModel,AppClickListener { view, app ->

            app.isAllowed = !app.isAllowed
            gestureSettingsViewModel.updateAppItem(app)
        })
        val manager = LinearLayoutManager(activity)
        recyclerView = binding.allApps
        recyclerView.adapter = adapter
        recyclerView.layoutManager = manager
        gestureSettingsViewModel.setIsLoading(true)
        gestureSettingsViewModel.apps.observe(viewLifecycleOwner, Observer {
            it?.let { newList ->
                adapter.submitList(newList)
                gestureSettingsViewModel.setAppCount(newList.size)
                gestureSettingsViewModel.setIsLoading(false)
            }
        })

        allwedAppsTitle = binding.textView4
        startSharedPreferences()
        startTourGuide()

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun closeTourGuide() {
        mTourGuideHandler?.cleanUp()
        if(showTourGuide){
            editor?.putBoolean(SHOW_SWITCH_TOUR, false)
            editor?.apply()
        }
    }

    override fun onStop() {
        super.onStop()
        closeTourGuide()
    }



    private fun startTourGuide() {

        if(showTourGuide){
            mTourGuideHandler = activity?.let {
                setMyTourGuide(
                    it, getString(R.string.turn_on_gestures_title),
                    getString(R.string.description_gestures),
                    Gravity.START or Gravity.BOTTOM,
                    switchButton)
            }
        }
    }

    private fun startSharedPreferences() {
        sharedPreferences = context?.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)
        showTourGuide =  sharedPreferences?.getBoolean(SHOW_SWITCH_TOUR, true) ?: true
        editor = sharedPreferences?.edit()
    }


    override fun onResume() {
        super.onResume()
        gestureSettingsViewModel.shouldBeChecked()

    }

    override fun onPause() {
        super.onPause()
        gestureSettingsViewModel.shouldBeChecked()
    }



}