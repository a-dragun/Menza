package com.example.menza.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menza.models.Review
import com.example.menza.repositories.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val repository: ReviewRepository = ReviewRepository()
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    fun submitReview(foodId: String, userId: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            val review = Review(
                foodId = foodId,
                userId = userId,
                rating = rating,
                comment = comment?.takeIf { it.isNotBlank() }
            )

            val result = repository.addReview(foodId, review)
            if (result.isSuccess) {
                loadReviews(foodId)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage
            }
            _isSubmitting.value = false
        }
    }

    fun loadReviews(foodId: String) {
        viewModelScope.launch {
            val result = repository.getReviewsForFood(foodId)
            if (result.isSuccess) {
                _reviews.value = result.getOrNull() ?: emptyList()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun deleteReview(foodId: String, reviewId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            val result = repository.deleteReview(foodId, reviewId)
            if (result.isSuccess) {
                loadReviews(foodId)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage
            }
            _isSubmitting.value = false
        }
    }
}
