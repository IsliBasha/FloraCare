package com.floracare.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FloraCareApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createCareChannels()
    }

    private fun createCareChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        CareChannel.entries.forEach { ch ->
            val existing = manager.getNotificationChannel(ch.id)
            if (existing == null) {
                manager.createNotificationChannel(
                    NotificationChannel(ch.id, ch.title, NotificationManager.IMPORTANCE_DEFAULT)
                        .apply { description = ch.description }
                )
            }
        }
    }
}

enum class CareChannel(val id: String, val title: String, val description: String) {
    WATER("care_water", "Watering", "Reminders to water your plants"),
    FERTILIZE("care_fertilize", "Fertilizing", "Feed schedule reminders"),
    OTHER("care_other", "Other care", "Misting, rotating, repotting"),
}
