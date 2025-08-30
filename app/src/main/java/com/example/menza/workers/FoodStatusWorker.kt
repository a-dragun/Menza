package com.example.menza.workers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.menza.R
import com.example.menza.R.drawable.ic_stat_name
import com.example.menza.models.Food
import com.example.menza.models.FoodStatus
import com.example.menza.models.User
import com.example.menza.repositories.RestaurantRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FoodStatusWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.success()
            val userSnapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            val user =
                userSnapshot.toObject(User::class.java) ?: return@withContext Result.success()

            val favoriteFoodIds = user.favorites
            if (favoriteFoodIds.isEmpty()) {
                return@withContext Result.success()
            }

            favoriteFoodIds.chunked(10).forEach { chunk ->
                val foodsSnapshot = firestore.collection("foods")
                    .whereIn("id", chunk)
                    .get()
                    .await()
                val foods = foodsSnapshot.documents.mapNotNull { it.toObject(Food::class.java) }

                foods.forEach { food ->
                    val lastStatus = getLastKnownStatus(userId, food.id)
                    val restaurantRepository = RestaurantRepository()
                    var restaurantName = restaurantRepository.getRestaurantById(food.restaurantId).getOrNull()?.name
                    if(restaurantName == null) {
                        restaurantName = ""
                    }
                    if (food.status != lastStatus && food.status != FoodStatus.UNAVAILABLE) {
                        saveLastKnownStatus(userId, food.id, food.status)
                        sendNotification(food, restaurantName)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            println("FoodStatusWorker error: ${e.message}")
            Result.retry()
        }
    }

    private fun getLastKnownStatus(userId: String, foodId: String): FoodStatus? {
        val prefs = applicationContext.getSharedPreferences(
            "food_status_prefs_$userId",
            Context.MODE_PRIVATE
        )
        val statusString = prefs.getString(foodId, null) ?: return null
        return try {
            FoodStatus.valueOf(statusString)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    @SuppressLint("UseKtx")
    private fun saveLastKnownStatus(userId: String, foodId: String, status: FoodStatus) {
        val prefs = applicationContext.getSharedPreferences(
            "food_status_prefs_$userId",
            Context.MODE_PRIVATE
        )
        prefs.edit().putString(foodId, status.name).apply()
    }

    private fun sendNotification(food: Food, restaurantName: String) {
        val channelId = "food_status_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                applicationContext.getString(R.string.food_status_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    applicationContext.getString(R.string.food_status_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

        try {
            val statusText = when (food.status) {
                FoodStatus.PREPARING -> applicationContext.getString(R.string.status_preparing)
                FoodStatus.SERVING -> applicationContext.getString(R.string.status_serving)
                else -> return
            }

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(ic_stat_name)
                .setContentTitle(applicationContext.getString(R.string.notification_title))
                .setContentText(
                    applicationContext.getString(
                        R.string.notification_content,
                        food.displayName(),
                        restaurantName,
                        statusText
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(
                food.id.hashCode(),
                notification
            )
            println("Notification sent for food ${food.id}")
        } catch (e: IllegalArgumentException) {
            println("Failed to send notification: ${e.message}")
        }
    }
}