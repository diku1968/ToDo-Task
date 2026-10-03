package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.data.preferences.UserPreferencesRepository
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object AdConfig {
    // Official Google AdMob Test Ad Units
    const val ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val ADMOB_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val ADMOB_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    // Cooldown interval between interstitial ads (3 minutes)
    const val INTERSTITIAL_MIN_INTERVAL_MS = 3 * 60 * 1000L
}

class AdManager(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val tag = "AdManager"

    private val _isAdsEnabled = MutableStateFlow(true)
    val isAdsEnabled: StateFlow<Boolean> = _isAdsEnabled.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var lastInterstitialShownTime = 0L

    init {
        CoroutineScope(Dispatchers.IO).launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                _isAdsEnabled.value = prefs.showAds
            }
        }

        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(tag, "MobileAds initialized: $initializationStatus")
                loadInterstitialAd()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error initializing MobileAds", e)
        }
    }

    fun loadInterstitialAd() {
        if (!_isAdsEnabled.value || interstitialAd != null || isInterstitialLoading) {
            return
        }

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            AdConfig.ADMOB_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(tag, "Interstitial ad successfully loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(tag, "Interstitial ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (!_isAdsEnabled.value) {
            onAdDismissed()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInterstitialShownTime < AdConfig.INTERSTITIAL_MIN_INTERVAL_MS) {
            Log.d(tag, "Interstitial throttled by cooldown interval")
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastInterstitialShownTime = System.currentTimeMillis()
                    loadInterstitialAd()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    Log.w(tag, "Interstitial ad failed to show: ${adError.message}")
                    loadInterstitialAd()
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(tag, "Interstitial ad displayed")
                }
            }
            ad.show(activity)
        } else {
            loadInterstitialAd()
            onAdDismissed()
        }
    }
}
