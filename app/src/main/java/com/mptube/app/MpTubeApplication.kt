package com.mptube.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.bitmapFactoryMaxParallelism
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.memoryCacheMaxSizePercentWhileInBackground
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.request.crossfade

class MpTubeApplication : Application(), SingletonImageLoader.Factory
{
    override fun newImageLoader(context: Context): ImageLoader
    {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val lowRamDevice = activityManager?.isLowRamDevice == true ||
            Runtime.getRuntime().maxMemory() <= LOW_MEMORY_HEAP_BYTES

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, if (lowRamDevice) 0.06 else 0.10)
                    .build()
            }
            .memoryCacheMaxSizePercentWhileInBackground(0.25)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil_images"))
                    .maxSizeBytes(if (lowRamDevice) 32L * MB else 48L * MB)
                    .build()
            }
            .bitmapFactoryMaxParallelism(if (lowRamDevice) 1 else 2)
            .allowHardware(!lowRamDevice)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }

    companion object
    {
        private const val MB = 1024L * 1024L
        private const val LOW_MEMORY_HEAP_BYTES = 256L * MB
    }
}
