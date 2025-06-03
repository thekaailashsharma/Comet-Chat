package learn.comet.chat.messages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
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
    isHighlighted: Boolean = false,
    onReply: (BaseMessage) -> Unit,
    onSwipeStateChanged: (SwipeableState<Int>) -> Unit,
    onReplyClick: (Int) -> Unit,
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
                mediaState = mediaState,
                isHighlighted = isHighlighted,
                onReplyClick = onReplyClick
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
    mediaState: MediaMessageState?,
    isHighlighted: Boolean = false,
    onReplyClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Show reply preview if this message is a reply
        message.metadata?.let { metadata ->
            ReplyMetadata.fromJson(metadata)?.let { replyMetadata ->
                ReplyPreview(
                    replyMetadata = replyMetadata,
                    onClick = { onReplyClick(replyMetadata.repliedToMessageId) }
                )
            }
        }
        
        // Original message content
        MessageItem(
            message = message,
            mediaState = mediaState,
            isHighlighted = isHighlighted
        )
    }
}

@Composable
fun ReplyPreview(
    replyMetadata: ReplyMetadata,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical accent line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(24.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(1.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = replyMetadata.repliedToSender,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = replyMetadata.repliedToText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
} 