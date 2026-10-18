package com.mptube.app.ui.util

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MpTubeUiMetrics(
    val largeControls: Boolean,
    val compactLandscape: Boolean,
    val carLikeLandscape: Boolean,
    val contentWidthFraction: Float,
    val touchTarget: Dp,
    val navigationHeight: Dp,
    val titleSize: TextUnit,
    val bodySize: TextUnit
)

@Composable
fun rememberMpTubeUiMetrics(): MpTubeUiMetrics {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isAndroidCar = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_CAR
    val isLargeScreen = configuration.smallestScreenWidthDp >= 600
    val carLikeLandscape = isLandscape && (isAndroidCar || isLargeScreen || configuration.screenHeightDp >= 480)

    if (isLandscape) {
        return MpTubeUiMetrics(
            largeControls = false,
            compactLandscape = true,
            carLikeLandscape = carLikeLandscape,
            contentWidthFraction = 0.65f,
            touchTarget = 48.dp,
            navigationHeight = 52.dp,
            titleSize = 16.sp,
            bodySize = 13.sp
        )
    }

    return if (isAndroidCar) {
        MpTubeUiMetrics(
            largeControls = true,
            compactLandscape = false,
            carLikeLandscape = false,
            contentWidthFraction = 1f,
            touchTarget = 56.dp,
            navigationHeight = 68.dp,
            titleSize = 22.sp,
            bodySize = 18.sp
        )
    } else {
        MpTubeUiMetrics(
            largeControls = false,
            compactLandscape = false,
            carLikeLandscape = false,
            contentWidthFraction = 1f,
            touchTarget = 48.dp,
            navigationHeight = 64.dp,
            titleSize = 18.sp,
            bodySize = 14.sp
        )
    }
}
