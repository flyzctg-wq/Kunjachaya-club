package com.example

import android.app.Application
import android.util.Log
import com.example.util.NotificationHelper
import com.google.firebase.FirebaseApp

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            NotificationHelper.createNotificationChannels(this)
            NotificationHelper.initializeFcmTopics(this)
            Log.d("MainApplication", "Firebase and Notification Channels initialized successfully.")
        } catch (e: Exception) {
            Log.e("MainApplication", "Error during MainApplication onCreate", e)
        }
    }
}
