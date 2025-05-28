package com.example.menza.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menza.ui.theme.backgroundColor
import com.example.menza.ui.theme.primaryColor
import com.example.menza.ui.theme.primaryTextColor
import com.example.menza.ui.theme.secondaryColor
import com.example.menza.ui.theme.secondaryTextColor
import com.example.menza.viewmodels.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel



@Preview(showBackground = true)
@Composable
fun LoginScreen(viewModel: AuthViewModel = viewModel()) {
    val state = viewModel.uiState.value

    Box(
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Login", fontSize = 48.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(60.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(Modifier.height(30.dp))

            SubmitButton(onClick = { viewModel.login() }, text = "Login")

            if (state.isLoading) {
                Spacer(Modifier.height(10.dp))
                CircularProgressIndicator()
            }

            state.errorMessage?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color.Red)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Forgot password?")
        }
    }
}
