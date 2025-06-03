package learn.comet.chat.core

import android.content.Context
import android.util.Log
import com.cometchat.chat.core.AppSettings
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "CometChatManager"

class CometChatManager private constructor() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState
    private var isInitialized = false

    companion object {
        private const val APP_ID = "2762909d74f13599" // Replace with your App ID
        private const val REGION = "in" // Replace with your Region
        private const val AUTH_KEY = "c4e958749b5bd3a04b212dc1a84f03a49e31a8d7" // Replace with your Auth Key
        private var instance: CometChatManager? = null

        fun getInstance(): CometChatManager {
            return instance ?: synchronized(this) {
                instance ?: CometChatManager().also { instance = it }
            }
        }
    }

    init {
        setupLoginListeners()
    }

    private fun setupLoginListeners() {
        CometChat.addLoginListener("LOGIN_LISTENER", object : CometChat.LoginListener() {
            override fun loginSuccess(user: User) {
                Log.d(TAG, "Login success: ${user.name}")
                _loginState.value = LoginState.Success(user)
            }

            override fun loginFailure(e: CometChatException) {
                Log.e(TAG, "Login failure: ${e.message}")
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
            }

            override fun logoutSuccess() {
                Log.d(TAG, "Logout success")
                _loginState.value = LoginState.LoggedOut
            }

            override fun logoutFailure(e: CometChatException) {
                Log.e(TAG, "Logout failure: ${e.message}")
                _loginState.value = LoginState.Error(e.message ?: "Logout failed")
            }
        })
    }

    suspend fun initializeCometChat(context: Context): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (isInitialized) {
                    return@withContext Result.success("Already initialized")
                }

                Log.d(TAG, "Initializing CometChat...")
                val result = suspendCancellableCoroutine { continuation ->
                    val appSettings = AppSettings.AppSettingsBuilder()
                        .subscribePresenceForAllUsers()
                        .setRegion(REGION)
                        .build()

                    CometChat.init(context, APP_ID, appSettings, object : CometChat.CallbackListener<String>() {
                        override fun onSuccess(message: String) {
                            Log.d(TAG, "CometChat initialized successfully")
                            isInitialized = true
                            // Check if user is already logged in after initialization
                            CometChat.getLoggedInUser()?.let {
                                _loginState.value = LoginState.Success(it)
                            }
                            continuation.resume(message)
                        }

                        override fun onError(e: CometChatException) {
                            Log.e(TAG, "CometChat initialization failed: ${e.message}")
                            continuation.resumeWithException(e)
                        }
                    })
                }
                Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during CometChat initialization: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun loginWithEmailPassword(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isInitialized) {
                    return@withContext Result.failure(Exception("CometChat not initialized"))
                }

                Log.d(TAG, "Attempting login for email: $email")
                // For demo purposes, we'll create a UID from email
                val uid = email.substringBefore("@")
                val user = suspendCancellableCoroutine { continuation ->
                    CometChat.login(uid, AUTH_KEY, object : CometChat.CallbackListener<User>() {
                        override fun onSuccess(user: User) {
                            Log.d(TAG, "Login successful for user: ${user.name}")
                            continuation.resume(user)
                        }

                        override fun onError(e: CometChatException) {
                            Log.e(TAG, "Login failed: ${e.message}")
                            continuation.resumeWithException(e)
                        }
                    })
                }
                Result.success(user)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during login: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun signUpWithEmailPassword(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isInitialized) {
                    return@withContext Result.failure(Exception("CometChat not initialized"))
                }

                Log.d(TAG, "Attempting signup for email: $email")
                // For demo purposes, we'll create a UID from email
                val uid = email.substringBefore("@")
                val name = email.substringBefore("@")
                
                // Create user in CometChat
                val user = User(uid, name)
                val result = suspendCancellableCoroutine { continuation ->
                    CometChat.createUser(user, AUTH_KEY, object : CometChat.CallbackListener<User>() {
                        override fun onSuccess(user: User) {
                            Log.d(TAG, "User created successfully: ${user.name}")
                            continuation.resume(user)
                        }

                        override fun onError(e: CometChatException) {
                            Log.e(TAG, "User creation failed: ${e.message}")
                            continuation.resumeWithException(e)
                        }
                    })
                }
                
                // Auto login after signup
                loginWithEmailPassword(email, password)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during signup: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun logout() {
        return withContext(Dispatchers.IO) {
            if (!isInitialized) {
                throw Exception("CometChat not initialized")
            }

            suspendCancellableCoroutine { continuation ->
                CometChat.logout(object : CometChat.CallbackListener<String>() {
                    override fun onSuccess(message: String) {
                        Log.d(TAG, "Logout successful")
                        continuation.resume(Unit)
                    }

                    override fun onError(e: CometChatException) {
                        Log.e(TAG, "Logout failed: ${e.message}")
                        continuation.resumeWithException(e)
                    }
                })
            }
        }
    }

    fun getLoggedInUser(): User? {
        return if (isInitialized) CometChat.getLoggedInUser() else null
    }

    fun cleanup() {
        Log.d(TAG, "Cleaning up CometChat manager")
        CometChat.removeLoginListener("LOGIN_LISTENER")
    }
}

sealed class LoginState {
    object Initial : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
    object LoggedOut : LoginState()
} 