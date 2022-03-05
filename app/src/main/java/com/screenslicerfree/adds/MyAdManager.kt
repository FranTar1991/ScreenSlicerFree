package com.screenslicerfree.adds

import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.screenslicerfree.MainActivity
import com.screenslicerfree.R


private var TAG = "MyAddsManager"

fun loadBanner(mAdView: AdView) {
    val adRequest: AdRequest = AdRequest.Builder().build()
    mAdView.loadAd(adRequest)
}


class MyInterstitialAd(private val activity: MainActivity, private val addUnitID: String, private val showAdFirstThenAction: Boolean, private val callingView: ImageView?, private val action: () -> Unit){

    private var mInterstitialAd: InterstitialAd? = null
    private var isLoaded = false

    fun loadInterstitial() {

        if (!isLoaded){
            val adRequest = AdRequest.Builder().build()
            Log.i(TAG, "calling the load method.")
            InterstitialAd.load(activity, addUnitID, adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        mInterstitialAd = interstitialAd
                        setInterstitialCallBacks(mInterstitialAd)
                        isLoaded = true
                        Log.i(TAG, "the ad is ready.")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.i(TAG, "the ad did not load.")
                        isLoaded = false
                    }
                })
        }

    }

     private fun setInterstitialCallBacks(mInterstitialAd: InterstitialAd?){
        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.i(TAG, "Ad was dismissed.")
                if (!showAdFirstThenAction){
                    action()
                }
                callingView?.isEnabled = true
                isLoaded = false

            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                Log.i(TAG, "Ad failed to show.: $adError")
                action()
                isLoaded = false
            }

            override fun onAdShowedFullScreenContent() {
                if (showAdFirstThenAction){
                    action()
                    isLoaded = false
                }
                callingView?.isEnabled = true
                isLoaded = false
            }
        }
    }

     fun showInterstitial(){
         callingView?.isEnabled = false
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(activity)
        } else {
            action()
            callingView?.isEnabled = true
            Log.i(TAG, "The interstitial ad wasn't ready yet.")
        }
    }

}

class MyRewardedInterstitialAd(private val activity: MainActivity){
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isLoaded = false

    init {
        loadAd()
    }

    fun loadAd() {

        if (!isLoaded){
            // Use the test ad unit ID to load an ad.
            RewardedInterstitialAd.load(activity, launchToRewardedInterstitialUnitId,
                AdRequest.Builder().build(), object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        rewardedInterstitialAd = ad
                        Log.i(TAG, "onAdLoaded")
                        rewardedInterstitialAd?.fullScreenContentCallback = setCallbacks()
                        isLoaded = true
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.i(TAG, "onAdFailedToLoad: $loadAdError")
                        isLoaded = false
                    }
                })
        }

    }

    private fun setCallbacks(): FullScreenContentCallback {
        return  object : FullScreenContentCallback() {
            /** Called when the ad failed to show full screen content.  */
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.i(TAG, "onAdFailedToShowFullScreenContent")
            }

            /** Called when ad showed the full screen content.  */
            override fun onAdShowedFullScreenContent() {
                Log.i(TAG, "onAdShowedFullScreenContent")
            }

            /** Called when full screen content is dismissed.  */
            override fun onAdDismissedFullScreenContent() {
                Log.i(TAG, "onAdDismissedFullScreenContent")
            }
        }
    }

    fun showRewardedAd(context: Context?, onUserEarnedRewardListener: OnUserEarnedRewardListener){
        if (rewardedInterstitialAd != null){
            rewardedInterstitialAd?.show(activity,onUserEarnedRewardListener);
        }else{
            Toast.makeText(context, context?.getString(R.string.ad_not_loaded), Toast.LENGTH_SHORT).show()
        }
    }
}



