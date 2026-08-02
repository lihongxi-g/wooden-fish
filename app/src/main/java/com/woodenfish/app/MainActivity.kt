package com.woodenfish.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.woodenfish.app.ui.WoodenFishScreen

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: WoodenFishViewModel

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // User granted — schedule notifications if enabled
            val prefs = PreferencesManager(this)
            NotificationHelper(this).scheduleNotifications(prefs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = WoodenFishViewModel(this)

        // Create notification channel early
        NotificationHelper.createChannel(this)

        // Request notification permission on first launch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val prefs = PreferencesManager(this)
            if (prefs.isFirstLaunch()) {
                prefs.markLaunched()
                requestNotificationPermission()
            }
        }

        setContent {
            WoodenFishScreen(viewModel = viewModel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
