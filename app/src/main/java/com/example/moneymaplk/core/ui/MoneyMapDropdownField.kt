package com.example.moneymaplk.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MoneyMapDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select category",
    errorText: String? = null,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = selectedValue.ifBlank { placeholder },
                    modifier = Modifier.weight(1f),
                    color = if (selectedValue.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option) },
                        onClick = {
                            expanded = false
                            onOptionSelected(option)
                        }
                    )
                }
            }
        }
        errorText?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
