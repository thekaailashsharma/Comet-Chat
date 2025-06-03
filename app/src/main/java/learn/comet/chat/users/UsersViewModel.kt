package learn.comet.chat.users

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.models.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import learn.comet.chat.users.data.UsersRepository

private const val TAG = "UsersViewModel"

class UsersViewModel(
    private val repository: UsersRepository = UsersRepository()
) : ViewModel() {
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()
    
    private val _uiState = MutableStateFlow<UsersUiState>(UsersUiState.Initial)
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = UsersUiState.Loading
            try {
                repository.getUsers(searchKeyword = _searchQuery.value.takeIf { it.isNotBlank() })
                    .catch { error ->
                        Log.e(TAG, "Error loading users: ${error.message}")
                        _uiState.value = UsersUiState.Error(error.message ?: "Failed to load users")
                    }
                    .collect { usersList ->
                        Log.d(TAG, "Loaded ${usersList.size} users")
                        _users.value = usersList
                        _uiState.value = UsersUiState.Success
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading users: ${e.message}")
                _uiState.value = UsersUiState.Error(e.message ?: "Failed to load users")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        loadUsers() // Reload users with new search query
    }
}

sealed class UsersUiState {
    object Initial : UsersUiState()
    object Loading : UsersUiState()
    object Success : UsersUiState()
    data class Error(val message: String) : UsersUiState()
} 