package com.furflix.app

import android.app.Application
import coil.Coil
import com.furflix.app.data.remote.FurAffinityScraper
import com.furflix.app.ui.components.ImageLoaderFactory

class FurFlixApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        FurAffinityScraper.init(this)
        // Route ALL Coil loads (even bare AsyncImage calls) through our loader,
        // which carries FA headers/cookies and the GIF decoder for animated avatars.
        Coil.setImageLoader(ImageLoaderFactory.getInstance(this))
    }

    companion object {
        lateinit var instance: FurFlixApp
            private set
    }
}
