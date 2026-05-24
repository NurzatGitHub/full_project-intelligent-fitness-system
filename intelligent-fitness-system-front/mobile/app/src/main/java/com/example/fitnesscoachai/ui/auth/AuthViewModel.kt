package com.example.fitnesscoachai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.example.fitnesscoachai.data.models.GoogleLoginRequest
import com.example.fitnesscoachai.data.models.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class AuthViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(email, password))

                if (response.isSuccessful && response.body() != null) {
                    _loginState.value = LoginState.Success(response.body()!!)
                    return@launch
                }

                val serverMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                val code = response.code()

                val pretty = when (code) {
                    400 -> "Wrong data. ${serverMsg ?: ""}".trim()
                    401 -> "Invalid email or password"
                    403 -> "Account disabled"
                    404 -> "Endpoint not found (check BASE_URL/api path)"
                    else -> "Login failed ($code). ${serverMsg ?: response.message()}".trim()
                }

                _loginState.value = LoginState.Error(pretty)

            } catch (e: HttpException) {
                _loginState.value = LoginState.Error("HTTP error: ${e.code()}")
            } catch (e: IOException) {
                // 🔥 Это как раз твой кейс "Failed to connect..."
                _loginState.value = LoginState.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Network error: ${e.message ?: "unknown"}")
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = RetrofitClient.apiService.google(GoogleLoginRequest(idToken))
                if (response.isSuccessful && response.body() != null) {
                    _loginState.value = LoginState.Success(response.body()!!)
                } else {
                    val body = response.errorBody()?.string()
                    _loginState.value = LoginState.Error(
                        "Google login failed: ${response.code()} ${body ?: ""}".trim()
                    )
                }
            } catch (e: IOException) {
                _loginState.value = LoginState.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Network error: ${e.message}")
            }
        }
    }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val authResponse: com.example.fitnesscoachai.data.models.AuthResponse) : LoginState()
        data class Error(val message: String) : LoginState()
    }
}