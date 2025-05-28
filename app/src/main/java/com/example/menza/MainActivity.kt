package com.example.menza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.menza.ui.theme.MenzaTheme
import com.example.menza.viewmodels.AuthViewModel
import com.example.menza.views.ChooseFavoriteFoodScreen
import com.example.menza.views.RegisterScreen
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MenzaTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()

                NavHost(
                    navController = navController,
                    startDestination = "register"
                ) {
                    composable("register") {
                        RegisterScreen(
                            viewModel = authViewModel,
                            onSuccessfulRegistration = {
                                println("Navigating to chooseFavoriteFood")
                                navController.navigate("chooseFavoriteFood") {
                                    popUpTo("register") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("chooseFavoriteFood") {
                        println("Rendering ChooseFavoriteFoodScreen")
                        ChooseFavoriteFoodScreen()
                    }
                }
            }
        }
    }
}