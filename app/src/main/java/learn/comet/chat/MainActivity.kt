package learn.comet.chat

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.rajat.pdfviewer.HeaderData
import com.rajat.pdfviewer.PdfRendererView
import com.rajat.pdfviewer.compose.PdfRendererViewCompose
import com.rajat.pdfviewer.util.PdfSource

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
import learn.comet.chat.viewer.MediaViewer
import learn.comet.chat.viewer.MediaViewerData
import learn.comet.chat.viewer.MediaType
import java.util.Date

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
                    when (val state = currentState) {
                        is NavigationState.Initial, is NavigationState.Auth -> {
                            AuthScreen(
                                viewModel = authViewModel,
                                onAuthSuccess = {
                                    navigationState.value = NavigationState.Users
                                },
                                modifier = Modifier.padding(padding)
                            )
                        }
                        is NavigationState.Users -> {
                            UsersScreen(
                                viewModel = usersViewModel,
                                onUserSelected = { user ->
                                    navigationState.value = NavigationState.Chat(user.uid)
                                },
                                modifier = Modifier.padding(padding)
                            )
                        }
                        is NavigationState.Chat -> {
                            ChatScreen(
                                viewModel = messageViewModel,
                                receiverId = state.userId,
                                onBackPressed = {
                                    navigationState.value = NavigationState.Users
                                },
                                onNavigateToMedia = { mediaData ->
                                    navigationState.value = NavigationState.MediaViewer(
                                        data = mediaData,
                                        previousChatUserId = state.userId
                                    )
                                },
                                modifier = Modifier.padding(padding)
                            )
                        }
                        is NavigationState.MediaViewer -> {
                            MediaViewer(
                                data = state.data,
                                onBackPressed = {
                                    navigationState.value = NavigationState.Chat(state.previousChatUserId)
                                },
                                onShare = {
                                    // Handle sharing
                                },
                                modifier = Modifier.padding(padding)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uri: Uri,
    onBackPressed: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var downloadProgress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var isZoomedIn by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableStateOf(1f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("PDF Document")
                        if (!isLoading) {
                            Text(
                                text = "Page $currentPage of $totalPages",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, "Share")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val statusCallback = remember(currentPage, totalPages, isLoading, errorMessage, downloadProgress) {
                object : PdfRendererView.StatusCallBack {
                    override fun onPdfLoadStart() {
                        isLoading = true
                        errorMessage = null
                    }

                    override fun onPdfLoadProgress(progress: Int, downloadedBytes: Long, totalBytes: Long?) {
                        downloadProgress = progress
                        isLoading = progress < 100
                    }

                    override fun onPdfLoadSuccess(absolutePath: String) {
                        isLoading = false
                        errorMessage = null
                    }

                    override fun onError(error: Throwable) {
                        isLoading = false
                        errorMessage = error.message
                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    }

                    override fun onPageChanged(page: Int, total: Int) {
                        currentPage = page
                        totalPages = total
                    }
                }
            }

            val zoomListener = remember(isZoomedIn, zoomScale) {
                object : PdfRendererView.ZoomListener {
                    override fun onZoomChanged(zoomed: Boolean, scale: Float) {
                        isZoomedIn = zoomed
                        zoomScale = scale
                    }
                }
            }

            PdfRendererViewCompose(
                source = PdfSource.Remote(uri.toString()),
                lifecycleOwner = lifecycleOwner,
                modifier = Modifier.fillMaxSize(),
                headers = HeaderData(mapOf()),
                statusCallBack = statusCallback,
                zoomListener = zoomListener
            )

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading PDF... $downloadProgress%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Error message
            errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading PDF",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBackPressed) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}

sealed class NavigationState {
    object Initial : NavigationState()
    object Auth : NavigationState()
    object Users : NavigationState()
    data class Chat(val userId: String) : NavigationState()
    data class MediaViewer(
        val data: MediaViewerData,
        val previousChatUserId: String
    ) : NavigationState()
}
