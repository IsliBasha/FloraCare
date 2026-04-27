package com.floracare.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.floracare.app.BuildConfig
import com.floracare.app.data.prefs.UserPrefs
import com.floracare.app.data.prefs.UserPrefsDataStore
import com.floracare.app.ui.feature.settings.SettingsViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Single process-wide DataStore<Preferences> instance bound to the app context.
 * Using the `preferencesDataStore` property delegate enforces singleton per name.
 */
private val Context.userPreferences: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFS_NAME,
)

private const val USER_PREFS_NAME = "user_prefs"

@Module
@InstallIn(SingletonComponent::class)
object UserPrefsModule {

    @Provides @Singleton
    fun provideUserPrefsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.userPreferences

    @Provides @Singleton
    fun provideUserPrefs(dataStore: DataStore<Preferences>): UserPrefs =
        UserPrefsDataStore(dataStore)

    @Provides @Singleton
    @Named(SettingsViewModel.QUALIFIER_APP_VERSION)
    fun provideAppVersion(): String = BuildConfig.VERSION_NAME
}
