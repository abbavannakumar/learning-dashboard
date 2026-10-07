package com.example.learning.data.repository

import kotlinx.coroutines.delay

class AuthRepository {
    suspend fun login(email: String, password: String) {
        delay(650)
        if (!email.equals("demo@example.com", ignoreCase = true) || password != "password123") {
            throw IllegalArgumentException("Incorrect email or password. Use the demo credentials below.")
        }
    }
}
