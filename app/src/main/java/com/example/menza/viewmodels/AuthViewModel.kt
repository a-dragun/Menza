package com.example.menza.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import com.example.menza.repositories.AuthRepository
import com.example.menza.states.AuthUiState

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(AuthUiState())
    val uiState: State<AuthUiState> = _uiState

    fun onEmailChange(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail)
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword)
    }

    fun onUsernameChange(newUsername: String) {
        _uiState.value = _uiState.value.copy(username = newUsername)
    }

    fun register() {
        viewModelScope.launch {
            try {
                println("Starting registration")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    isRegistered = false
                )
                println("Calling repository.register")
                val result = repository.register(
                    email = _uiState.value.email,
                    password = _uiState.value.password,
                    username = _uiState.value.username
                )
                println("Repository result: isSuccess = ${result.isSuccess}")
                if (result.isSuccess) {
                    println("Registration successful, updating UI state")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRegistered = true,
                        errorMessage = null
                    )
                } else {
                    println("Registration failed: ${result.exceptionOrNull()?.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Registration failed",
                        isRegistered = false
                    )
                }
            } catch (e: Exception) {
                println("Exception in register: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Unknown error",
                    isRegistered = false
                )
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val result = repository.login(_uiState.value.email, _uiState.value.password)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Login failed"
                )
            }
        }
    }

    fun resetForm() {
        println("Resetting form")
        _uiState.value = AuthUiState()
    }
}