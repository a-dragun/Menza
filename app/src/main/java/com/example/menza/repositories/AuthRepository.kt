package com.example.menza.repositories

import com.example.menza.models.Role
import com.example.menza.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private suspend fun isUsernameTaken(username: String): Boolean {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()
            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }


    suspend fun register(email: String, password: String, username: String): Result<Unit> {
        return try {
            if (isUsernameTaken(username)) {
                return Result.failure(Exception("Username is already taken"))
            }

            println("Starting Firebase auth registration")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            println("Auth registration complete, UID: ${result.user?.uid}")
            val uid = result.user?.uid ?: return Result.failure(Exception("No UID"))
            val user = User(uid = uid, email = email, username = username, role = Role.STUDENT, favorites = emptyList())
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

    suspend fun getUserByEmail(email: String): Result<User?> {
        return try {
            val snapshot = firestore.collection("users").whereEqualTo("email", email).get().await()
            val user = if (snapshot.documents.isNotEmpty()) {
                snapshot.documents[0].toObject(User::class.java)
            } else null
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsersByIds(userIds: List<String>): Result<List<User>> {
        return try {
            val usersCollection = firestore.collection("users")
            val users = mutableListOf<User>()
            for (uid in userIds) {
                val doc = usersCollection.document(uid).get().await()
                doc.toObject(User::class.java)?.let { users.add(it) }
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(uid: String): Result<User?> {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val user = doc.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRole(userId: String): Result<Role?> {
        return try {
            val docSnapshot = firestore.collection("users").document(userId).get().await()
            if (docSnapshot.exists()) {
                val user = docSnapshot.toObject(User::class.java)
                Result.success(user?.role)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: String, role: Role): Result<Unit> {
        return try {
            firestore.collection("users").document(userId).update("role", role).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFavorites(userId: String, favorites: List<String>): Result<Unit> {
        return try {
            firestore.collection("users").document(userId).update("favorites", favorites).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavoriteFromAllUsers(foodId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("users").whereArrayContains("favorites", foodId).get().await()
            for (doc in snapshot.documents) {
                val user = doc.toObject(User::class.java) ?: continue
                val newFavorites = user.favorites - foodId
                doc.reference.update("favorites", newFavorites).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun deleteAccount(uid: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No authenticated user"))
            if (user.uid != uid) return Result.failure(Exception("UID mismatch"))
            firestore.collection("users").document(uid).delete().await()
            user.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}