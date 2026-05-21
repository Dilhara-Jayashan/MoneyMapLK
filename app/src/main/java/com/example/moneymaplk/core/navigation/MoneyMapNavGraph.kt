package com.example.moneymaplk.core.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.moneymaplk.presentation.auth.AuthViewModel
import com.example.moneymaplk.presentation.auth.LoginScreen
import com.example.moneymaplk.presentation.auth.RegisterScreen
import com.example.moneymaplk.presentation.auth.SplashScreen
import com.example.moneymaplk.presentation.dashboard.DashboardScreen
import com.example.moneymaplk.presentation.dashboard.DashboardViewModel
import com.example.moneymaplk.presentation.goals.GoalScreen
import com.example.moneymaplk.presentation.goals.GoalViewModel
import com.example.moneymaplk.presentation.paymentfollowup.PaymentFollowUpScreen
import com.example.moneymaplk.presentation.paymentfollowup.PaymentFollowUpViewModel
import com.example.moneymaplk.presentation.profile.ProfileScreen
import com.example.moneymaplk.presentation.profile.ProfileViewModel
import com.example.moneymaplk.presentation.recurring.RecurringPaymentsScreen
import com.example.moneymaplk.presentation.recurring.RecurringPaymentViewModel
import com.example.moneymaplk.presentation.reports.ReportsAnalyticsScreen
import com.example.moneymaplk.presentation.reports.ReportsViewModel
import com.example.moneymaplk.presentation.setup.FinancialSetupScreen
import com.example.moneymaplk.presentation.setup.SetupViewModel
import com.example.moneymaplk.presentation.transaction.AddTransactionScreen
import com.example.moneymaplk.presentation.transaction.TransactionListScreen
import com.example.moneymaplk.presentation.transaction.TransactionViewModel
import com.example.moneymaplk.domain.model.TransactionType

