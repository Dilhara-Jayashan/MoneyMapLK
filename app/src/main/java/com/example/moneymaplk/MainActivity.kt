package com.example.moneymaplk

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.moneymaplk.core.notifications.RecurrentNotificationHelper
import com.example.moneymaplk.core.notifications.RecurrentWorkScheduler
import com.example.moneymaplk.core.navigation.MoneyMapNavGraph
import com.example.moneymaplk.core.theme.MoneyMapLKTheme

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        RecurrentWorkScheduler.schedule(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RecurrentNotificationHelper.createChannel(this)
        requestNotificationPermissionIfNeeded()
        RecurrentWorkScheduler.schedule(this)
        setContent {
            MoneyMapLKTheme {
                MoneyMapNavGraph()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !RecurrentNotificationHelper.canPostNotifications(this)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
