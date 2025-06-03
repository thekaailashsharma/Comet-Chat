package learn.comet.chat.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import learn.comet.chat.core.CometChatManager

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = CometChatManager.getInstance().loginWithEmailPassword(email, password)
                result.fold(
                    onSuccess = { _uiState.value = AuthUiState.Success },
                    onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Login failed") }
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = CometChatManager.getInstance().signUpWithEmailPassword(email, password)
                result.fold(
                    onSuccess = { _uiState.value = AuthUiState.Success },
                    onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Sign up failed") }
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        CometChatManager.getInstance().cleanup()
    }
}

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
} 