package com.example.menza.models

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.menza.R

enum class FoodStatus(private val stringResId: Int) {
    SERVING(R.string.status_serving),
    UNAVAILABLE(R.string.status_unavailable),
    PREPARING(R.string.status_preparing);

    @Composable
    fun displayName(): String = stringResource(stringResId)

    fun getDisplayName(context: Context): String = context.getString(stringResId)

    override fun toString(): String = name
}