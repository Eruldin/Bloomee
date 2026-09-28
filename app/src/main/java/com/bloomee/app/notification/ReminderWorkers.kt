package com.bloomee.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bloomee.app.BloomeeApplication
import com.bloomee.app.domain.hydration.HydrationCalculator
import com.bloomee.app.domain.model.UserProfile
import com.bloomee.app.domain.prediction.CyclePredictor
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class HydrationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Quiet hours: a water nudge at 03:00 is worse than a missed reminder —
        // most users disable the channel entirely when it wakes them at night.
        val hour = LocalTime.now().hour
        if (hour >= QUIET_HOURS_START || hour < QUIET_HOURS_END) return Result.success()

        val container = (applicationContext as BloomeeApplication).container
        val profile = container.userPreferencesRepository.profile.first()
        if (!profile.reminderHydrationEnabled) return Result.success()

        val today = LocalDate.now()
        val hydration = container.hydrationRepository.days.first().firstOrNull { it.date == today }
        val consumed = hydration?.consumedMl ?: 0
        val goal = hydration?.goalMl
            ?: HydrationCalculator.dailyGoalMl(profile.weightKg, profile.activityLevel)
        if (consumed >= goal) return Result.success()

        val remaining = goal - consumed
        Notifications.show(
            context = applicationContext,
            channelId = Notifications.CHANNEL_HYDRATION,
            notificationId = 1001,
            title = "Su molası",
            text = "Bugünkü hedefine $remaining ml kaldı. Bir bardak su iyi gelir."
        )
        return Result.success()
    }

    private companion object {
        const val QUIET_HOURS_START = 22
        const val QUIET_HOURS_END = 8
    }
}

class CycleReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as BloomeeApplication).container
        val profile = container.userPreferencesRepository.profile.first()
        val logs = container.cycleRepository.logs.first()
        val stats = CyclePredictor.calculate(
            logs = logs,
            defaultCycleLength = profile.defaultCycleLength,
            defaultPeriodLength = profile.defaultPeriodLength
        )

        if (profile.reminderPeriodEnabled) {
            when (stats.daysToNextPeriod) {
                2 -> Notifications.show(
                    applicationContext,
                    Notifications.CHANNEL_CYCLE,
                    2001,
                    "Regl yaklaşıyor",
                    "Tahmine göre 2 gün kaldı. Hazırlığını şimdiden yapabilirsin."
                )

                0 -> Notifications.show(
                    applicationContext,
                    Notifications.CHANNEL_CYCLE,
                    2002,
                    "Tahmini regl günü",
                    "Bugün regl beklentisi var. Başladıysa günlüğüne işaretlemeyi unutma."
                )
            }
        }

        return Result.success()
    }
}

/**
 * Fires once at the configured hour and re-arms itself for the next day.
 * A one-shot chain survives WorkManager's flex windows better than an hourly
 * periodic job that asks "is it the right hour now" — a delayed run used to
 * silently skip the reminder for the whole day.
 */
class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as BloomeeApplication).container
        val profile = container.userPreferencesRepository.profile.first()

        if (profile.reminderMedicationEnabled) {
            ReminderScheduler.scheduleMedication(applicationContext, profile.medicationReminderHour)
            Notifications.show(
                applicationContext,
                Notifications.CHANNEL_MEDICATION,
                3001,
                "İlaç hatırlatması",
                "Günlük ilaç/takviye zamanın geldi."
            )
        }

        return Result.success()
    }
}

object ReminderScheduler {

    private const val HYDRATION_WORK = "bloomee_hydration_reminder"
    private const val CYCLE_WORK = "bloomee_cycle_reminder"
    private const val MEDICATION_WORK = "bloomee_medication_reminder"

    fun schedule(context: Context, profile: UserProfile) {
        val workManager = WorkManager.getInstance(context)

        if (profile.reminderHydrationEnabled) {
            workManager.enqueueUniquePeriodicWork(
                HYDRATION_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<HydrationReminderWorker>(3, TimeUnit.HOURS).build()
            )
        } else {
            workManager.cancelUniqueWork(HYDRATION_WORK)
        }

        if (profile.reminderPeriodEnabled) {
            workManager.enqueueUniquePeriodicWork(
                CYCLE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<CycleReminderWorker>(1, TimeUnit.HOURS)
                    .setInitialDelay(initialDelayToNextHour())
                    .build()
            )
        } else {
            workManager.cancelUniqueWork(CYCLE_WORK)
        }

        if (profile.reminderMedicationEnabled) {
            scheduleMedication(context, profile.medicationReminderHour)
        } else {
            workManager.cancelUniqueWork(MEDICATION_WORK)
        }
    }

    fun scheduleMedication(context: Context, hour: Int) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            MEDICATION_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                .setInitialDelay(delayToNextOccurrence(hour))
                .build()
        )
    }

    internal fun delayToNextOccurrence(hour: Int): Duration {
        val now = LocalDateTime.now()
        var target = now.withHour(hour.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target)
    }

    private fun initialDelayToNextHour(): Duration {
        val now = LocalDateTime.now()
        val nextHour = now.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1)
        return Duration.between(now, nextHour)
    }
}
