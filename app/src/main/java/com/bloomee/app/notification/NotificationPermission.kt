package com.bloomee.app.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** True when the app is actually allowed to post notifications (channel + permission). */
fun notificationsEnabled(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

/**
 * Returns an action that requests POST_NOTIFICATIONS when it is actually needed
 * (Android 13+ and not yet granted). Call it from a contextual moment — e.g. the
 * user enables a reminder — instead of asking at app launch. [onDenied] runs when
 * the request completes without the permission, so the UI can say the reminder
 * won't fire rather than leaving a dead switch on.
 */
@Composable
fun rememberNotificationPermissionRequest(onDenied: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (!granted) onDenied() }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
