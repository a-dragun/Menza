package com.example.menza.models

import java.util.Locale

data class Food(
    val id: String = "",
    val name: String = "",
    val englishName: String = "",
    val germanName: String = "",
    val photoUrl: String? = null,
    val restaurantId: String = "",
    val allergens: List<Allergen> = emptyList(),
    val tags: List<FoodTag> = emptyList(),
    val status: FoodStatus = FoodStatus.UNAVAILABLE,
    val regularPrice: Double = 0.0,
    val studentPrice: Double = 0.0,
) {
    fun displayName(): String {
        return when (Locale.getDefault().language) {
            "hr" -> name
            "en" -> englishName
            "de" -> germanName
            else -> name
        }
    }
}
