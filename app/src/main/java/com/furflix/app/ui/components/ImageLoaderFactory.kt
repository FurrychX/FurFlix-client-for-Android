@file:OptIn(coil.annotation.ExperimentalCoilApi::class)

package com.furflix.app.ui.components

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import com.furflix.app.data.remote.FurAffinityScraper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ImageLoaderFactory {

    private var imageLoader: ImageLoader? = null

    fun getInstance(context: Context): ImageLoader {
        return imageLoader ?: createLoader(context).also { imageLoader = it }
    }

    fun reset() {
        val loader = imageLoader
        imageLoader = null
        loader?.memoryCache?.clear()
        loader?.diskCache?.clear()
    }

    private fun createLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                // Animated avatars: GIFs need an explicit decoder
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(coil.decode.ImageDecoderDecoder.Factory())
                } else {
                    add(coil.decode.GifDecoder.Factory())
                }
            }
            .memoryCache {
                coil.memory.MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .okHttpClient(
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                            .header("Referer", "https://www.furaffinity.net/")
                            .build()

                        val cookiesStr = FurAffinityScraper.getCookiesString()
                        val finalRequest = if (cookiesStr.isNotEmpty()) {
                            request.newBuilder()
                                .header("Cookie", cookiesStr)
                                .build()
                        } else {
                            request
                        }

                        chain.proceed(finalRequest)
                    }
                    .build()
            )
            .diskCache(
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.25)
                    .build()
            )
            .crossfade(true)
            .build()
    }
}
