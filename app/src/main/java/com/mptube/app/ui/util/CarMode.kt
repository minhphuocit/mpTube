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

    val isAndroidCar = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_CAR
    val isLargeLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            configuration.screenWidthDp >= 600

    val largeControls = isAndroidCar || isLargeLandscape

    return if (largeControls) {
        MpTubeUiMetrics(
            largeControls = true,
            touchTarget = 64.dp,
            navigationHeight = 76.dp,
            titleSize = 26.sp,
            bodySize = 24.sp
        )
    } else {
        MpTubeUiMetrics(
            largeControls = false,
            touchTarget = 48.dp,
            navigationHeight = 64.dp,
            titleSize = 18.sp,
            bodySize = 14.sp
        )
    }
}
