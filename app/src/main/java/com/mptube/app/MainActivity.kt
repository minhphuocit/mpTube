package com.mptube.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mptube.app.ui.screens.MainScreen
import com.mptube.app.ui.screens.SplashScreen
import com.mptube.app.ui.theme.MpTubeTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setContent {
            MpTubeTheme(
                darkTheme = true,
                dynamicColor = false
            ) {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(800)
                    showSplash = false
                }

                if (showSplash) SplashScreen() else MainScreen()
            }
        }
    }
}
