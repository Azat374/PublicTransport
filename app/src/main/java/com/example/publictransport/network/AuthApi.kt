package com.example.publictransport.network

import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val expiresIn: Long)

interface AuthApi {
    /** Ваш эндпоинт логина, который отдаёт новый X-Auth-Token */
    @POST("auth/login")
    suspend fun login(@Body creds: LoginRequest): LoginResponse
}
