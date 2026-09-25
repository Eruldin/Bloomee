package com.bloomee.app.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Returns an action that requests POST_NOTIFICATIONS when it is actually needed
 * (Android 13+ and not yet granted). Call it from a contextual moment — e.g. the
 * user enables a reminder — instead of asking at app launch.
 */
@Composable
fun rememberNotificationPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
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
