package com.furflix.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.os.Build
import coil.Coil
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.furflix.app.ui.components.ImageLoaderFactory
import com.furflix.app.ui.navigation.FurFlixNavGraph
import com.furflix.app.ui.screens.SplashScreen
import com.furflix.app.ui.theme.FurFlixTheme
import com.furflix.app.ui.theme.AppIcon
import com.furflix.app.utils.IconManager

class MainActivity : AppCompatActivity() {
    private var pendingAppIcon: AppIcon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val imageLoader = ImageLoaderFactory.getInstance(this)
            .newBuilder()
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
        Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()
        setContent {
            val repository = remember { com.furflix.app.data.repository.FurRepository.getInstance(this) }
            val themePref by repository.themeFlow.collectAsState(initial = com.furflix.app.ui.theme.ThemePreference.DEFAULT)
            val appIcon by repository.appIconFlow.collectAsState(initial = null)
            
            if (appIcon != null) {
                pendingAppIcon = appIcon
            }

            FurFlixTheme(themePreference = themePref) {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        FurFlixNavGraph()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        pendingAppIcon?.let { icon ->
            IconManager.applyPendingIcon(this, icon)
        }
    }
}