@Composable
fun MoneyMapNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel()
    val setupViewModel: SetupViewModel = viewModel()
    val dashboardViewModel: DashboardViewModel = viewModel()
    val transactionViewModel: TransactionViewModel = viewModel()
    val goalViewModel: GoalViewModel = viewModel()
    val reportsViewModel: ReportsViewModel = viewModel()
    val recurringPaymentViewModel: RecurringPaymentViewModel = viewModel()
    val paymentFollowUpViewModel: PaymentFollowUpViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    fun resetUserScopedState() {
        setupViewModel.resetSessionState()
        dashboardViewModel.resetSessionState()
        transactionViewModel.resetSessionState()
        goalViewModel.resetSessionState()
        reportsViewModel.resetSessionState()
        recurringPaymentViewModel.resetSessionState()
        paymentFollowUpViewModel.resetSessionState()
        profileViewModel.resetSessionState()
    }
    fun navigateToLoginClearingUserState() {
        resetUserScopedState()
        navController.navigate(AppRoute.Login.route) {
            popUpTo(0)
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = mainDestinations.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route.route } == true
    }
    val shouldNavigateHomeOnBack = mainDestinations
        .filterNot { destination -> destination.route == AppRoute.Dashboard }
        .any { destination ->
            currentDestination?.hierarchy?.any { it.route == destination.route.route } == true
        }

    BackHandler(enabled = shouldNavigateHomeOnBack) {
        navController.navigateToMainDestination(AppRoute.Dashboard)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                MoneyMapBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Splash.route) {
                SplashScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = {
                        navController.navigate(AppRoute.Login.route) {
                            popUpTo(AppRoute.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToSetup = {
                        resetUserScopedState()
                        navController.navigate(AppRoute.FinancialSetup.route) {
                            popUpTo(AppRoute.Splash.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToDashboard = {
                        resetUserScopedState()
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Splash.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppRoute.Login.route) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToSetup = {
                        resetUserScopedState()
                        navController.navigate(AppRoute.FinancialSetup.route) {
                            popUpTo(AppRoute.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToDashboard = {
                        resetUserScopedState()
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(AppRoute.Register.route)
                    }
                )
            }
            composable(AppRoute.Register.route) {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onRegisterSuccess = {
                        resetUserScopedState()
                        navController.navigate(AppRoute.FinancialSetup.route) {
                            popUpTo(AppRoute.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        resetUserScopedState()
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable(AppRoute.FinancialSetup.route) {
                FinancialSetupScreen(
                    setupViewModel = setupViewModel,
                    onSetupComplete = {
                        resetUserScopedState()
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.FinancialSetup.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onRequireLogin = {
                        navigateToLoginClearingUserState()
                    }
                )
            }
            composable(AppRoute.Dashboard.route) {
                DashboardScreen(
                    dashboardViewModel = dashboardViewModel,
                    onAddTransactionClick = {
                        navController.navigate(AppRoute.AddTransaction.route)
                    },
                    onViewAllTransactionsClick = {
                        navController.navigateToMainDestination(AppRoute.Transactions)
                    },
                    onCreateGoalClick = {
                        navController.navigate(AppRoute.Goals.route)
                    },
                    onRecurringPaymentsClick = {
                        navController.navigateToMainDestination(AppRoute.RecurringPayments)
                    },
                    onPaymentFollowUpsClick = {
                        navController.navigateToMainDestination(AppRoute.PaymentFollowUps)
                    },
                    onReportsClick = {
                        navController.navigateToMainDestination(AppRoute.Reports)
                    },
                    onProfileClick = {
                        navController.navigate(AppRoute.Profile.route)
                    },
                    onRequireLogin = {
                        navigateToLoginClearingUserState()
                    }
                )
            }
            composable(
                route = "${AppRoute.AddTransaction.route}?type={type}&committed={committed}",
                arguments = listOf(
                    navArgument("type") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("committed") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val initialType = when (backStackEntry.arguments?.getString("type")) {
                    "income" -> TransactionType.INCOME
                    "expense" -> TransactionType.EXPENSE
                    else -> null
                }
                AddTransactionScreen(
                    transactionViewModel = transactionViewModel,
                    onSaved = {
                        transactionViewModel.retryLoadTransactions()
                        dashboardViewModel.retryDashboard()
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Dashboard.route) { inclusive = true }
                        }
                    },
                    onBackClick = { navController.popBackStack() },
                    onRequireLogin = {
                        navigateToLoginClearingUserState()
                    },
                    initialType = initialType,
                    initialCommitted = backStackEntry.arguments?.getBoolean("committed") ?: false
                )
            }
            composable(AppRoute.Transactions.route) {
                TransactionListScreen(
                    transactionViewModel = transactionViewModel,
                    onAddTransaction = { navController.navigate(AppRoute.AddTransaction.route) },
                    onRequireLogin = {
                        navigateToLoginClearingUserState()
                    }
                )
            }
            composable(AppRoute.Goals.route) {
                GoalScreen(
                    goalViewModel = goalViewModel,
                    onRequireLogin = {
                        navigateToLoginClearingUserState()
                    }
                )
            }
            composable(AppRoute.Reports.route) {
                ReportsAnalyticsScreen(
                    reportsViewModel = reportsViewModel,
                    onRequireLogin = {
                        navigateToLoginClearingUserState()
                    }
                )
            }
            composable(AppRoute.RecurringPayments.route) {
                RecurringPaymentsScreen(
                    recurringPaymentViewModel = recurringPaymentViewModel,
                    onAddExpenseClick = {
                        navController.navigate("${AppRoute.AddTransaction.route}?type=expense&committed=true")
                    },
                    onAddIncomeClick = {
                        navController.navigate("${AppRoute.AddTransaction.route}?type=income&committed=true")
                    },
                    onBackClick = { navController.popBackStack() },
                    onRequireLogin = {
                        navigateToLoginClearingUserState()
                    }
                )
            }
            composable(AppRoute.PaymentFollowUps.route) {
                PaymentFollowUpScreen(
                    paymentFollowUpViewModel = paymentFollowUpViewModel,
                    onAddExpenseActivityClick = {
                        navController.navigate("${AppRoute.AddTransaction.route}?type=expense&committed=false")
                    },
                    onRecurringItemClick = {
                        navController.navigate(AppRoute.RecurringPayments.route)
                    },
                    onBackClick = { navController.popBackStack() },
                    onRequireLogin = {
                        navigateToLoginClearingUserState()
                    }
                )
            }
            composable(AppRoute.Profile.route) {
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    onLogoutComplete = {
                        navigateToLoginClearingUserState()
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MoneyMapBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var showAddMenu by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    destination = mainDestinations[0],
                    selected = currentDestination.isRouteSelected(mainDestinations[0].route),
                    onClick = { navController.navigateToMainDestination(mainDestinations[0].route) },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    destination = mainDestinations[1],
                    selected = currentDestination.isRouteSelected(mainDestinations[1].route),
                    onClick = { navController.navigateToMainDestination(mainDestinations[1].route) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(78.dp))
                BottomNavItem(
                    destination = mainDestinations[2],
                    selected = currentDestination.isRouteSelected(mainDestinations[2].route),
                    onClick = { navController.navigateToMainDestination(mainDestinations[2].route) },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    destination = mainDestinations[3],
                    selected = currentDestination.isRouteSelected(mainDestinations[3].route),
                    onClick = { navController.navigateToMainDestination(mainDestinations[3].route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(66.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { showAddMenu = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(34.dp)
            )
        }
    }

    if (showAddMenu) {
        ModalBottomSheet(onDismissRequest = { showAddMenu = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Money Activity",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                AddMenuAction(
                    title = "Expense",
                    description = "Add a discretionary payment",
                    icon = Icons.Default.KeyboardArrowDown,
                    onClick = {
                        showAddMenu = false
                        navController.navigate("${AppRoute.AddTransaction.route}?type=expense&committed=false")
                    }
                )
                AddMenuAction(
                    title = "Income",
                    description = "Add received income",
                    icon = Icons.Default.KeyboardArrowUp,
                    onClick = {
                        showAddMenu = false
                        navController.navigate("${AppRoute.AddTransaction.route}?type=income&committed=false")
                    }
                )
                AddMenuAction(
                    title = "Committed Payment",
                    description = "Create a recurrent payment",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        showAddMenu = false
                        navController.navigate("${AppRoute.AddTransaction.route}?type=expense&committed=true")
                    }
                )
                AddMenuAction(
                    title = "Recurrent Income",
                    description = "Create repeating income",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        showAddMenu = false
                        navController.navigate("${AppRoute.AddTransaction.route}?type=income&committed=true")
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    destination: MainDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AddMenuAction(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun androidx.navigation.NavDestination?.isRouteSelected(route: AppRoute): Boolean {
    return this?.hierarchy?.any { destination ->
        destination.route == route.route
    } == true
}

private fun NavHostController.navigateToMainDestination(route: AppRoute) {
    navigate(route.route) {
        popUpTo(AppRoute.Dashboard.route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
