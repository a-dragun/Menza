package com.example.menza.repositories

import com.example.menza.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun register(email: String, password: String, username: String): Result<Unit> {
        return try {
            println("Starting Firebase auth registration")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            println("Auth registration complete, UID: ${result.user?.uid}")
            val uid = result.user?.uid ?: return Result.failure(Exception("No UID"))
            val user = User(uid = uid, email = email, username = username, role = "student")
            println("Saving user to Firestore: $user")
            firestore.collection("users").document(uid).set(user).await()
            println("User saved to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            println("Registration error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}