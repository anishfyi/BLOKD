package dev.anishfyi.blokd

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.anishfyi.blokd.block.BlocklistUpdateWorker
import java.util.concurrent.TimeUnit

class BlokdApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleBlocklistUpdates()
    }

    private fun scheduleBlocklistUpdates() {
        val request = PeriodicWorkRequestBuilder<BlocklistUpdateWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BlocklistUpdateWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
