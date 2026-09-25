package com.bloomee.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bloomee.app.BloomeeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val application = context.applicationContext as? BloomeeApplication ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ReminderScheduler.schedule(context, application.container.userPreferencesRepository.profile.first())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
