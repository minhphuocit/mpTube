package com.mptube.app.ui.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
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
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    val smallestScreenWidthDp = configuration.smallestScreenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val uiModeType = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK

    return remember(orientation, smallestScreenWidthDp, screenHeightDp, uiModeType) {
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        val isAndroidCar = uiModeType == Configuration.UI_MODE_TYPE_CAR
        val isLargeScreen = smallestScreenWidthDp >= 600
        val carLikeLandscape = isLandscape && (isAndroidCar || isLargeScreen || screenHeightDp >= 480)

        if (isLandscape) {
            MpTubeUiMetrics(
                largeControls = false,
                compactLandscape = true,
                carLikeLandscape = carLikeLandscape,
                contentWidthFraction = 0.65f,
                touchTarget = 48.dp,
                navigationHeight = 52.dp,
                titleSize = 16.sp,
                bodySize = 13.sp
            )
        } else if (isAndroidCar) {
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
}
