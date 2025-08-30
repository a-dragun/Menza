package com.example.menza.models

data class Review(
    val id: String = "",
    val foodId: String = "",
    val userId: String = "",
    val rating: Int = 0,
    val comment: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
