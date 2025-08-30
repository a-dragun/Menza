package com.example.menza.viewmodels

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menza.models.Food
import com.example.menza.models.FoodStatus
import com.example.menza.models.FoodTag
import com.example.menza.models.Allergen
import com.example.menza.models.Restaurant
import com.example.menza.models.Role
import com.example.menza.models.User
import com.example.menza.repositories.AuthRepository
import com.example.menza.repositories.GeocodingRepository
import com.example.menza.repositories.GeocodingResult
import com.example.menza.repositories.RestaurantRepository
import com.example.menza.repositories.ReviewRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class RestaurantViewModel(
    private val restaurantRepository: RestaurantRepository = RestaurantRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val reviewRepository: ReviewRepository = ReviewRepository()
) : ViewModel() {

    private val _restaurants = MutableStateFlow<List<Restaurant>>(emptyList())
    val restaurants: StateFlow<List<Restaurant>> = _restaurants

    private val _currentRestaurant = MutableStateFlow<Restaurant?>(null)
    val currentRestaurant: StateFlow<Restaurant?> = _currentRestaurant

    private val _staffUsers = MutableStateFlow<List<User>>(emptyList())
    val staffUsers: StateFlow<List<User>> = _staffUsers

    private val _foods = MutableStateFlow<List<Food>>(emptyList())
    val foods: StateFlow<List<Food>> = _foods

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _searchedUser = MutableStateFlow<User?>(null)
    val searchedUser: StateFlow<User?> = _searchedUser

    private val _events = Channel<Event>()
    val events = _events.receiveAsFlow()

    sealed class Event {
        data object RestaurantCreated : Event()
        data object RestaurantDeleted : Event()
        data object StaffAdded : Event()
        data object StaffRemoved : Event()
        data object FoodCreated : Event()
    }

    private val geocodingRepository = GeocodingRepository()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults

    fun searchAddress(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val results = geocodingRepository.searchAddress(query)
            _searchResults.value = results
            _isLoading.value = false
        }
    }

    fun createRestaurantFromGeocoding(
        name: String,
        result: GeocodingResult,
        photoData: ByteArray? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val restaurant = Restaurant(
                id = "",
                name = name,
                city = result.city,
                address = result.address,
                lat = result.lat,
                lng = result.lon
            )
            val r = restaurantRepository.createRestaurant(restaurant, photoData)
            if (r.isSuccess) {
                _events.send(Event.RestaurantCreated)
                loadAllRestaurants()
            } else {
                _error.value = r.exceptionOrNull()?.localizedMessage ?: "Failed to create restaurant"
            }
            _isLoading.value = false
        }
    }

    fun loadAllRestaurants() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = restaurantRepository.getAllRestaurants()
            if (result.isSuccess) {
                _restaurants.value = result.getOrNull() ?: emptyList()
            } else {
                _error.value = result.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    fun loadRestaurantById(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = restaurantRepository.getRestaurantById(id)
            if (result.isSuccess) {
                _currentRestaurant.value = result.getOrNull()
                loadStaffForCurrentRestaurant()
                loadFoodsForCurrentRestaurant()
            } else {
                _error.value = result.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    private fun loadStaffForCurrentRestaurant() {
        _currentRestaurant.value?.let { restaurant ->
            viewModelScope.launch {
                _isLoading.value = true
                val result = authRepository.getUsersByIds(restaurant.staffIds)
                if (result.isSuccess) {
                    _staffUsers.value = result.getOrNull() ?: emptyList()
                } else {
                    println("Error fetching staff users: ${result.exceptionOrNull()?.message}")
                    _staffUsers.value = emptyList()
                }
                _isLoading.value = false
            }
        }
    }

    private fun loadFoodsForCurrentRestaurant() {
        _currentRestaurant.value?.let { restaurant ->
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                val result = restaurantRepository.getFoodsByRestaurantId(restaurant.id)
                if (result.isSuccess) {
                    _foods.value = result.getOrNull() ?: emptyList()
                } else {
                    _error.value = result.exceptionOrNull()?.localizedMessage
                }
                _isLoading.value = false
            }
        }
    }

    fun deleteRestaurant(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val restaurantResult = restaurantRepository.getRestaurantById(id)
            if (restaurantResult.isSuccess) {
                val restaurant = restaurantResult.getOrNull()
                if (restaurant != null) {
                    restaurant.staffIds.forEach { userId ->
                        val roleUpdateResult = authRepository.updateUserRole(userId, Role.STUDENT)
                        if (!roleUpdateResult.isSuccess) {
                            _error.value = roleUpdateResult.exceptionOrNull()?.localizedMessage
                            _isLoading.value = false
                            return@launch
                        }
                    }
                    restaurant.foodIds.forEach { foodId ->
                        deleteFood(foodId)
                    }

                    val deleteResult = restaurantRepository.deleteRestaurant(id)
                    if (deleteResult.isSuccess) {
                        _events.send(Event.RestaurantDeleted)
                        loadAllRestaurants()
                    } else {
                        _error.value = deleteResult.exceptionOrNull()?.localizedMessage
                    }
                } else {
                    _error.value = "Restaurant not found"
                }
            } else {
                _error.value = restaurantResult.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    fun addStaffMember(userId: String) {
        _currentRestaurant.value?.let { restaurant ->
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                val roleResult = authRepository.getUserRole(userId)
                if (roleResult.isSuccess) {
                    val role = roleResult.getOrNull()
                    if (role == Role.STAFF || role == Role.ADMIN) {
                        _error.value = "User is already a staff member or admin in another restaurant"
                        _isLoading.value = false
                        return@launch
                    }
                } else {
                    _error.value = roleResult.exceptionOrNull()?.localizedMessage
                    _isLoading.value = false
                    return@launch
                }

                val result = restaurantRepository.addStaffMember(restaurant, userId)
                if (result.isSuccess) {
                    val roleUpdateResult = authRepository.updateUserRole(userId, Role.STAFF)
                    if (roleUpdateResult.isSuccess) {
                        _events.send(Event.StaffAdded)
                        loadRestaurantById(restaurant.id)
                    } else {
                        _error.value = roleUpdateResult.exceptionOrNull()?.localizedMessage
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.localizedMessage
                }
                _isLoading.value = false
            }
        }
    }

    fun removeStaffMember(userId: String) {
        _currentRestaurant.value?.let { restaurant ->
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                val result = restaurantRepository.removeStaffMember(restaurant, userId)
                if (result.isSuccess) {
                    val roleUpdateResult = authRepository.updateUserRole(userId, Role.STUDENT)
                    if (roleUpdateResult.isSuccess) {
                        _events.send(Event.StaffRemoved)
                        loadRestaurantById(restaurant.id)
                    } else {
                        _error.value = roleUpdateResult.exceptionOrNull()?.localizedMessage
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.localizedMessage
                }
                _isLoading.value = false
            }
        }
    }

    fun createFood(
        name: String,
        englishName: String,
        germanName: String,
        allergens: List<Allergen>,
        tags: List<FoodTag>,
        regularPrice: Double,
        studentPrice: Double,
        photoData: ByteArray?,
        userId: String
    ) {
        _currentRestaurant.value?.let { restaurant ->
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                val food = Food(
                    name = name,
                    englishName = englishName,
                    germanName = germanName,
                    allergens = allergens,
                    tags = tags,
                    status = FoodStatus.UNAVAILABLE,
                    regularPrice = regularPrice,
                    studentPrice = studentPrice
                )
                val result = restaurantRepository.createFood(food, restaurant, userId, photoData)
                if (result.isSuccess) {
                    _events.send(Event.FoodCreated)
                    loadFoodsForCurrentRestaurant()
                } else {
                    _error.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to create food"
                }
                _isLoading.value = false
            }
        }
    }

    suspend fun translateText(text: String, targetLang: String): String {
        return restaurantRepository.translateText(text, targetLang)
    }

    fun searchUserByEmail(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _searchedUser.value = null
            val result = authRepository.getUserByEmail(email)
            if (result.isSuccess) {
                _searchedUser.value = result.getOrNull()
                if (_searchedUser.value == null) {
                    _error.value = "User not found"
                }
            } else {
                _error.value = result.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    fun clearSearch() {
        _searchedUser.value = null
    }

    fun updateFoodStatus(foodId: String, newStatus: FoodStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = restaurantRepository.updateFoodStatus(foodId, newStatus)
            if (result.isSuccess) {
                loadFoodsForCurrentRestaurant()
            } else {
                _error.value = result.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    fun deleteFood(foodId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val reviewsResult = reviewRepository.deleteReviewsForFood(foodId)
            if (!reviewsResult.isSuccess) {
                _error.value = reviewsResult.exceptionOrNull()?.localizedMessage
                _isLoading.value = false
                return@launch
            }

            val favoritesResult = authRepository.removeFavoriteFromAllUsers(foodId)
            if (!favoritesResult.isSuccess) {
                _error.value = favoritesResult.exceptionOrNull()?.localizedMessage
                _isLoading.value = false
                return@launch
            }

            val deleteResult = restaurantRepository.deleteFood(foodId)
            if (deleteResult.isSuccess) {
                loadFoodsForCurrentRestaurant()
            } else {
                _error.value = deleteResult.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    @SuppressLint("DefaultLocale")
    fun getDistanceToRestaurant(restaurant: Restaurant, userLocation: Pair<Double, Double>?): String? {
        if (userLocation == null || restaurant.lat == null || restaurant.lng == null) {
            return null
        }
        val (userLat, userLon) = userLocation
        val results = FloatArray(1)
        android.location.Location.distanceBetween(userLat, userLon, restaurant.lat, restaurant.lng, results)
        val distanceMeters = results[0]
        return if (distanceMeters < 1000) {
            "${distanceMeters.toInt()} m"
        } else {
            String.format("%.1f km", distanceMeters / 1000f)
        }
    }

    fun clearRestaurantData() {
        _currentRestaurant.value = null
        _foods.value = emptyList()
        _staffUsers.value = emptyList()
        _error.value = null
        _isLoading.value = false
    }
}