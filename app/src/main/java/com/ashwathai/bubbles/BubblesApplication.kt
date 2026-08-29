package com.ashwathai.bubbles

import android.app.Application

class BubblesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        LevelPlayAdManager.init(this)
    }
}
