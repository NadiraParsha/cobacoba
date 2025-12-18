package com.example.smartfan.api

data class RegisterRequest(
    val username: String,
    val password: String,
    val password2: String
)