package com.example.menza.models

data class Restaurant(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val imageUrl: String? = null,
    val staffIds: List<String> = emptyList(),
    val foodIds: List<String> = emptyList(),
    val lat: Double? = null,
    val lng: Double? = null
)