package com.example.menza.models

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.menza.R

enum class Allergen(private val stringResId: Int) {
    GLUTEN(R.string.allergen_gluten),
    CRUSTACEANS(R.string.allergen_crustaceans),
    EGGS(R.string.allergen_eggs),
    FISH(R.string.allergen_fish),
    PEANUTS(R.string.allergen_peanuts),
    SOYBEANS(R.string.allergen_soybeans),
    MILK(R.string.allergen_milk),
    NUTS(R.string.allergen_nuts),
    CELERY(R.string.allergen_celery),
    MUSTARD(R.string.allergen_mustard),
    SESAME(R.string.allergen_sesame_seeds);

    @Composable
    fun displayName(): String = stringResource(stringResId)

    fun getDisplayName(context: Context): String = context.getString(stringResId)

    override fun toString(): String = name
}