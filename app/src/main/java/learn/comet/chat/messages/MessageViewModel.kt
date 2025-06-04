package learn.comet.chat.messages

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.chat.models.MediaMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import learn.comet.chat.messages.data.MessageRepository
import kotlinx.coroutines.delay
import learn.comet.chat.messages.data.MediaMessageState
import learn.comet.chat.viewer.MediaType
import learn.comet.chat.viewer.MediaViewerData
import java.util.Date

private const val TAG = "MessageViewModel"

class MessageViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val repository: MessageRepository = MessageRepository(application)
    
    private val _messages = MutableStateFlow<List<BaseMessage>>(emptyList())
    val messages: StateFlow<List<BaseMessage>> = _messages.asStateFlow()
    
    private val _uiState = MutableStateFlow<MessageUiState>(MessageUiState.Initial)
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()
    
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()
    
    private val _mediaState = MutableStateFlow<MediaState>(MediaState.None)
    val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()
    
    private val _mediaPickerState = MutableStateFlow<MediaPickerState>(MediaPickerState.Hidden)
    val mediaPickerState: StateFlow<MediaPickerState> = _mediaPickerState.asStateFlow()

    val mediaMessageStates = repository.mediaMessageStates
    val replyToMessage = repository.replyToMessage
    
    private var currentReceiverId: String? = null
    private var isLoadingMore = false
    private var messageListenerJob: Job? = null

    private val _highlightedMessageId = MutableStateFlow<Int?>(null)
    val highlightedMessageId: StateFlow<Int?> = _highlightedMessageId.asStateFlow()

    private var _mediaViewerData = MutableStateFlow<MediaViewerData?>(null)
    val mediaViewerData: StateFlow<MediaViewerData?> = _mediaViewerData.asStateFlow()

    fun setReplyToMessage(message: BaseMessage) {
        repository.setReplyToMessage(message)
    }

    fun clearReplyToMessage() {
        repository.setReplyToMessage(null)
    }

    fun startChat(receiverId: String) {
        if (currentReceiverId == receiverId) return
        
        // Cancel existing listener
        messageListenerJob?.cancel()
        
        currentReceiverId = receiverId
        _messages.value = emptyList()
        loadInitialMessages()
        setupMessageListener()
    }

    private fun loadInitialMessages() {
        viewModelScope.launch {
            _uiState.value = MessageUiState.Loading
            try {
                currentReceiverId?.let { receiverId ->
                    repository.getMessages(receiverId)
                        .catch { error ->
                            Log.e(TAG, "Error loading messages: ${error.message}")
                            _uiState.value = MessageUiState.Error(error.message ?: "Failed to load messages")
                        }
                        .collect { messagesList ->
                            Log.d(TAG, "Loaded ${messagesList.size} messages")
                            updateMessages(messagesList)
                            _uiState.value = MessageUiState.Success
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading messages: ${e.message}")
                _uiState.value = MessageUiState.Error(e.message ?: "Failed to load messages")
            }
        }
    }

    fun loadMoreMessages() {
        if (isLoadingMore || _messages.value.isEmpty()) return
        
        viewModelScope.launch {
            isLoadingMore = true
            _uiState.value = MessageUiState.LoadingMore
            
            try {
                currentReceiverId?.let { receiverId ->
                    repository.loadMoreMessages(
                        receiverId = receiverId,
                        lastMessage = _messages.value.lastOrNull()
                    ).catch { error ->
                        Log.e(TAG, "Error loading more messages: ${error.message}")
                        _uiState.value = MessageUiState.Error(error.message ?: "Failed to load more messages")
                    }.collect { moreMessages ->
                        Log.d(TAG, "Loaded ${moreMessages.size} more messages")
                        updateMessages(_messages.value + moreMessages)
                        _uiState.value = MessageUiState.Success
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading more messages: ${e.message}")
                _uiState.value = MessageUiState.Error(e.message ?: "Failed to load more messages")
            } finally {
                isLoadingMore = false
            }
        }
    }

    private fun setupMessageListener() {
        messageListenerJob = viewModelScope.launch {
            try {
                val currentUser = CometChat.getLoggedInUser()
                if (currentUser == null) {
                    Log.e(TAG, "No logged in user found")
                    return@launch
                }

                repository.listenForMessages(currentUser.uid)
                    .catch { error ->
                        Log.e(TAG, "Error in message listener: ${error.message}")
                    }
                    .collect { message ->
                        if (shouldAddMessage(message)) {
                            updateMessages(_messages.value + message)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in message listener: ${e.message}")
            }
        }
    }

    private fun shouldAddMessage(message: BaseMessage): Boolean {
        val currentUserId = CometChat.getLoggedInUser()?.uid
        val receiverId = when (val receiver = message.receiver) {
            is User -> receiver.uid
            else -> null
        }
        val senderId = message.sender?.uid

        return when {
            // Message sent by current user to current receiver
            senderId == currentUserId && receiverId == currentReceiverId -> true
            // Message received from current receiver
            senderId == currentReceiverId && receiverId == currentUserId -> true
            else -> false
        }.also { shouldAdd ->
            Log.d(TAG, "Should add message ${message.id}? $shouldAdd " +
                "(currentUser: $currentUserId, currentReceiver: $currentReceiverId, " +
                "sender: $senderId, receiver: $receiverId)")
        }
    }

    private fun updateMessages(newMessages: List<BaseMessage>) {
        _messages.update { currentMessages ->
            (currentMessages + newMessages)
                .distinctBy { it.id }
                .sortedByDescending { it.sentAt }
        }
    }

    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isBlank() || currentReceiverId == null) return

        viewModelScope.launch {
            _uiState.value = MessageUiState.Sending
            try {
                repository.sendTextMessage(currentReceiverId!!, text)
                    .onSuccess {
                        Log.d(TAG, "Message sent successfully: ${it.id}")
                        _messageText.value = ""
                        _uiState.value = MessageUiState.Success
                        loadInitialMessages()
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to send message: ${error.message}")
                        _uiState.value = MessageUiState.Error(error.message ?: "Failed to send message")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while sending message: ${e.message}")
                _uiState.value = MessageUiState.Error(e.message ?: "Failed to send message")
            }
        }
    }

    fun showMediaPicker(type: MediaPickerState) {
        _mediaPickerState.value = type
    }

    fun hideMediaPicker() {
        _mediaPickerState.value = MediaPickerState.Hidden
    }

    fun setMediaPreview(uri: Uri, type: MediaType) {
        _mediaState.value = MediaState.Preview(uri, type)
    }

    fun clearMediaPreview() {
        _mediaState.value = MediaState.None
    }

    fun updateMediaCaption(caption: String) {
        val currentState = _mediaState.value
        if (currentState is MediaState.Preview) {
            _mediaState.value = currentState.copy(caption = caption)
        }
    }

    fun sendMediaMessage() {
        val currentState = _mediaState.value
        if (currentState !is MediaState.Preview || currentReceiverId == null) return

        viewModelScope.launch {
            _uiState.value = MessageUiState.Sending
            try {
                repository.sendMediaMessage(
                    receiverId = currentReceiverId!!,
                    uri = currentState.uri,
                    type = currentState.type,
                    caption = currentState.caption
                ).onSuccess {
                    Log.d(TAG, "Media message sent successfully: ${it.id}")
                    clearMediaPreview()
                    _uiState.value = MessageUiState.Success
                    loadInitialMessages()
                }.onFailure { error ->
                    Log.e(TAG, "Failed to send media message: ${error.message}")
                    _uiState.value = MessageUiState.Error(error.message ?: "Failed to send media message")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while sending media message: ${e.message}")
                _uiState.value = MessageUiState.Error(e.message ?: "Failed to send media message")
            }
        }
    }

    fun handlePdfSelection(uri: Uri) {
        setMediaPreview(uri, MediaType.PDF)
    }

    fun handlePdfClick(message: MediaMessage) {
        message.attachment?.fileUrl?.let { url ->
            handleMediaClick(
                uri = url.toUri(),
                type = MediaType.PDF,
                name = message.attachment?.fileName ?: "Document"
            )
        }
    }

    fun handleImageClick(message: MediaMessage) {
        message.attachment?.fileUrl?.let { url ->
            handleMediaClick(
                uri = url.toUri(),
                type = MediaType.IMAGE,
                name = message.attachment?.fileName ?: "Image"
            )
        }
    }

    fun handleMediaClick(uri: Uri, type: MediaType, name: String) {
        _mediaViewerData.value = MediaViewerData(
            uri = uri,
            type = type,
            name = name,
            timestamp = Date()
        )
    }

    fun clearMediaViewer() {
        _mediaViewerData.value = null
    }

    fun scrollToMessage(messageId: Int) {
        viewModelScope.launch {
            _highlightedMessageId.value = messageId
            // Clear highlight after animation duration
            delay(1000)
            _highlightedMessageId.value = null
        }
    }

    private fun updateMediaMessageState(messageId: Int, state: MediaMessageState) {
        repository.updateMediaMessageState(messageId, state)
    }

    override fun onCleared() {
        super.onCleared()
        messageListenerJob?.cancel()
        clearReplyToMessage()
        Log.d(TAG, "ViewModel cleared")
    }
}

sealed class MediaState {
    object None : MediaState()
    data class Preview(
        val uri: Uri,
        val type: MediaType,
        val caption: String = ""
    ) : MediaState()
}

enum class MediaPickerState {
    Hidden,
    Shown
}

sealed class MessageUiState {
    object Initial : MessageUiState()
    object Loading : MessageUiState()
    object LoadingMore : MessageUiState()
    object Sending : MessageUiState()
    object Success : MessageUiState()
    data class Error(val message: String) : MessageUiState()
} 