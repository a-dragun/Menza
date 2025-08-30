package com.example.menza.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.menza.viewmodels.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }

    LaunchedEffect(Unit) {
        val isLoggedIn = withContext(Dispatchers.IO) {
            viewModel.checkIfLoggedIn()
            viewModel.uiState.value.isLoggedIn
        }
        delay(1000)
        if (isLoggedIn) {
            navController.navigate("restaurantList") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }
}