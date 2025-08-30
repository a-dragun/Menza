package com.example.menza.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menza.models.User
import com.example.menza.repositories.AuthRepository
import com.example.menza.repositories.RestaurantRepository
import com.example.menza.states.AuthUiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(AuthUiState())
    val uiState: State<AuthUiState> = _uiState
    private val _searchedUser = MutableStateFlow<User?>(null)
    private val _searchUserLoading = MutableStateFlow(false)
    private val _searchUserError = MutableStateFlow<String?>(null)

    fun onEmailChange(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail)
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword)
    }

    fun onUsernameChange(newUsername: String) {
        _uiState.value = _uiState.value.copy(username = newUsername)
    }

    fun onRepeatPasswordChange(newRepeatPassword: String) {
        _uiState.value = _uiState.value.copy(repeatPassword = newRepeatPassword)
    }

    fun register() {
        viewModelScope.launch {
            try {
                if (_uiState.value.password != _uiState.value.repeatPassword) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Passwords do not match",
                        isLoading = false
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    isRegistered = false
                )

                val result = repository.register(
                    email = _uiState.value.email,
                    password = _uiState.value.password,
                    username = _uiState.value.username
                )

                if (result.isSuccess) {
                    val uid = repository.getCurrentUserId() ?: return@launch
                    val userResult = repository.getUserById(uid)
                    if (userResult.isSuccess) {
                        val user = userResult.getOrNull()
                        if (user != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRegistered = true,
                                isLoggedIn = true,
                                email = user.email,
                                username = user.username,
                                role = user.role.name
                            )
                            loadFavorites()
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to fetch user data"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = userResult.exceptionOrNull()?.message ?: "Failed to fetch user data"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.login(_uiState.value.email, _uiState.value.password)
            if (result.isSuccess) {
                val uid = repository.getCurrentUserId() ?: ""
                val userResult = repository.getUserById(uid)
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()
                    if (user != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            email = user.email,
                            username = user.username,
                            role = user.role.name
                        )
                        loadFavorites()
                    } else {
                        FirebaseAuth.getInstance().signOut()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            errorMessage = "User does not exist"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = userResult.exceptionOrNull()?.message ?: "Failed to fetch user data"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun clearSearch() {
        _searchedUser.value = null
        _searchUserError.value = null
        _searchUserLoading.value = false
    }

    fun resetForm() {
        _uiState.value = AuthUiState()
        clearSearch()
    }

    private val restaurantRepository = RestaurantRepository()

    data class FavoriteItem(
        val foodId: String,
        val foodName: String,
        val restaurantName: String,
        val city: String,
        val photoUrl: String?
    )

    private val _favorites = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val favorites: StateFlow<List<FavoriteItem>> = _favorites
    private val _favoritesLoading = MutableStateFlow(false)
    val favoritesLoading: StateFlow<Boolean> = _favoritesLoading

    fun loadFavorites() {
        val uid = repository.getCurrentUserId() ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = "User not logged in")
            return
        }
        viewModelScope.launch {
            _favoritesLoading.value = true
            try {
                val userResult = repository.getUserById(uid)
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull() ?: run {
                        _uiState.value = _uiState.value.copy(errorMessage = "User not found")
                        _favoritesLoading.value = false
                        return@launch
                    }
                    val favItems = mutableListOf<FavoriteItem>()
                    for (foodId in user.favorites) {
                        val foodResult = restaurantRepository.getFoodById(foodId)
                        if (foodResult.isSuccess) {
                            val food = foodResult.getOrNull() ?: continue
                            val restaurantResult = restaurantRepository.getRestaurantById(food.restaurantId)
                            if (restaurantResult.isSuccess) {
                                val restaurant = restaurantResult.getOrNull() ?: continue
                                favItems.add(
                                    FavoriteItem(
                                        foodId = foodId,
                                        foodName = food.displayName(),
                                        restaurantName = restaurant.name,
                                        city = restaurant.city,
                                        photoUrl = food.photoUrl
                                    )
                                )
                            }
                        }
                    }
                    _favorites.value = favItems
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = userResult.exceptionOrNull()?.message ?: "Failed to load favorites")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error loading favorites")
            } finally {
                _favoritesLoading.value = false
            }
        }
    }

    fun removeFavorite(foodId: String) {
        val uid = repository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val userResult = repository.getUserById(uid)
            if (userResult.isSuccess) {
                val user = userResult.getOrNull() ?: return@launch
                val newFavorites = user.favorites - foodId
                val updateResult = repository.updateFavorites(uid, newFavorites)
                if (updateResult.isSuccess) {
                    loadFavorites()
                }
            }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = _uiState.value.copy(isLoggedIn = false)
    }

    fun deleteAccount() {
        val uid = repository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.deleteAccount(uid)
            if (result.isSuccess) {
                _uiState.value = AuthUiState()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete account"
                )
            }
        }
    }

    fun checkIfLoggedIn() {
        val uid = repository.getCurrentUserId()
        if (uid != null) {
            viewModelScope.launch {
                val userResult = repository.getUserById(uid)
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()
                    if (user != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoggedIn = true,
                            email = user.email,
                            username = user.username,
                            role = user.role.name
                        )
                        loadFavorites()
                    } else {
                        FirebaseAuth.getInstance().signOut()
                        _uiState.value = _uiState.value.copy(isLoggedIn = false)
                    }
                } else {
                    FirebaseAuth.getInstance().signOut()
                    _uiState.value = _uiState.value.copy(isLoggedIn = false)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoggedIn = false)
        }
    }
}