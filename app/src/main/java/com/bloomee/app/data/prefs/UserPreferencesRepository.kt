package com.bloomee.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bloomee.app.domain.model.ActivityLevel
import com.bloomee.app.domain.model.ThemeMode
import com.bloomee.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bloomee_profile")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val displayName = stringPreferencesKey("display_name")
        val birthYear = intPreferencesKey("birth_year")
        val weightKg = doublePreferencesKey("weight_kg")
        val heightCm = intPreferencesKey("height_cm")
        val activityLevel = stringPreferencesKey("activity_level")
        val cycleLength = intPreferencesKey("cycle_length")
        val periodLength = intPreferencesKey("period_length")
        val hydrationGoal = intPreferencesKey("hydration_goal")
        val partnerMode = booleanPreferencesKey("partner_mode")
        val partnerName = stringPreferencesKey("partner_name")
        val reminderHydration = booleanPreferencesKey("reminder_hydration")
        val reminderPeriod = booleanPreferencesKey("reminder_period")
        val reminderMedication = booleanPreferencesKey("reminder_medication")
        val medicationHour = intPreferencesKey("medication_hour")
        val cloudSync = booleanPreferencesKey("cloud_sync")
        val assistantApiKey = stringPreferencesKey("assistant_api_key")
        val themeName = stringPreferencesKey("theme_name")
        val themeMode = stringPreferencesKey("theme_mode")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
    }

    val profile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            displayName = prefs[Keys.displayName].orEmpty(),
            birthYear = prefs[Keys.birthYear],
            weightKg = prefs[Keys.weightKg],
            heightCm = prefs[Keys.heightCm],
            activityLevel = prefs[Keys.activityLevel]
                ?.let { name -> ActivityLevel.entries.firstOrNull { it.name == name } }
                ?: ActivityLevel.MODERATE,
            defaultCycleLength = prefs[Keys.cycleLength] ?: 28,
            defaultPeriodLength = prefs[Keys.periodLength] ?: 5,
            hydrationGoalMl = prefs[Keys.hydrationGoal] ?: 2000,
            partnerModeEnabled = prefs[Keys.partnerMode] ?: false,
            partnerName = prefs[Keys.partnerName].orEmpty(),
            reminderHydrationEnabled = prefs[Keys.reminderHydration] ?: true,
            reminderPeriodEnabled = prefs[Keys.reminderPeriod] ?: true,
            reminderMedicationEnabled = prefs[Keys.reminderMedication] ?: false,
            medicationReminderHour = prefs[Keys.medicationHour] ?: 21,
            cloudSyncEnabled = prefs[Keys.cloudSync] ?: false,
            assistantApiKey = prefs[Keys.assistantApiKey].orEmpty(),
            themeName = prefs[Keys.themeName] ?: "rose",
            themeMode = ThemeMode.fromName(prefs[Keys.themeMode]),
            onboardingCompleted = prefs[Keys.onboardingCompleted] ?: false
        )
    }

    suspend fun update(transform: (UserProfile) -> UserProfile) {
        context.dataStore.edit { prefs ->
            val current = UserProfile(
                displayName = prefs[Keys.displayName].orEmpty(),
                birthYear = prefs[Keys.birthYear],
                weightKg = prefs[Keys.weightKg],
                heightCm = prefs[Keys.heightCm],
                activityLevel = prefs[Keys.activityLevel]
                    ?.let { name -> ActivityLevel.entries.firstOrNull { it.name == name } }
                    ?: ActivityLevel.MODERATE,
                defaultCycleLength = prefs[Keys.cycleLength] ?: 28,
                defaultPeriodLength = prefs[Keys.periodLength] ?: 5,
                hydrationGoalMl = prefs[Keys.hydrationGoal] ?: 2000,
                partnerModeEnabled = prefs[Keys.partnerMode] ?: false,
                partnerName = prefs[Keys.partnerName].orEmpty(),
                reminderHydrationEnabled = prefs[Keys.reminderHydration] ?: true,
                reminderPeriodEnabled = prefs[Keys.reminderPeriod] ?: true,
                reminderMedicationEnabled = prefs[Keys.reminderMedication] ?: false,
                medicationReminderHour = prefs[Keys.medicationHour] ?: 21,
                cloudSyncEnabled = prefs[Keys.cloudSync] ?: false,
                assistantApiKey = prefs[Keys.assistantApiKey].orEmpty(),
                themeName = prefs[Keys.themeName] ?: "rose",
                themeMode = ThemeMode.fromName(prefs[Keys.themeMode]),
                onboardingCompleted = prefs[Keys.onboardingCompleted] ?: false
            )
            val updated = transform(current)

            prefs[Keys.displayName] = updated.displayName
            updated.birthYear?.let { prefs[Keys.birthYear] = it }
            updated.weightKg?.let { prefs[Keys.weightKg] = it }
            updated.heightCm?.let { prefs[Keys.heightCm] = it }
            prefs[Keys.activityLevel] = updated.activityLevel.name
            prefs[Keys.cycleLength] = updated.defaultCycleLength
            prefs[Keys.periodLength] = updated.defaultPeriodLength
            prefs[Keys.hydrationGoal] = updated.hydrationGoalMl
            prefs[Keys.partnerMode] = updated.partnerModeEnabled
            prefs[Keys.partnerName] = updated.partnerName
            prefs[Keys.reminderHydration] = updated.reminderHydrationEnabled
            prefs[Keys.reminderPeriod] = updated.reminderPeriodEnabled
            prefs[Keys.reminderMedication] = updated.reminderMedicationEnabled
            prefs[Keys.medicationHour] = updated.medicationReminderHour
            prefs[Keys.cloudSync] = updated.cloudSyncEnabled
            prefs[Keys.assistantApiKey] = updated.assistantApiKey
            prefs[Keys.themeName] = updated.themeName
            prefs[Keys.themeMode] = updated.themeMode.name
            prefs[Keys.onboardingCompleted] = updated.onboardingCompleted
        }
    }
}
