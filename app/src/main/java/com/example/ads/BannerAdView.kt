package com.example.ads

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun BannerAdView(
    adManager: AdManager,
    modifier: Modifier = Modifier
) {
    val isAdsEnabled by adManager.isAdsEnabled.collectAsState()
    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }

    if (!isAdsEnabled) {
        return
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdConfig.ADMOB_BANNER_ID
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                            Log.d("BannerAdView", "Banner ad loaded successfully")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                            Log.w("BannerAdView", "Banner ad failed to load: ${error.message}")
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
