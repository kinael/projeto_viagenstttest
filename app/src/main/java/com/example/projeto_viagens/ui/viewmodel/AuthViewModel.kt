package com.example.projeto_viagens.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_viagens.data.local.AppDatabase
import com.example.projeto_viagens.data.local.UserEntity
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.getDatabase(application).userDao()

    var loginUiState by mutableStateOf(LoginUiState())
        private set

    var registerUiState by mutableStateOf(RegisterUiState())
        private set

    var forgotPasswordUiState by mutableStateOf(ForgotPasswordUiState())
        private set

    // --- Login ---
    fun onLoginEmailChange(value: String) {
        loginUiState = loginUiState.copy(email = value, errorMessage = null)
    }

    fun onLoginPasswordChange(value: String) {
        loginUiState = loginUiState.copy(password = value, errorMessage = null)
    }

    fun onLoginPasswordVisibilityToggle() {
        loginUiState = loginUiState.copy(isPasswordVisible = !loginUiState.isPasswordVisible)
    }

    fun login(onSuccess: () -> Unit) {
        if (loginUiState.email.isBlank() || loginUiState.password.isBlank()) {
            loginUiState = loginUiState.copy(errorMessage = "Preencha e-mail e senha.")
            return
        }

        loginUiState = loginUiState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val user = userDao.getUserByEmail(loginUiState.email)
            when {
                user == null -> {
                    loginUiState = loginUiState.copy(
                        isLoading = false,
                        errorMessage = "E-mail não cadastrado."
                    )
                }
                user.password != loginUiState.password -> {
                    loginUiState = loginUiState.copy(
                        isLoading = false,
                        errorMessage = "Senha incorreta."
                    )
                }
                else -> {
                    loginUiState = loginUiState.copy(isLoading = false)
                    onSuccess()
                }
            }
        }
    }

    // --- Register ---
    fun onRegisterNameChange(value: String) {
        registerUiState = registerUiState.copy(name = value, errorMessage = null)
    }

    fun onRegisterEmailChange(value: String) {
        registerUiState = registerUiState.copy(email = value, errorMessage = null)
    }

    fun onRegisterPhoneChange(value: String) {
        registerUiState = registerUiState.copy(phone = value, errorMessage = null)
    }

    fun onRegisterPasswordChange(value: String) {
        registerUiState = registerUiState.copy(password = value, errorMessage = null)
    }

    fun onRegisterConfirmPasswordChange(value: String) {
        registerUiState = registerUiState.copy(confirmPassword = value, errorMessage = null)
    }

    fun onRegisterPasswordVisibilityToggle() {
        registerUiState = registerUiState.copy(isPasswordVisible = !registerUiState.isPasswordVisible)
    }

    fun onRegisterConfirmPasswordVisibilityToggle() {
        registerUiState =
            registerUiState.copy(isConfirmPasswordVisible = !registerUiState.isConfirmPasswordVisible)
    }

    fun resetRegisterState() {
        registerUiState = RegisterUiState()
    }

    fun register(onSuccess: () -> Unit) {
        val state = registerUiState

        if (state.name.isBlank() || state.email.isBlank() || state.phone.isBlank() ||
            state.password.isBlank() || state.confirmPassword.isBlank()
        ) {
            registerUiState = state.copy(errorMessage = "Todos os campos são obrigatórios.")
            return
        }
        if (state.password != state.confirmPassword) {
            registerUiState = state.copy(errorMessage = "As senhas não coincidem.")
            return
        }

        registerUiState = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val existing = userDao.getUserByEmail(state.email)
                if (existing != null) {
                    registerUiState = state.copy(
                        isLoading = false,
                        errorMessage = "E-mail já cadastrado."
                    )
                    return@launch
                }

                userDao.insertUser(
                    UserEntity(
                        name = state.name,
                        email = state.email,
                        phone = state.phone,
                        password = state.password
                    )
                )

                registerUiState = state.copy(isLoading = false)
                onSuccess()

            } catch (e: Exception) {
                registerUiState = state.copy(
                    isLoading = false,
                    errorMessage = "Erro ao cadastrar. Tente novamente."
                )
            }
        }
    }

    // --- Forgot Password ---
    fun onForgotPasswordEmailChange(value: String) {
        forgotPasswordUiState =
            forgotPasswordUiState.copy(email = value, errorMessage = null, successMessage = null)
    }

    fun sendPasswordRecovery(onSuccess: () -> Unit) {
        if (forgotPasswordUiState.email.isBlank()) {
            forgotPasswordUiState = forgotPasswordUiState.copy(errorMessage = "Informe seu e-mail.")
            return
        }
        forgotPasswordUiState = forgotPasswordUiState.copy(
            successMessage = "E-mail de recuperação enviado!"
        )
        onSuccess()
    }
}