package com.screenslicerpro.gestures.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.screenslicerpro.R
import com.screenslicerpro.databinding.FragmentGesturesSettingsBinding
import com.screenslicerpro.databinding.FragmentMainBinding


class GesturesFragmentSettings : Fragment() {



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding: FragmentGesturesSettingsBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_gestures_settings, container, false)

        // Inflate the layout for this fragment
        return binding.root
    }


}