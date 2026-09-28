package com.bloomee.app

import android.app.Application
import android.content.Context
import com.bloomee.app.assistant.AssistantClient
import com.bloomee.app.data.backup.BackupRepository
import com.bloomee.app.data.local.BloomeeDatabase
import com.bloomee.app.data.prefs.UserPreferencesRepository
import com.bloomee.app.data.repository.CycleRepository
import com.bloomee.app.data.repository.HydrationRepository
import com.bloomee.app.data.repository.NutritionRepository
import com.bloomee.app.data.sync.CloudSync
import com.bloomee.app.data.sync.FirebaseCloudSync
import com.bloomee.app.notification.Notifications
import com.bloomee.app.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val database = BloomeeDatabase.get(context)

    val userPreferencesRepository = UserPreferencesRepository(context)
    val cloudSync: CloudSync = FirebaseCloudSync(context, userPreferencesRepository)
    val cycleRepository = CycleRepository(database.dailyLogDao(), cloudSync)
    val hydrationRepository = HydrationRepository(database.hydrationDao(), cloudSync)
    val nutritionRepository = NutritionRepository(database.nutritionDao(), cloudSync)
    val backupRepository = BackupRepository(
        context,
        cycleRepository,
        hydrationRepository,
        nutritionRepository
    )
    val assistantClient = AssistantClient()
}

class BloomeeApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private companion object {
        const val TOMBSTONE_RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.ensureChannels(this)

        scope.launch {
            val profile = container.userPreferencesRepository.profile.first()
            container.cloudSync.setEnabled(profile.cloudSyncEnabled)
            ReminderScheduler.schedule(this@BloomeeApplication, profile)

            // Drop deletion markers that have outlived every device that could
            // still resurrect the record — independent of sync being enabled.
            val cutoff = System.currentTimeMillis() - TOMBSTONE_RETENTION_MS
            container.cycleRepository.pruneTombstones(cutoff)
            container.hydrationRepository.pruneTombstones(cutoff)
            container.nutritionRepository.pruneTombstones(cutoff)

            if (profile.cloudSyncEnabled) {
                container.cloudSync.syncNow(
                    container.cycleRepository,
                    container.hydrationRepository,
                    container.nutritionRepository
                )
            }
        }
    }
}
