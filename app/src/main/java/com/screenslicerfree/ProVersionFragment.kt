package com.screenslicerfree

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.screenslicerfree.databinding.FragmentProVersionBinding


class ProVersionFragment : Fragment() {


    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentProVersionBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_pro_version, container, false)
        binding.lifecycleOwner = activity

        toolbar = binding.toolBarPro
        binding.launchButton.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.screenslicerpro")))
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.screenslicerpro")))
            }
        }


        toolbar.apply {
            setNavigationOnClickListener(View.OnClickListener {
                this.findNavController().navigateUp()
            })
        }
        return binding.root
    }

}