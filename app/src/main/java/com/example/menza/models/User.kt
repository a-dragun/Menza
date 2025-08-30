package com.example.menza.models

enum class Role {
    ADMIN,
    STUDENT,
    STAFF
}

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val role: Role = Role.STUDENT,
    val favorites: List<String> = emptyList(),
)