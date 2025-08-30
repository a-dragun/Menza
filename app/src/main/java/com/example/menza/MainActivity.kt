package com.example.menza

import com.example.menza.views.CreateFoodScreen
import com.example.menza.viewmodels.RestaurantViewModel
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.menza.ui.theme.MenzaTheme
import com.example.menza.viewmodels.AuthViewModel
import com.example.menza.views.AddRestaurantScreen
import com.example.menza.views.AdminDashboardScreen
import com.example.menza.views.FoodDetailScreen
import com.example.menza.views.FoodSearchScreen
import com.example.menza.views.LoginScreen
import com.example.menza.views.RateFoodScreen
import com.example.menza.views.RegisterScreen
import com.example.menza.views.RestaurantDetailScreen
import com.example.menza.views.RestaurantListScreen
import com.example.menza.views.SettingsScreen
import com.example.menza.views.SplashScreen
import com.example.menza.workers.FoodStatusWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    @SuppressLint("StateFlowValueCalledInComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        val workRequest = PeriodicWorkRequestBuilder<FoodStatusWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )

            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "food_status_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MenzaTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val restaurantViewModel: RestaurantViewModel = viewModel()

                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                    composable("splash") {
                        SplashScreen(
                            viewModel = authViewModel,
                            navController = navController
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            viewModel = authViewModel,
                            onSuccessfulRegistration = {
                                authViewModel.resetForm()
                                navController.navigate("restaurantList") {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            },
                            onNavigateToLogin = {
                                navController.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            viewModel = authViewModel,
                            onSuccessfulLogin = {
                                navController.navigate("restaurantList") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = {
                                navController.navigate("register") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("restaurantList") {
                        RestaurantListScreen(
                            viewModel = restaurantViewModel,
                            onRestaurantClick = { restaurantId ->
                                navController.navigate("restaurantFood/$restaurantId")
                            },
                            onAddRestaurantClick = { navController.navigate("addRestaurant") },
                            onSettingsClick = { navController.navigate("settings") },
                            onEditRestaurantsClick = { navController.navigate("adminDashboard") }
                        )
                    }
                    composable(
                        route = "restaurantFood/{restaurantId}",
                        arguments = listOf(navArgument("restaurantId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val restaurantId = backStackEntry.arguments?.getString("restaurantId")
                        if (restaurantId != null) {
                            FoodSearchScreen(
                                viewModel = restaurantViewModel,
                                restaurantId = restaurantId,
                                onAddFoodClick = { navController.navigate("createFood/$restaurantId") },
                                onFoodClick = { foodId -> navController.navigate("foodDetail/$foodId") },
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            Text("Invalid restaurant ID")
                        }
                    }
                    composable(
                        route = "createFood/{restaurantId}",
                        arguments = listOf(navArgument("restaurantId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val restaurantId = backStackEntry.arguments?.getString("restaurantId")
                        if (restaurantId != null) {
                            CreateFoodScreen(
                                restaurantViewModel = restaurantViewModel,
                                restaurantId = restaurantId,
                                onFoodAdded = { navController.popBackStack() },
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            Text("Invalid restaurant ID")
                        }
                    }
                    composable("adminDashboard") {
                        AdminDashboardScreen(
                            viewModel = restaurantViewModel,
                            onRestaurantClick = { restaurantId ->
                                navController.navigate("restaurantDetail/$restaurantId")
                            },
                            onSettingsClick = { navController.navigate("settings") },
                            onAddRestaurantClick = { navController.navigate("addRestaurant") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("addRestaurant") {
                        AddRestaurantScreen(
                            viewModel = restaurantViewModel,
                            onRestaurantAdded = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            authViewModel = authViewModel,
                            onBack = { navController.popBackStack() },
                            navController = navController
                        )
                    }
                    composable(
                        route = "restaurantDetail/{restaurantId}",
                        arguments = listOf(navArgument("restaurantId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val restaurantId = backStackEntry.arguments?.getString("restaurantId")
                        if (restaurantId != null) {
                            RestaurantDetailScreen(
                                viewModel = restaurantViewModel,
                                restaurantId = restaurantId,
                                onBack = { navController.popBackStack() },
                                onRestaurantDeleted = { navController.popBackStack() }
                            )
                        } else {
                            Text("Invalid restaurant ID")
                        }
                    }
                    composable(
                        route = "foodDetail/{foodId}",
                        arguments = listOf(navArgument("foodId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val foodId = backStackEntry.arguments?.getString("foodId")
                        if (foodId != null) {
                            FoodDetailScreen(
                                foodId = foodId,
                                viewModel = restaurantViewModel,
                                authViewModel = authViewModel,
                                onBack = { navController.popBackStack() },
                                onRateFoodClick = { fId, fName ->
                                    navController.navigate("rateFood/$fId/$fName")
                                }
                            )
                        } else {
                            Text("Invalid food ID")
                        }
                    }
                    composable(
                        route = "rateFood/{foodId}/{foodName}",
                        arguments = listOf(
                            navArgument("foodId") { type = NavType.StringType },
                            navArgument("foodName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val foodId = backStackEntry.arguments?.getString("foodId") ?: ""
                        val foodName = backStackEntry.arguments?.getString("foodName") ?: ""
                        val userId = authViewModel.repository.getCurrentUserId()
                        val restaurantName = restaurantViewModel.currentRestaurant.value?.name
                        val restaurantCity = restaurantViewModel.currentRestaurant.value?.city
                        if (userId != null && restaurantName != null && restaurantCity != null) {
                            RateFoodScreen(
                                foodId = foodId,
                                foodName = foodName,
                                userId = userId,
                                onBack = { navController.popBackStack() },
                                onReviewSubmitted = { navController.popBackStack() },
                                restaurantName = restaurantName,
                                restaurantCity = restaurantCity
                            )
                        } else {
                            Text("Missing required data")
                        }
                    }
                }
            }
        }
    }
}