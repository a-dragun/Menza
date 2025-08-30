package com.example.menza.repositories

import com.example.menza.models.Review
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ReviewRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun addReview(foodId: String, review: Review): Result<Unit> {
        return try {
            val reviewRef = db.collection("foods")
                .document(foodId)
                .collection("reviews")
                .document()

            val reviewWithId = review.copy(id = reviewRef.id)
            reviewRef.set(reviewWithId).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReviewsForFood(foodId: String): Result<List<Review>> {
        return try {
            val snapshot = db.collection("foods")
                .document(foodId)
                .collection("reviews")
                .get()
                .await()

            val reviews = snapshot.toObjects(Review::class.java)
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReviewsForFood(foodId: String): Result<Unit> {
        return try {
            val snapshot = db.collection("foods").document(foodId).collection("reviews").get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReview(foodId: String, reviewId: String): Result<Unit> {
        return try {
            db.collection("foods")
                .document(foodId)
                .collection("reviews")
                .document(reviewId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
