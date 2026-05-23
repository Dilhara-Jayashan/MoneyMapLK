package com.example.moneymaplk.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    tertiaryActionLabel: String? = null,
    onTertiaryAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium
        )
        if (primaryActionLabel != null && onPrimaryAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onPrimaryAction) {
                Text(primaryActionLabel)
            }
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onSecondaryAction) {
                Text(secondaryActionLabel)
            }
        }
        if (tertiaryActionLabel != null && onTertiaryAction != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onTertiaryAction) {
                Text(tertiaryActionLabel)
            }
        }
    }
}
