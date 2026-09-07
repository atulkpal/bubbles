package com.ashwathai.bubbles

import android.app.Application
import com.ashwathai.bubbles.data.local.SettingsRepositoryImpl
import com.ashwathai.bubbles.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BubblesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        LevelPlayAdManager.init(this)

        // Init billing: check persisted adsRemoved flag first
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val dataStore = AppModule.provideDataStore(this@BubblesApplication)
            val settingsRepo = SettingsRepositoryImpl(dataStore)
            val adsRemovedPersisted = settingsRepo.adsRemoved.first()
            BillingManager.init(this@BubblesApplication, adsRemovedPersisted)

            // When billing confirms a new purchase, persist it
            BillingManager.onPurchaseAcknowledged = {
                scope.launch { settingsRepo.setAdsRemoved(true) }
            }
        }
    }
}
