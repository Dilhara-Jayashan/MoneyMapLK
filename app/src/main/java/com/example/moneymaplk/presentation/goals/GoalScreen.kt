package com.example.moneymaplk.presentation.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moneymaplk.core.theme.MoneyMapGreen
import com.example.moneymaplk.core.ui.EmptyState
import com.example.moneymaplk.core.ui.ErrorState
import com.example.moneymaplk.core.ui.LoadingState
import com.example.moneymaplk.core.ui.MoneyMapPrimaryButton
import com.example.moneymaplk.core.ui.MoneyMapSecondaryButton
import com.example.moneymaplk.core.ui.MoneyMapTextField
import com.example.moneymaplk.core.util.formatLkr
import com.example.moneymaplk.core.util.formatWholePercent
import com.example.moneymaplk.domain.model.Goal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    goalViewModel: GoalViewModel,
    onRequireLogin: () -> Unit
) {
    val uiState by goalViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        goalViewModel.loadGoal()
    }

    LaunchedEffect(uiState.shouldReturnToLogin) {
        if (uiState.shouldReturnToLogin) {
            onRequireLogin()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(4000)
            goalViewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(5000)
            goalViewModel.clearErrorMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            goalViewModel.clearSuccessMessage()
            goalViewModel.clearErrorMessage()
        }
    }

    if (uiState.isAddGoalVisible) {
        AddGoalSheet(
            uiState = uiState,
            onDismiss = goalViewModel::hideAddGoalForm,
            onNameChange = goalViewModel::onNewGoalNameChange,
            onTargetAmountChange = goalViewModel::onNewGoalTargetAmountChange,
            onSavedAmountChange = goalViewModel::onNewGoalSavedAmountChange,
            onTargetDateChange = goalViewModel::onNewGoalTargetDateChange,
            onDescriptionChange = goalViewModel::onNewGoalDescriptionChange,
            onSaveClick = goalViewModel::createGoal
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        GoalHeader(onAddGoalClick = goalViewModel::showAddGoalForm)
        Spacer(modifier = Modifier.height(20.dp))

        when {
            uiState.isLoading && uiState.goals.isEmpty() -> {
                LoadingState(
                    message = "Loading your goals...",
                    modifier = Modifier.weight(1f)
                )
            }

            uiState.goals.isEmpty() && uiState.errorMessage != null -> {
                uiState.errorMessage?.let { message ->
                    ErrorState(
                        title = "Could not load goals",
                        message = message,
                        modifier = Modifier.weight(1f),
                        actionText = "Try Again",
                        onActionClick = goalViewModel::retryGoal
                    )
                }
            }

            uiState.goals.isEmpty() -> {
                EmptyGoalState(
                    isSaving = uiState.isSaving,
                    errorMessage = uiState.errorMessage,
                    successMessage = uiState.successMessage,
                    onCreateGoalClick = goalViewModel::createDefaultMacBookGoal,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                GoalContent(
                    uiState = uiState,
                    onGoalSelected = goalViewModel::onGoalSelected,
                    onShowOnHome = goalViewModel::showOnHome,
                    onContributionAmountChange = goalViewModel::onContributionAmountChange,
                    onAddContributionClick = goalViewModel::addContribution,
                    onRetryClick = goalViewModel::retryGoal,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GoalHeader(
    onAddGoalClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Goals",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Track the money you are setting aside for what matters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = onAddGoalClick,
            modifier = Modifier.widthIn(min = 116.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Add Goal")
        }
    }
}

@Composable
private fun GoalContent(
    uiState: GoalUiState,
    onGoalSelected: (String) -> Unit,
    onShowOnHome: (String) -> Unit,
    onContributionAmountChange: (String) -> Unit,
    onAddContributionClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedGoal = uiState.goal

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        uiState.errorMessage?.let { message ->
            item {
                ErrorState(
                    title = "Could not update goal",
                    message = message,
                    actionText = "Try Again",
                    onActionClick = onRetryClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        uiState.successMessage?.let { message ->
            item {
                StatusCard(
                    message = message,
                    color = MoneyMapGreen
                )
            }
        }

        item {
            Text(
                text = "Active Goals",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(
            items = uiState.goals,
            key = { goal -> goal.goalId }
        ) { goal ->
            GoalCard(
                goal = goal,
                isSelected = goal.goalId == uiState.selectedGoalId,
                isShownOnHome = goal.goalId == uiState.homeGoalId,
                isSaving = uiState.isSaving,
                onClick = { onGoalSelected(goal.goalId) },
                onShowOnHomeClick = { onShowOnHome(goal.goalId) }
            )
        }

        if (selectedGoal != null) {
            item {
                ContributionCard(
                    goal = selectedGoal,
                    amount = uiState.contributionAmount,
                    amountError = uiState.contributionError,
                    isSaving = uiState.isSaving,
                    onAmountChange = onContributionAmountChange,
                    onAddClick = onAddContributionClick
                )
            }

            item {
                GoalInsightCard(goal = selectedGoal)
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    isSelected: Boolean,
    isShownOnHome: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
    onShowOnHomeClick: () -> Unit
) {
    val progress = (goal.progressPercentage / 100.0).coerceIn(0.0, 1.0).toFloat()
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name.ifBlank { "Savings Goal" },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSelected) {
                        Text(
                            text = "Selected for contributions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isShownOnHome) {
                        Text(
                            text = "Shown on Home",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (goal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${formatWholePercent(goal.progressPercentage)} complete",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MoneyMapGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            GoalAmountRow(label = "Target amount", amount = goal.targetAmountLkr)
            Spacer(modifier = Modifier.height(8.dp))
            GoalAmountRow(label = "Saved amount", amount = goal.savedAmountLkr)
            Spacer(modifier = Modifier.height(8.dp))
            GoalAmountRow(label = "Remaining amount", amount = goal.remainingAmountLkr)
            Spacer(modifier = Modifier.height(8.dp))
            GoalTextRow(label = "Target date", value = formatGoalDate(goal))
            if (!isShownOnHome) {
                Spacer(modifier = Modifier.height(12.dp))
                MoneyMapSecondaryButton(
                    text = "Show on Home",
                    onClick = onShowOnHomeClick,
                    enabled = !isSaving
                )
            }
        }
    }
}

@Composable
private fun ContributionCard(
    goal: Goal,
    amount: String,
    amountError: String?,
    isSaving: Boolean,
    onAmountChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Add savings contribution",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = goal.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            MoneyMapTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = "Contribution amount in LKR",
                placeholder = "25000",
                errorText = amountError,
                keyboardType = KeyboardType.Decimal
            )
            Spacer(modifier = Modifier.height(12.dp))
            MoneyMapPrimaryButton(
                text = if (isSaving) "Adding..." else "Add to Goal",
                onClick = onAddClick,
                enabled = !isSaving && goal.remainingAmountLkr > 0.0
            )
        }
    }
}

@Composable
private fun GoalInsightCard(goal: Goal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Goal Insight",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = goalInsight(goal.progressPercentage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalSheet(
    uiState: GoalUiState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onTargetAmountChange: (String) -> Unit,
    onSavedAmountChange: (String) -> Unit,
    onTargetDateChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Add Goal",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            MoneyMapTextField(
                value = uiState.newGoalName,
                onValueChange = onNameChange,
                label = "Goal name",
                placeholder = "Emergency fund",
                errorText = uiState.newGoalNameError
            )
            MoneyMapTextField(
                value = uiState.newGoalTargetAmount,
                onValueChange = onTargetAmountChange,
                label = "Target amount in LKR",
                placeholder = "250000",
                errorText = uiState.newGoalTargetAmountError,
                keyboardType = KeyboardType.Decimal
            )
            MoneyMapTextField(
                value = uiState.newGoalSavedAmount,
                onValueChange = onSavedAmountChange,
                label = "Current saved amount in LKR",
                placeholder = "0",
                errorText = uiState.newGoalSavedAmountError,
                keyboardType = KeyboardType.Decimal
            )
            MoneyMapTextField(
                value = uiState.newGoalTargetDate,
                onValueChange = onTargetDateChange,
                label = "Target date",
                placeholder = "2026-12-25",
                errorText = uiState.newGoalTargetDateError
            )
            MoneyMapTextField(
                value = uiState.newGoalDescription,
                onValueChange = onDescriptionChange,
                label = "Description",
                placeholder = "Optional",
                singleLine = false
            )
            MoneyMapPrimaryButton(
                text = if (uiState.isSaving) "Saving..." else "Save Goal",
                onClick = onSaveClick,
                enabled = !uiState.isSaving
            )
            MoneyMapSecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                enabled = !uiState.isSaving
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun EmptyGoalState(
    isSaving: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onCreateGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        errorMessage?.let { message ->
            ErrorState(
                title = "Could not create goal",
                message = message,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        successMessage?.let { message ->
            StatusCard(
                message = message,
                color = MoneyMapGreen
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        EmptyState(
            title = "No goal found",
            message = "Create the MacBook Pro M4 goal to start tracking your savings.",
            actionText = if (isSaving) "Creating..." else "Create MacBook Goal",
            onActionClick = onCreateGoalClick
        )
    }
}

@Composable
private fun StatusCard(
    message: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GoalAmountRow(
    label: String,
    amount: Double
) {
    GoalTextRow(
        label = label,
        value = formatLkr(amount)
    )
}

@Composable
private fun GoalTextRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun goalInsight(progressPercentage: Double): String {
    return when {
        progressPercentage >= 100.0 -> "Goal completed."
        progressPercentage >= 50.0 -> "You are more than halfway there."
        progressPercentage > 0.0 -> "You are getting started. Keep building consistency."
        else -> "Start with your first contribution."
    }
}

private fun formatGoalDate(goal: Goal): String {
    goal.targetDate?.toDate()?.let { date ->
        return readableDateFormat().format(date)
    }

    val parsedDeadline = parseInputDate(goal.deadline)
    return if (parsedDeadline != null) {
        readableDateFormat().format(parsedDeadline)
    } else {
        "Target date not set"
    }
}

private fun parseInputDate(value: String): Date? {
    val cleanValue = value.trim()
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(cleanValue)) return null
    return runCatching {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        formatter.isLenient = false
        formatter.parse(cleanValue)
    }.getOrNull()
}

private fun readableDateFormat(): SimpleDateFormat {
    return SimpleDateFormat("dd MMM yyyy", Locale.US)
}
