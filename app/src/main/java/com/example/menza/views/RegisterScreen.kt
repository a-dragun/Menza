package com.example.menza.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menza.ui.theme.backgroundColor
import com.example.menza.ui.theme.primaryColor
import com.example.menza.ui.theme.primaryTextColor
import com.example.menza.ui.theme.secondaryColor
import com.example.menza.ui.theme.secondaryTextColor
import com.example.menza.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onSuccessfulRegistration: () -> Unit
) {
    val state = viewModel.uiState.value

    Box(
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Register",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
            )

            Spacer(Modifier.height(60.dp))

            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = secondaryColor
                )
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = secondaryColor
                )
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = secondaryColor
                )
            )

            Spacer(Modifier.height(30.dp))

            SubmitButton(
                onClick = {
                    println("Register button clicked")
                    viewModel.register()
                },
                text = "Register",
                enabled = !state.isLoading
            )

            Spacer(Modifier.height(10.dp))

            if (state.isLoading) {
                println("Showing CircularProgressIndicator")
                CircularProgressIndicator(
                    color = primaryColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            if (state.isRegistered && !state.isLoading) {
                println("Showing success message, isRegistered = ${state.isRegistered}")
                Text(
                    text = "User successfully registered!",
                    color = Color.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LaunchedEffect(key1 = state.isRegistered) {
                    println("LaunchedEffect triggered, preparing to navigate")
                    delay(2000)
                    println("Delay complete, resetting form and navigating")
                    viewModel.resetForm()
                    onSuccessfulRegistration()
                }
            }

            state.errorMessage?.let { error ->
                println("Showing error message: $error")
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SubmitButton(
    onClick: () -> Unit,
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth(0.8f)
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = primaryColor,
            disabledContainerColor = secondaryColor
        )
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else secondaryTextColor,
            fontWeight = FontWeight.Bold
        )
    }
}