package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.data.FirebaseManager
import com.example.fcm.NotificationHelper

class NeedYouApp : Application(), Application.ActivityLifecycleCallbacks {

    private var activeActivitiesCount = 0

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        if (activeActivitiesCount == 0) {
            FirebaseManager.getInstance(this).setAppForegroundStatus(true)
        }
        activeActivitiesCount++
    }

    override fun onActivityStopped(activity: Activity) {
        activeActivitiesCount--
        if (activeActivitiesCount == 0) {
            FirebaseManager.getInstance(this).setAppForegroundStatus(false)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
