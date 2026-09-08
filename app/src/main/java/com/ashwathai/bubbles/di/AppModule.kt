package com.ashwathai.bubbles.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.ashwathai.bubbles.data.local.EconomyRepositoryImpl
import com.ashwathai.bubbles.data.local.ScoreRepositoryImpl
import com.ashwathai.bubbles.data.local.SettingsRepositoryImpl
import com.ashwathai.bubbles.data.sound.SoundManager
import com.ashwathai.bubbles.domain.repository.EconomyRepository
import com.ashwathai.bubbles.domain.repository.ScoreRepository
import com.ashwathai.bubbles.domain.repository.SettingsRepository
import com.ashwathai.bubbles.domain.usecase.ActivatePowerUpUseCase
import com.ashwathai.bubbles.domain.usecase.CanSpawnBubbleUseCase
import com.ashwathai.bubbles.domain.usecase.CheckLevelCompleteUseCase
import com.ashwathai.bubbles.domain.usecase.CheckPowerUpExpirationUseCase
import com.ashwathai.bubbles.domain.usecase.HandleTapUseCase
import com.ashwathai.bubbles.domain.usecase.SpawnBubblesUseCase
import com.ashwathai.bubbles.domain.usecase.UpdateBubblesUseCase
import com.ashwathai.bubbles.domain.usecase.UpdateMessagesUseCase
import com.ashwathai.bubbles.domain.usecase.UpdateParticlesUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

object AppModule {

    private val Context.bubblesDataStore: DataStore<Preferences> by preferencesDataStore(name = "bubbles_prefs")

    fun provideDataStore(context: Context): DataStore<Preferences> {
        return context.bubblesDataStore
    }

    fun provideScoreRepository(dataStore: DataStore<Preferences>): ScoreRepository {
        return ScoreRepositoryImpl(dataStore)
    }

    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository {
        return SettingsRepositoryImpl(dataStore)
    }

    fun provideEconomyRepository(dataStore: DataStore<Preferences>): EconomyRepository {
        return EconomyRepositoryImpl(dataStore)
    }

    fun provideSoundManager(context: Context): SoundManager {
        return SoundManager(context)
    }

    fun provideSpawnBubblesUseCase(): SpawnBubblesUseCase {
        return SpawnBubblesUseCase()
    }

    fun provideUpdateBubblesUseCase(): UpdateBubblesUseCase {
        return UpdateBubblesUseCase()
    }

    fun provideHandleTapUseCase(): HandleTapUseCase {
        return HandleTapUseCase()
    }

    fun provideUpdateMessagesUseCase(): UpdateMessagesUseCase {
        return UpdateMessagesUseCase()
    }

    fun provideUpdateParticlesUseCase(): UpdateParticlesUseCase {
        return UpdateParticlesUseCase()
    }

    fun provideCheckLevelCompleteUseCase(): CheckLevelCompleteUseCase {
        return CheckLevelCompleteUseCase()
    }

    fun provideCanSpawnBubbleUseCase(): CanSpawnBubbleUseCase {
        return CanSpawnBubbleUseCase()
    }

    fun provideActivatePowerUpUseCase(): ActivatePowerUpUseCase {
        return ActivatePowerUpUseCase()
    }

    fun provideCheckPowerUpExpirationUseCase(): CheckPowerUpExpirationUseCase {
        return CheckPowerUpExpirationUseCase()
    }

    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}