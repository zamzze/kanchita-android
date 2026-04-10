package com.samsse.streamingapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsse.streamingapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    sealed class AuthState {
        object Idle    : AuthState()
        object Loading : AuthState()
        object Success : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _state = MutableLiveData<AuthState>(AuthState.Idle)
    val state: LiveData<AuthState> = _state

    fun login(email: String, password: String) {
        if (!validateFields(email, password)) return
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = authRepository.login(email, password)
            _state.value = if (result.isSuccess) {
                AuthState.Success
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        if (!validateFields(email, password)) return
        if (password != confirmPassword) {
            _state.value = AuthState.Error("Las contraseñas no coinciden")
            return
        }
        if (password.length < 8) {
            _state.value = AuthState.Error("La contraseña debe tener al menos 8 caracteres")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = authRepository.register(email, password)
            _state.value = if (result.isSuccess) {
                AuthState.Success
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    private fun validateFields(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Completa todos los campos")
            return false
        }
        return true
    }
}