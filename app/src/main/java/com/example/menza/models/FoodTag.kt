package com.example.menza.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import android.content.Context
import com.example.menza.R

enum class FoodTag(private val stringResId: Int) {
    WITH_EGGS(R.string.tag_with_eggs),
    BREAKFAST(R.string.tag_breakfast),
    SEAFOOD(R.string.tag_seafood),
    VEGAN(R.string.tag_vegan),
    GLUTEN_FREE(R.string.tag_gluten_free),
    LACTOSE_FREE(R.string.tag_lactose_free),
    LOW_CALORIE(R.string.tag_low_calorie),
    CHICKEN(R.string.tag_chicken),
    PASTA(R.string.tag_pasta),
    SANDWICH(R.string.tag_sandwich),
    SOUP(R.string.tag_soup),
    SALAD(R.string.tag_salad),
    DESSERT(R.string.tag_dessert),
    SNACKS(R.string.tag_snacks),
    SPICY(R.string.tag_spicy),
    SWEET(R.string.tag_sweet),
    RAW_FOOD(R.string.tag_raw_food),
    FAST_FOOD(R.string.tag_fast_food),
    TRADITIONAL(R.string.tag_traditional),
    BBQ(R.string.tag_bbq),
    GRILLED(R.string.tag_grilled),
    FRIED(R.string.tag_fried),
    BAKED(R.string.tag_baked),
    STEAMED(R.string.tag_steamed),
    DAIRY(R.string.tag_dairy),
    NUT_FREE(R.string.tag_nut_free),
    LOW_SUGAR(R.string.tag_low_sugar);

    @Composable
    fun displayName(): String = stringResource(stringResId)

    fun getDisplayName(context: Context): String = context.getString(stringResId)

    override fun toString(): String = name
}