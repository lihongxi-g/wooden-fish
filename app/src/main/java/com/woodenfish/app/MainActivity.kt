package com.woodenfish.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.woodenfish.app.ui.WoodenFishScreen

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: WoodenFishViewModel

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val prefs = PreferencesManager(this)
        if (granted || !prefs.isNotificationEnabled()) {
            NotificationHelper(this).scheduleNotifications(prefs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = WoodenFishViewModel(application)
        NotificationHelper.createChannel(this)

        // Request notification permission on first launch (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Mark first launch
        val prefs = PreferencesManager(this)
        if (prefs.isFirstLaunch()) {
            prefs.markLaunched()
        }

        setContent {
            WoodenFishScreen(viewModel = viewModel)
        }
    }
}
