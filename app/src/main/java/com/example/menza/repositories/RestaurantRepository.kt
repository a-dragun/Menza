package com.example.menza.repositories

import android.util.Base64
import com.example.menza.models.Food
import com.example.menza.models.FoodStatus
import com.example.menza.models.Restaurant
import com.example.menza.models.Allergen
import com.example.menza.models.FoodTag
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class RestaurantRepository(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val restaurantsCollection = firestore.collection("restaurants")
    private val foodsCollection = firestore.collection("foods")

    suspend fun createRestaurant(
        restaurant: Restaurant,
        photoData: ByteArray? = null
    ): Result<Unit> {
        return try {
            val docRef = restaurantsCollection.document()
            val restaurantWithId = restaurant.copy(id = docRef.id)

            val restaurantMap = mutableMapOf(
                "id" to restaurantWithId.id,
                "name" to restaurantWithId.name,
                "address" to restaurantWithId.address,
                "city" to restaurantWithId.city,
                "staffIds" to restaurantWithId.staffIds,
                "foodIds" to restaurantWithId.foodIds,
                "lat" to restaurantWithId.lat,
                "lng" to restaurantWithId.lng
            )
            photoData?.let { bytes ->
                restaurantMap["photoBlob"] = Blob.fromBytes(bytes)
            }

            docRef.set(restaurantMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllRestaurants(): Result<List<Restaurant>> {
        return try {
            val snapshot = restaurantsCollection.get().await()
            val restaurants = snapshot.documents.mapNotNull { doc ->
                val r = doc.toObject(Restaurant::class.java)
                val photoBlob = doc.get("photoBlob") as? Blob
                r?.copy(
                    imageUrl = photoBlob?.let { Base64.encodeToString(it.toBytes(), Base64.DEFAULT) }
                )
            }
            Result.success(restaurants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getRestaurantById(id: String): Result<Restaurant> {
        return try {
            val docSnapshot = restaurantsCollection.document(id).get().await()
            if (docSnapshot.exists()) {
                val restaurant = docSnapshot.toObject(Restaurant::class.java)
                if (restaurant != null) {
                    Result.success(restaurant.copy(id = docSnapshot.id))
                } else {
                    Result.failure(Exception("Failed to parse restaurant data"))
                }
            } else {
                Result.failure(Exception("Restaurant not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateRestaurant(restaurant: Restaurant): Result<Unit> {
        return try {
            restaurantsCollection.document(restaurant.id).update(
                mapOf(
                    "name" to restaurant.name,
                    "address" to restaurant.address,
                    "city" to restaurant.city,
                    "imageUrl" to restaurant.imageUrl,
                    "staffIds" to restaurant.staffIds,
                    "foodIds" to restaurant.foodIds
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRestaurant(restaurantId: String): Result<Unit> {
        return try {
            val restaurantResult = getRestaurantById(restaurantId)
            if (restaurantResult.isSuccess) {
                val restaurant = restaurantResult.getOrNull()
                restaurant?.foodIds?.forEach { foodId ->
                    foodsCollection.document(foodId).delete().await()
                }
            }
            restaurantsCollection.document(restaurantId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStaffMember(restaurant: Restaurant, userId: String): Result<Unit> {
        val updatedStaff = restaurant.staffIds.toMutableList()
        if (!updatedStaff.contains(userId)) {
            updatedStaff.add(userId)
            val updatedRestaurant = restaurant.copy(staffIds = updatedStaff)
            return updateRestaurant(updatedRestaurant)
        }
        return Result.success(Unit)
    }

    suspend fun removeStaffMember(restaurant: Restaurant, userId: String): Result<Unit> {
        val updatedStaff = restaurant.staffIds.toMutableList()
        if (updatedStaff.remove(userId)) {
            val updatedRestaurant = restaurant.copy(staffIds = updatedStaff)
            return updateRestaurant(updatedRestaurant)
        }
        return Result.success(Unit)
    }

    suspend fun createFood(
        food: Food,
        restaurant: Restaurant,
        userId: String,
        photoData: ByteArray?
    ): Result<Unit> {
        return try {
            if (!restaurant.staffIds.contains(userId)) {
                return Result.failure(Exception("User is not a staff member of this restaurant"))
            }
            val englishName = translateText(food.name, "en")
            val germanName = translateText(food.name, "de")

            val docRef = foodsCollection.document()
            val foodWithTranslations = food.copy(
                id = docRef.id,
                restaurantId = restaurant.id,
                englishName = englishName,
                germanName = germanName,
                photoUrl = null
            )

            val photoBlob = photoData?.let { Blob.fromBytes(it) }

            val foodMap = foodWithTranslations.toMap().toMutableMap().apply {
                this["allergens"] = foodWithTranslations.allergens.map { it.name }
                this["tags"] = foodWithTranslations.tags.map { it.name }
                if (photoBlob != null) this["photoBlob"] = photoBlob
            }

            docRef.set(foodMap).await()

            val updatedFoodIds = restaurant.foodIds.toMutableList().apply { add(docRef.id) }
            updateRestaurant(restaurant.copy(foodIds = updatedFoodIds)).getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getFoodsByRestaurantId(restaurantId: String): Result<List<Food>> {
        return try {
            val snapshot = foodsCollection.whereEqualTo("restaurantId", restaurantId).get().await()
            val foods = snapshot.documents.mapNotNull { doc ->
                val food = doc.toObject(Food::class.java)?.copy(
                    allergens = (doc.get("allergens") as? List<*>)?.mapNotNull { allergenName ->
                        Allergen.entries.find { it.name == allergenName }
                    } ?: emptyList(),
                    tags = (doc.get("tags") as? List<*>)?.mapNotNull { tagName ->
                        FoodTag.entries.find { it.name == tagName }
                    } ?: emptyList()
                )
                val photoBlob = doc.get("photoBlob") as? Blob
                if (photoBlob != null && food != null) {
                    val bytes = photoBlob.toBytes()
                    food.copy(photoUrl = Base64.encodeToString(bytes, Base64.DEFAULT))
                } else {
                    food
                }
            }
            Result.success(foods)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun updateFoodStatus(foodId: String, newStatus: FoodStatus): Result<Unit> {
        return try {
            val foodRef = foodsCollection.document(foodId)
            foodRef.update("status", newStatus.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFood(foodId: String): Result<Unit> {
        return try {
            val foodDoc = foodsCollection.document(foodId).get().await()
            val food = foodDoc.toObject(Food::class.java) ?: return Result.failure(Exception("Food not found"))

            food.photoUrl?.let { url ->
                storage.getReferenceFromUrl(url).delete().await()
            }

            foodDoc.reference.delete().await()

            val restaurantResult = getRestaurantById(food.restaurantId)
            if (restaurantResult.isSuccess) {
                val restaurant = restaurantResult.getOrNull()!!
                val newFoodIds = restaurant.foodIds - foodId
                updateRestaurant(restaurant.copy(foodIds = newFoodIds)).getOrThrow()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFoodById(id: String): Result<Food?> {
        return try {
            val doc = foodsCollection.document(id).get().await()
            if (doc.exists()) {
                val food = doc.toObject(Food::class.java)?.copy(
                    allergens = (doc.get("allergens") as? List<*>)?.mapNotNull { name ->
                        Allergen.entries.find { it.name == name }
                    } ?: emptyList(),
                    tags = (doc.get("tags") as? List<*>)?.mapNotNull { name ->
                        FoodTag.entries.find { it.name == name }
                    } ?: emptyList()
                )
                val photoBlob = doc.get("photoBlob") as? Blob
                val photoUrl = photoBlob?.let { Base64.encodeToString(it.toBytes(), Base64.DEFAULT) }
                Result.success(food?.copy(photoUrl = photoUrl))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun translateText(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val url = "https://api.mymemory.translated.net/get?q=$text&langpair=hr|$targetLang"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (!response.isSuccessful || responseBody == null) return@withContext text

        val jsonResponse = JSONObject(responseBody)
        jsonResponse.getJSONObject("responseData").getString("translatedText")
    }
}
fun Food.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "name" to name,
        "englishName" to englishName,
        "germanName" to germanName,
        "restaurantId" to restaurantId,
        "allergens" to allergens,
        "tags" to tags.map { it.name },
        "status" to status.name,
        "regularPrice" to regularPrice,
        "studentPrice" to studentPrice
    )
}

