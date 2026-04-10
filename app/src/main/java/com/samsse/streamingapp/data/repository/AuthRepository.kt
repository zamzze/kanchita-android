package com.samsse.streamingapp.data.repository

import com.samsse.streamingapp.data.remote.ApiService
import com.samsse.streamingapp.data.remote.LoginRequest
import com.samsse.streamingapp.data.remote.RegisterRequest
import com.samsse.streamingapp.data.remote.TokenManager

class AuthRepository(
    private val api: ApiService,
    private val tokenManager: TokenManager
) {

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()
                val loginData = body?.data
                if (loginData != null) {
                    tokenManager.saveTokens(loginData.accessToken, loginData.refreshToken)
                    val saved = tokenManager.getAccessToken()
                    android.util.Log.d("AuthRepo", "Token saved: $saved")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Respuesta inválida del servidor"))
                }
            } else {
                Result.failure(Exception("Credenciales incorrectas"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepo", "Exception: ${e.message}", e)
            Result.failure(Exception("Error de conexión"))
        }
    }

    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            val response = api.register(RegisterRequest(email, password))
            if (response.isSuccessful) {
                val loginData = response.body()?.data
                if (loginData != null) {
                    tokenManager.saveTokens(loginData.accessToken, loginData.refreshToken)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Respuesta inválida del servidor"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    409  -> "El correo ya está registrado"
                    400  -> "Datos inválidos"
                    else -> "Error al registrarse"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión"))
        }
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        tokenManager.clearTokens()
    }

    suspend fun getAccessToken(): String? = tokenManager.getAccessToken()
}