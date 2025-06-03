package learn.comet.chat

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import learn.comet.chat.auth.AuthScreen
import learn.comet.chat.auth.AuthViewModel
import learn.comet.chat.core.CometChatManager
import learn.comet.chat.messages.ChatScreen
import learn.comet.chat.messages.MessageViewModel
import learn.comet.chat.ui.theme.LearnCometChatTheme
import learn.comet.chat.users.UsersScreen
import learn.comet.chat.users.UsersViewModel

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val messageViewModel: MessageViewModel by lazy {
        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))[MessageViewModel::class.java]
    }
    private val usersViewModel: UsersViewModel by viewModels()
    private val navigationState = MutableStateFlow<NavigationState>(NavigationState.Initial)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize CometChat first
        initializeCometChat()

        setContent {
            LearnCometChatTheme {
                val currentState by navigationState.collectAsState()
                
                Scaffold { padding ->
                    when (currentState) {
                        NavigationState.Initial, NavigationState.Auth -> {
                            AuthScreen(
                                onAuthSuccess = {
                                    navigationState.value = NavigationState.Users
                                },
                                viewModel = authViewModel,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                            )
                        }
                        NavigationState.Users -> {
                            UsersScreen(
                                viewModel = usersViewModel,
                                onUserSelected = { user ->
                                    Log.d(TAG, "Selected user: ${user.name} (${user.uid})")
                                    navigationState.value = NavigationState.Chat(user.uid)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                            )
                        }
                        is NavigationState.Chat -> {
                            ChatScreen(
                                viewModel = messageViewModel,
                                receiverId = (currentState as NavigationState.Chat).userId,
                                onBackPressed = {
                                    navigationState.value = NavigationState.Users
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun initializeCometChat() {
        lifecycleScope.launch {
            try {
                CometChatManager.getInstance().initializeCometChat(applicationContext)
                    .onSuccess {
                        Log.d(TAG, "CometChat initialized successfully")
                        // Check if user is already logged in
                        if (CometChatManager.getInstance().getLoggedInUser() != null) {
                            navigationState.value = NavigationState.Users
                        } else {
                            navigationState.value = NavigationState.Auth
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to initialize CometChat: ${error.message}")
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to initialize CometChat: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        navigationState.value = NavigationState.Auth
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during CometChat initialization: ${e.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Failed to initialize CometChat: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                navigationState.value = NavigationState.Auth
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CometChatManager.getInstance().cleanup()
    }
}

sealed class NavigationState {
    object Initial : NavigationState()
    object Auth : NavigationState()
    object Users : NavigationState()
    data class Chat(val userId: String) : NavigationState()
}
