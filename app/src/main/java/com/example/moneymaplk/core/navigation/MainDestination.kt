package com.example.moneymaplk.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.vector.ImageVector

data class MainDestination(
    val route: AppRoute,
    val label: String,
    val icon: ImageVector
)

val mainDestinations = listOf(
    MainDestination(AppRoute.Dashboard, "Home", Icons.Default.Home),
    MainDestination(AppRoute.PaymentFollowUps, "Follow Up", Icons.Default.List),
    MainDestination(AppRoute.Reports, "Reports", Icons.Default.List),
    MainDestination(AppRoute.RecurringPayments, "Recurrent", Icons.Default.Refresh)
)
