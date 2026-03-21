package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.club360fit.app.ui.theme.BurgundyPrimary

private val standardGoals = listOf(
    "Weight loss",
    "Build muscle & strength",
    "General fitness & health",
    "Improve endurance / cardio",
    "Sports performance",
    "Rehabilitation / mobility",
    "Not sure yet"
)

@Composable
fun ClientGoalDropdown(
    selectedGoal: String?,
    onGoalSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember(selectedGoal) {
        val g = selectedGoal?.trim()
        if (g.isNullOrBlank()) standardGoals
        else if (g in standardGoals) standardGoals
        else listOf(g) + standardGoals.filter { it != g }
    }
    var expanded by remember { mutableStateOf(false) }
    val display = selectedGoal?.trim()?.takeIf { it.isNotBlank() }
        ?: options.firstOrNull()

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = display ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Goal") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Open goal list")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedLabelColor = BurgundyPrimary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTrailingIconColor = BurgundyPrimary,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = BurgundyPrimary,
                cursorColor = BurgundyPrimary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onGoalSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
