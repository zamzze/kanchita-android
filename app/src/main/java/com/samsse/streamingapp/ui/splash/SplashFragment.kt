package com.samsse.streamingapp.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.samsse.streamingapp.R
import com.samsse.streamingapp.data.remote.RefreshApiProvider
import com.samsse.streamingapp.data.remote.RefreshRequest
import com.samsse.streamingapp.data.remote.TokenManager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SplashFragment : Fragment() {

    private val tokenManager: TokenManager by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_splash, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAuth()
    }

    private fun checkAuth() {
        lifecycleScope.launch {
            val accessToken  = tokenManager.getAccessToken()
            val refreshToken = tokenManager.getRefreshToken()

            when {
                // No hay tokens → ir al login
                accessToken == null -> {
                    goToLogin()
                }
                // Hay tokens → intentar refresh para validar sesión
                else -> {
                    val isValid = tryRefresh(refreshToken)
                    if (isValid) {
                        goToHome()
                    } else {
                        goToLogin()
                    }
                }
            }
        }
    }

    private suspend fun tryRefresh(refreshToken: String?): Boolean {
        if (refreshToken == null) return false
        return try {
            val response = RefreshApiProvider.get().refresh(RefreshRequest(refreshToken))
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) {
                    tokenManager.saveTokens(data.accessToken, data.refreshToken)
                    true
                } else false
            } else {
                tokenManager.clearTokens()
                false
            }
        } catch (e: Exception) {
            // Sin conexión — usar el token existente igual
            true
        }
    }

    private fun goToLogin() {
        findNavController().navigate(R.id.action_splash_to_login)
    }

    private fun goToHome() {
        findNavController().navigate(R.id.action_splash_to_home)
    }
}