package learn.comet.chat.messages

import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.TextMessage
import kotlinx.coroutines.launch
import learn.comet.chat.messages.data.MediaMessageState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MessageViewModel,
    receiverId: String,
    onBackPressed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val mediaState by viewModel.mediaState.collectAsState()
    val mediaPickerState by viewModel.mediaPickerState.collectAsState()
    val mediaMessageStates by viewModel.mediaMessageStates.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(receiverId) {
        viewModel.startChat(receiverId)
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                receiverId = receiverId,
                onBackPressed = onBackPressed
            )
        },
        bottomBar = {
            Column {
                if (mediaState is MediaState.Preview) {
                    MediaPreview(
                        state = mediaState as MediaState.Preview,
                        onCaptionChange = viewModel::updateMediaCaption,
                        onSend = viewModel::sendMediaMessage,
                        onDismiss = viewModel::clearMediaPreview,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                MessageInput(
                    text = messageText,
                    onTextChange = viewModel::updateMessageText,
                    onSendClick = viewModel::sendMessage,
                    onAttachClick = { viewModel.showMediaPicker(MediaPickerState.Gallery) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MessageList(
                messages = messages,
                mediaMessageStates = mediaMessageStates,
                listState = listState,
                onLoadMore = viewModel::loadMoreMessages,
                modifier = Modifier.fillMaxSize()
            )

            // Loading indicator
            if (uiState is MessageUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Media picker
            MediaPicker(
                state = mediaPickerState,
                onMediaSelected = { uri, type -> viewModel.setMediaPreview(uri, type) },
                onDismiss = viewModel::hideMediaPicker
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    receiverId: String,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = receiverId,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Online", // You can make this dynamic based on user status
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun MessageList(
    messages: List<BaseMessage>,
    mediaMessageStates: Map<Int, MediaMessageState>,
    listState: LazyListState,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        reverseLayout = true,
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(
            items = messages,
            key = { it.id }
        ) { message ->
            AnimatedMessageItem(
                message = message,
                mediaState = mediaMessageStates[message.id]
            )
        }

        item {
            if (messages.isNotEmpty()) {
                LoadMoreButton(onClick = onLoadMore)
            }
        }
    }
}

@Composable
fun AnimatedMessageItem(
    message: BaseMessage,
    mediaState: MediaMessageState?
) {
    val isCurrentUser = message.sender?.uid == CometChat.getLoggedInUser()?.uid

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInHorizontally(
            initialOffsetX = { if (isCurrentUser) it else -it }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        MessageItem(
            message = message,
            mediaState = mediaState
        )
    }
}

@Composable
fun MessageItem(
    message: BaseMessage,
    mediaState: MediaMessageState?
) {
    val isCurrentUser = message.sender?.uid == CometChat.getLoggedInUser()?.uid
    val backgroundColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (!isCurrentUser) {
                // Avatar
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = message.sender?.name?.firstOrNull()?.uppercase() ?: "#",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                        bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                    ),
                    color = backgroundColor,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        )
                    ) {
                        when (message) {
                            is TextMessage -> {
                                Text(
                                    text = message.text,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            is MediaMessage -> {
                                // Handle media message display
                                if (message.type == CometChatConstants.MESSAGE_TYPE_IMAGE) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(LocalContext.current)
                                                .data(message.attachment?.fileUrl)
                                                .build()
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    message.caption?.let { caption ->
                                        if (caption.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = caption,
                                                color = textColor,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = formatMessageTime(message.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttachClick) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Type a message") },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            IconButton(
                onClick = onSendClick,
                enabled = text.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun LoadMoreButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text("Load More Messages")
    }
}

internal fun formatMessageTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    return when {
        DateUtils.isToday(timestamp) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        timestamp > now - DateUtils.WEEK_IN_MILLIS -> {
            SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
} 