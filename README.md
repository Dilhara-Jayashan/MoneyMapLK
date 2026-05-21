# MoneyMap LK

MoneyMap LK is a Kotlin Android personal finance management app built for the SE3092 Android assignment. The app is designed for a Sri Lankan user who receives income from multiple sources, spends in different categories, tracks expected payments, manages recurring commitments, and saves toward financial goals.

The app uses Jetpack Compose, MVVM, Firebase Authentication, and Cloud Firestore. It follows a single Activity architecture with feature-based packages, reusable Compose UI components, and repository classes for Firebase access.

## Project Overview

MoneyMap LK helps users understand where their money comes from, where it goes, and how much is safe to spend after savings plans and emergency buffers. The app focuses on practical finance visibility rather than complex accounting.

Version 1 includes authentication, financial setup, transaction tracking, a real-time dashboard, goals, recurring payments, payment follow-ups, reports, profile settings, and Firestore security rules.

## Problem Scenario Summary

The target user is Kavindu Silva, a junior software engineer in Colombo. He earns from:

- Salary
- Freelance projects
- Google AdSense in USD
- Crypto
- Other occasional income sources

His main problems are irregular income, unclear expense visibility, forgotten payment follow-ups, recurring payments, and saving for a MacBook Pro M4 worth about LKR 490,000.

MoneyMap LK addresses this by combining income and expense tracking, LKR/USD conversion, safe-to-spend calculations, goal progress, recurring payment awareness, and payment follow-up tracking.

## Key Features

- Email and password registration and login
- Google Sign-In support, when Firebase Google provider is configured
- Forgot password reset email flow
- First-time financial setup
- LKR and USD money entry with exchange-rate conversion
- User profile and financial settings
- Income and expense transaction creation
- Transaction list with income and expense filters
- Dashboard with current savings, monthly income, monthly expenses, safe-to-spend, featured goal, recent transactions, spending awareness, and smart insights
- Multiple savings goals
- Selected goal support for the Home dashboard
- Goal contribution validation against available balance and remaining goal amount
- Recurring payment creation and pause/deactivation
- Payment follow-ups for expected money
- Automatic overdue display logic for unpaid follow-ups past the expected date
- Reports for income, expenses, net balance, savings rate, income sources, spending categories, recurring commitments, and follow-up summaries
- Profile editing and logout
- Centralized finance validation and calculation helpers
- Firestore security rules for owner-only access

## Tech Stack

- Language: Kotlin
- UI: Jetpack Compose
- Design system: Material 3
- Architecture: MVVM
- Navigation: Navigation Compose
- State management: StateFlow
- Async work: Kotlin Coroutines
- Authentication: Firebase Authentication
- Database: Cloud Firestore
- Build system: Gradle Kotlin DSL
- Minimum SDK: API 26

## Architecture Summary

The app follows this data flow:

```text
Composable UI -> ViewModel -> Repository -> Firebase Authentication / Cloud Firestore
```

Responsibilities are separated as follows:

- Composables display UI state and send user actions.
- ViewModels hold screen state, validation, loading flags, success/error messages, and calculations.
- Repositories handle Firebase Authentication and Firestore calls.
- Domain models represent app data such as transactions, goals, recurring payments, invoices, and user profiles.
- Central finance helpers keep validation and calculations consistent across the app.

Main package structure:

```text
com.example.moneymaplk
├── core
│   ├── navigation
│   ├── theme
│   └── ui
├── data
│   ├── firebase
│   └── repository
├── domain
│   ├── calculation
│   ├── model
│   └── validation
└── presentation
    ├── auth
    ├── dashboard
    ├── goals
    ├── invoices
    ├── profile
    ├── recurring
    ├── reports
    ├── setup
    └── transaction
```

## Firebase Setup Steps

1. Create a Firebase project.
2. Add an Android app using the package name:

```text
com.example.moneymaplk
```

3. Download `google-services.json`.
4. Place it here:

```text
app/google-services.json
```

5. Enable Firebase Authentication providers:

- Email/Password
- Google, if testing Google Sign-In

6. For Google Sign-In, add the debug/release SHA-1 and SHA-256 fingerprints in Firebase project settings.
7. Create a Cloud Firestore database.
8. Open `firestore.rules` from this project and publish it in the Firebase Console under Firestore Database -> Rules.
9. Run the app and create a test account.

## Firestore Collection Structure

MoneyMap LK stores each user's data under their Firebase Authentication user ID.

```text
users/{userId}
users/{userId}/transactions/{transactionId}
users/{userId}/goals/{goalId}
users/{userId}/recurringPayments/{paymentId}
users/{userId}/invoices/{invoiceId}
users/{userId}/monthlySummaries/{monthId}
```

Collection purposes:

- `users/{userId}`: user profile, setup status, preferred currency, savings setup, selected Home goal
- `transactions`: income and expense records
- `goals`: savings goals and goal progress
- `recurringPayments`: subscriptions and repeated commitments
- `invoices`: payment follow-ups for money expected from clients, refunds, platforms, or other people
- `monthlySummaries`: planned structure for monthly summary data

## Build and Run Instructions

Prerequisites:

- Android Studio
- JDK compatible with the Android Gradle Plugin
- Firebase project configured
- `app/google-services.json` present

Build debug APK:

```bash
./gradlew assembleDebug
```

Run from Android Studio:

1. Open the project folder in Android Studio.
2. Let Gradle sync.
3. Select an emulator or physical Android device.
4. Click **Run**.

If Firebase Authentication or Firestore calls fail, confirm that `google-services.json` matches the Firebase project and that the required Authentication providers are enabled.

## Demo Flow

Suggested demo sequence:

1. Open the app.
2. Register with email/password, or continue with Google if configured.
3. Complete financial setup:
   - Preferred currency
   - Current savings
   - Monthly salary baseline
   - Planned monthly savings
   - Emergency buffer
   - MacBook Pro M4 goal
4. View the Home dashboard.
5. Add an income transaction, such as salary or AdSense.
6. Add an expense transaction and choose required or flexible spending.
7. Open Activity to view and filter transactions.
8. Open Goals and add a contribution or create another goal.
9. Mark a goal as **Show on Home** and return to Home.
10. Add a recurring payment.
11. Add a payment follow-up and test waiting/overdue/paid states.
12. Open Reports and change the time filter.
13. Open Profile, edit financial settings, and logout.
14. Login again and confirm setup routing returns completed users to Home.

## Known Limitations

- The app does not automatically create income transactions when a payment follow-up is marked as paid.
- Recurring payments can be created and paused, but automatic scheduled transaction creation is not implemented.
- Reports use simple cards and bars rather than an advanced charting library.
- Currency conversion uses the exchange rate entered by the user; there is no live exchange-rate API.
- Payment follow-up reminders are stored as dates only; push notifications are not implemented.
- Firestore security rules are included in the project, but they must be published manually in Firebase Console.
- Hilt dependency injection is not used in the current implementation.
- Monthly summaries are part of the planned Firestore structure, but the app currently calculates dashboard and reports from live collections.

## Screens and Features included

- Splash screen with startup routing
- Login screen
- Register screen
- Forgot password dialog
- Google Sign-In entry point
- Financial setup screen
- Dashboard/Home screen
- Add transaction screen
- Transaction list/Activity screen
- Goals screen with multiple goals and Home goal selection
- Recurring payments screen
- Payment follow-ups screen
- Reports screen
- Profile and financial settings screen
- Logout flow
- Reusable Compose UI components
- MoneyMap LK theme
- Firestore schema documentation
- MVVM architecture plan
- Implementation roadmap
- Firestore security rules and deployment notes
 