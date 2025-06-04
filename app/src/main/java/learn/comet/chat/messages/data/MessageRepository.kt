package learn.comet.chat.messages.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import learn.comet.chat.utils.FileUtils
import learn.comet.chat.viewer.MediaType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "MessageRepository"

class MessageRepository(
    private val context: Context
) {
    private val _messageUpdates = MutableSharedFlow<BaseMessage>(replay = 0)
    val messageUpdates = _messageUpdates.asSharedFlow()

    private val _mediaMessageStates = MutableStateFlow<Map<Int, MediaMessageState>>(emptyMap())
    val mediaMessageStates: StateFlow<Map<Int, MediaMessageState>> = _mediaMessageStates.asStateFlow()

    private val _replyToMessage = MutableStateFlow<BaseMessage?>(null)
    val replyToMessage: StateFlow<BaseMessage?> = _replyToMessage.asStateFlow()

    fun setReplyToMessage(message: BaseMessage?) {
        _replyToMessage.value = message
    }

    fun getMessages(receiverId: String, limit: Int = 30): Flow<List<BaseMessage>> = flow {
        Log.d(TAG, "Fetching messages for receiver: $receiverId")
        
        try {
            val messages = suspendCancellableCoroutine { continuation ->
                val messagesRequest = MessagesRequest.MessagesRequestBuilder()
                    .setUID(receiverId)
                    .setLimit(limit)
                    .build()

                messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<BaseMessage>>() {
                    override fun onSuccess(messages: List<BaseMessage>) {
                        Log.d("Messagessssss", "Successfully fetched ${messages.size} messages")
                        Log.d("Messagesssss", "Successfully fetched $messages")
                        continuation.resume(messages)
                    }

                    override fun onError(e: CometChatException?) {
                        Log.e(TAG, "Error fetching messages: ${e?.message}")
                        continuation.resumeWithException(e ?: Exception("Unknown error"))
                    }
                })
            }
            emit(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while fetching messages: ${e.message}")
            throw e
        }
    }

    fun loadMoreMessages(receiverId: String, lastMessage: BaseMessage?, limit: Int = 30): Flow<List<BaseMessage>> = flow {
        Log.d(TAG, "Loading more messages for receiver: $receiverId")
        
        try {
            val messages = suspendCancellableCoroutine { continuation ->
                val messagesRequest = MessagesRequest.MessagesRequestBuilder()
                    .setUID(receiverId)
                    .setLimit(limit)
                    .hideReplies(true)
                    .hideDeletedMessages(true)
                    .setMessageId(lastMessage?.id ?: 0)
                    .build()

                messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<BaseMessage>>() {
                    override fun onSuccess(messages: List<BaseMessage>) {
                        Log.d(TAG, "Successfully loaded ${messages.size} more messages")
                        continuation.resume(messages)
                    }

                    override fun onError(e: CometChatException?) {
                        Log.e(TAG, "Error loading more messages: ${e?.message}")
                        continuation.resumeWithException(e ?: Exception("Unknown error"))
                    }
                })
            }
            emit(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while loading more messages: ${e.message}")
            throw e
        }
    }

    suspend fun sendTextMessage(receiverId: String, message: String): Result<TextMessage> {
        return try {
            Log.d(TAG, "Sending message to receiver: $receiverId")
            val sentMessage = suspendCancellableCoroutine { continuation ->
                val textMessage = TextMessage(
                    receiverId,
                    message,
                    CometChatConstants.RECEIVER_TYPE_USER
                ).apply {
                    // Add reply metadata if replying to a message
                    _replyToMessage.value?.let { replyTo ->
                        val replyMetadata = ReplyMetadata.fromMessage(replyTo)
                        metadata = ReplyMetadata.toJson(replyMetadata)
                    }
                }

                CometChat.sendMessage(textMessage, object : CometChat.CallbackListener<TextMessage>() {
                    override fun onSuccess(message: TextMessage) {
                        Log.d(TAG, "Message sent successfully: ${message.id}")
                        _messageUpdates.tryEmit(message)
                        // Clear reply state after successful send
                        _replyToMessage.value = null
                        continuation.resume(message)
                    }

                    override fun onError(e: CometChatException) {
                        Log.e(TAG, "Error sending message: ${e.message}")
                        continuation.resumeWithException(e)
                    }
                })
            }
            Result.success(sentMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while sending message: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun sendMediaMessage(
        receiverId: String,
        uri: Uri,
        type: MediaType,
        caption: String = ""
    ): Result<MediaMessage> {
        return try {
            Log.d(TAG, "Sending media message to receiver: $receiverId")
            val attachment = FileUtils.getFileFromUri(context, uri) ?: throw Exception("Failed to get file from URI")
            
            val sentMessage = suspendCancellableCoroutine { continuation ->
                val mediaMessage = MediaMessage(
                    receiverId,
                    attachment,
                    type.toMessageType(),
                    CometChatConstants.RECEIVER_TYPE_USER
                ).apply {
                    this.caption = caption
                    // Add reply metadata if replying to a message
                    _replyToMessage.value?.let { replyTo ->
                        val replyMetadata = ReplyMetadata.fromMessage(replyTo)
                        metadata = ReplyMetadata.toJson(replyMetadata)
                    }
                }

                updateMediaMessageState(mediaMessage.id, MediaMessageState.Initial)

                CometChat.sendMediaMessage(mediaMessage, object : CometChat.CallbackListener<MediaMessage>() {
                    override fun onSuccess(message: MediaMessage) {
                        Log.d(TAG, "Media message sent successfully: ${message.id}")
                        updateMediaMessageState(message.id, MediaMessageState.Sent)
                        _messageUpdates.tryEmit(message)
                        // Clear reply state after successful send
                        _replyToMessage.value = null
                        continuation.resume(message)
                    }

                    override fun onError(e: CometChatException) {
                        Log.e(TAG, "Error sending media message: ${e.message}")
                        updateMediaMessageState(mediaMessage.id, MediaMessageState.Error(e.message ?: "Failed to send"))
                        continuation.resumeWithException(e)
                    }
                })
            }
            Result.success(sentMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while sending media message: ${e.message}")
            Result.failure(e)
        }
    }

    internal fun updateMediaMessageState(messageId: Int, state: MediaMessageState) {
        _mediaMessageStates.value = _mediaMessageStates.value.toMutableMap().apply {
            put(messageId, state)
        }
    }

    fun listenForMessages(currentUserId: String): Flow<BaseMessage> = callbackFlow {
        Log.d(TAG, "Starting message listener for user: $currentUserId")
        val listenerID = "MessageListener_$currentUserId"
        
        val messageListener = object : CometChat.MessageListener() {
            override fun onTextMessageReceived(message: TextMessage) {
                handleMessageReceived(message)
            }

            override fun onMediaMessageReceived(message: MediaMessage) {
                handleMediaMessageReceived(message)
            }

            override fun onMessagesDelivered(messageReceipt: MessageReceipt?) {
                Log.d(TAG, "Message delivered - ID: ${messageReceipt?.messageId}")
            }

            override fun onMessagesRead(messageReceipt: MessageReceipt?) {
                Log.d(TAG, "Message read - ID: ${messageReceipt?.messageId}")
            }

            private fun handleMessageReceived(message: BaseMessage) {
                val senderName = message.sender?.name ?: "Unknown"
                val receiverName = when (val receiver = message.receiver) {
                    is User -> receiver.name
                    else -> "Unknown"
                }
                Log.d(TAG, "Message received - ID: ${message.id}, From: $senderName, To: $receiverName")
                trySend(message)
                _messageUpdates.tryEmit(message)
            }

            private fun handleMediaMessageReceived(message: MediaMessage) {
                updateMediaMessageState(message.id, MediaMessageState.Received)
                handleMessageReceived(message)
            }
        }
        
        CometChat.addMessageListener(listenerID, messageListener)

        // Also listen for message updates (sent messages)
        launch {
            messageUpdates.collect { message ->
                Log.d(TAG, "Message update received - ID: ${message.id}")
                trySend(message)
            }
        }

        awaitClose {
            Log.d(TAG, "Removing message listener: $listenerID")
            CometChat.removeMessageListener(listenerID)
        }
    }.distinctUntilChanged { old, new -> old.id == new.id }

    private fun MediaType.toMessageType(): String = when (this) {
        MediaType.IMAGE -> CometChatConstants.MESSAGE_TYPE_IMAGE
        MediaType.PDF -> CometChatConstants.MESSAGE_TYPE_FILE
    }
}

sealed class MediaMessageState {
    object Initial : MediaMessageState()
    object Received : MediaMessageState()
    object Sent : MediaMessageState()
    data class Uploading(val progress: Int) : MediaMessageState()
    data class Downloading(val progress: Int) : MediaMessageState()
    data class Error(val message: String) : MediaMessageState()
} 