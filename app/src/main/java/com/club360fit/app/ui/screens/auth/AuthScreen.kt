package com.club360fit.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.theme.White
import com.club360fit.app.ui.utils.fromFeetInches
import com.club360fit.app.ui.utils.fromPounds
import com.club360fit.app.ui.utils.toFeetInches
import com.club360fit.app.ui.utils.toPounds

@Composable
fun AuthScreen(
    isSignIn: Boolean,
    onAuthSuccess: (Boolean) -> Unit,
    onNavigateToCreateAccount: (() -> Unit)? = null,
    onNavigateToSignIn: (() -> Unit)? = null,
    onBack: () -> Unit,
    viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = if (isSignIn) "Sign in" else "Create account",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::updateEmail,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BurgundyPrimary,
                    focusedLabelColor = BurgundyPrimary,
                    cursorColor = BurgundyPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::updatePassword,
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BurgundyPrimary,
                    focusedLabelColor = BurgundyPrimary,
                    cursorColor = BurgundyPrimary
                )
            )
            if (isSignIn) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { viewModel.sendPasswordResetEmail(state.email) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = BurgundyPrimary
                    )
                ) {
                    Text("Forgot password?")
                }
                if (state.resetEmailSent) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Check your email for a reset link.", color = MaterialTheme.colorScheme.primary)
                }
                state.resetErrorMessage?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }

            if (!isSignIn) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Profile (optional for MVP)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = state.name, onValueChange = viewModel::updateName, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BurgundyPrimary, focusedLabelColor = BurgundyPrimary, cursorColor = BurgundyPrimary))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.age, 
                        onValueChange = viewModel::updateAge, 
                        label = { Text("Age") }, 
                        modifier = Modifier.weight(1f), 
                        singleLine = true, 
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BurgundyPrimary, focusedLabelColor = BurgundyPrimary, cursorColor = BurgundyPrimary)
                    )

                    // Imperial Height
                    val heightCm = state.height.toIntOrNull() ?: 0
                    val (feet, inches) = heightCm.toFeetInches()
                    var feetText by remember(heightCm) { mutableStateOf(if (feet > 0) feet.toString() else "") }
                    var inchesText by remember(heightCm) { mutableStateOf(if (inches > 0) inches.toString() else "") }

                    OutlinedTextField(
                        value = feetText,
                        onValueChange = {
                            feetText = it.filter(Char::isDigit)
                            val f = feetText.toIntOrNull() ?: 0
                            val i = inchesText.toIntOrNull() ?: 0
                            viewModel.updateHeight(fromFeetInches(f, i).toString())
                        },
                        label = { Text("Height (ft)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BurgundyPrimary, focusedLabelColor = BurgundyPrimary, cursorColor = BurgundyPrimary)
                    )
                    OutlinedTextField(
                        value = inchesText,
                        onValueChange = {
                            inchesText = it.filter(Char::isDigit)
                            val f = feetText.toIntOrNull() ?: 0
                            val i = inchesText.toIntOrNull() ?: 0
                            viewModel.updateHeight(fromFeetInches(f, i).toString())
                        },
                        label = { Text("Height (in)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BurgundyPrimary, focusedLabelColor = BurgundyPrimary, cursorColor = BurgundyPrimary)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Imperial Weight
                val weightKg = state.weight.toIntOrNull() ?: 0
                var weightLbsText by remember(weightKg) {
                    mutableStateOf(if (weightKg > 0) weightKg.toPounds().toString() else "")
                }
                
                OutlinedTextField(
                    value = weightLbsText, 
                    onValueChange = {
                        weightLbsText = it.filter(Char::isDigit)
                        val lbs = weightLbsText.toIntOrNull()
                        if (lbs != null) {
                            viewModel.updateWeight(fromPounds(lbs).toString())
                        } else {
                            viewModel.updateWeight("")
                        }
                    }, 
                    label = { Text("Weight (lbs)") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true, 
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BurgundyPrimary, focusedLabelColor = BurgundyPrimary, cursorColor = BurgundyPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = state.phone, onValueChange = viewModel::updatePhone, label = { Text("Phone #") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BurgundyPrimary, focusedLabelColor = BurgundyPrimary, cursorColor = BurgundyPrimary))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = state.medicalConditions, onValueChange = viewModel::updateMedicalConditions, label = { Text("Medical conditions") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BurgundyPrimary, focusedLabelColor = BurgundyPrimary, cursorColor = BurgundyPrimary))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = state.foodRestrictions, onValueChange = viewModel::updateFoodRestrictions, label = { Text("Food restrictions") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BurgundyPrimary, focusedLabelColor = BurgundyPrimary, cursorColor = BurgundyPrimary))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = state.mealsPerDay, onValueChange = viewModel::updateMealsPerDay, label = { Text("How many meals per day?") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BurgundyPrimary, focusedLabelColor = BurgundyPrimary, cursorColor = BurgundyPrimary))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = state.workoutFrequency, onValueChange = viewModel::updateWorkoutFrequency, label = { Text("How often do you workout?") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BurgundyPrimary, focusedLabelColor = BurgundyPrimary, cursorColor = BurgundyPrimary))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = state.overallGoal, onValueChange = viewModel::updateOverallGoal, label = { Text("What's your overall goal?") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BurgundyPrimary, focusedLabelColor = BurgundyPrimary, cursorColor = BurgundyPrimary))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("I am an admin", color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = state.isAdmin, onCheckedChange = viewModel::updateIsAdmin, colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = BurgundyPrimary))
                }
            }

            state.errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(msg, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.submit(isSignIn) { onAuthSuccess(it) } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary, contentColor = White)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp), color = White)
                } else {
                    Text(if (isSignIn) "Sign in" else "Create account")
                }
            }
            if (isSignIn && onNavigateToCreateAccount != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateToCreateAccount, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = BurgundyPrimary)) {
                    Text("Create account instead")
                }
            }
            if (!isSignIn && onNavigateToSignIn != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateToSignIn, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = BurgundyPrimary)) {
                    Text("Sign in instead")
                }
            }
        }
    }
}
