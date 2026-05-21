package com.example.moneymaplk.core.navigation

sealed class AppRoute(val route: String) {
    data object Splash : AppRoute("splash")
    data object Login : AppRoute("login")
    data object Register : AppRoute("register")
    data object FinancialSetup : AppRoute("financial_setup")
    data object Dashboard : AppRoute("dashboard")
    data object AddTransaction : AppRoute("add_transaction")
    data object Transactions : AppRoute("transactions")
    data object Goals : AppRoute("goals")
    data object Reports : AppRoute("reports")
    data object RecurringPayments : AppRoute("recurring_payments")
    data object PaymentFollowUps : AppRoute("paymentFollowUps")
    data object Profile : AppRoute("profile")
}
