package learn.comet.chat.messages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import kotlinx.coroutines.launch
import learn.comet.chat.messages.data.MediaMessageState
import learn.comet.chat.messages.data.ReplyMetadata
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableMessageItem(
    message: BaseMessage,
    mediaState: MediaMessageState?,
    onReply: (BaseMessage) -> Unit,
    onSwipeStateChanged: (SwipeableState<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isCurrentUser = message.sender?.uid == CometChat.getLoggedInUser()?.uid
    val swipeableState = rememberSwipeableState(0)
    val sizePx = with(LocalDensity.current) { 96.dp.toPx() }
    val anchors = mapOf(0f to 0, sizePx to 1)

    // Monitor swipe state changes
    LaunchedEffect(swipeableState.targetValue) {
        onSwipeStateChanged(swipeableState)
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Reply icon that appears when swiping
        AnimatedVisibility(
            visible = swipeableState.offset.value > 0,
            enter = fadeIn(animationSpec = tween(100)),
            exit = fadeOut(animationSpec = tween(100)),
            modifier = Modifier
                .align(if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = "Reply",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .alpha(swipeableState.offset.value / sizePx)
            )
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = if (isCurrentUser) {
                            -swipeableState.offset.value.roundToInt()
                        } else {
                            swipeableState.offset.value.roundToInt()
                        },
                        y = 0
                    )
                }
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    orientation = Orientation.Horizontal,
                    thresholds = { _, _ -> FractionalThreshold(0.3f) }
                )
        ) {
            MessageContent(
                message = message,
                mediaState = mediaState
            )
        }

        // Handle swipe completion
        LaunchedEffect(swipeableState.currentValue) {
            if (swipeableState.currentValue == 1) {
                onReply(message)
                swipeableState.animateTo(0)
            }
        }
    }
}

@Composable
fun MessageContent(
    message: BaseMessage,
    mediaState: MediaMessageState?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Show reply preview if this message is a reply
        message.metadata?.let { metadata ->
            ReplyMetadata.fromJson(metadata)?.let { replyMetadata ->
                ReplyPreview(replyMetadata)
            }
        }
        
        // Original message content
        MessageItem(
            message = message,
            mediaState = mediaState
        )
    }
}

@Composable
fun ReplyPreview(
    replyMetadata: ReplyMetadata,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Replying to ${replyMetadata.repliedToSender}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = replyMetadata.repliedToText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
} 