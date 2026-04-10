package com.samsse.streamingapp.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenManager.getAccessToken() }
        val request = addToken(chain.request(), token)
        val response = chain.proceed(request)

        // Si el token expiró, intentar refresh
        if (response.code == 401) {
            response.close()
            val newToken = runBlocking { refreshToken() }
            if (newToken != null) {
                return chain.proceed(addToken(chain.request(), newToken))
            }
        }

        return response
    }

    private fun addToken(request: Request, token: String?): Request {
        return if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
    }

    private suspend fun refreshToken(): String? {
        return try {
            val refreshToken = tokenManager.getRefreshToken() ?: return null
            val refreshApi   = RefreshApiProvider.get()
            val response     = refreshApi.refresh(RefreshRequest(refreshToken))
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) {
                    tokenManager.saveTokens(data.accessToken, data.refreshToken)
                    data.accessToken
                } else null
            } else {
                tokenManager.clearTokens()
                null
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            null
        }
    }
}