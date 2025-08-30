package com.example.menza.states

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val role: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistered: Boolean = false,
    val isLoggedIn: Boolean = false,
    val repeatPassword: String = ""
)

