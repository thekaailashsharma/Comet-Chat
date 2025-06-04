package learn.comet.chat.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cometchat.chat.models.MediaMessage
import learn.comet.chat.messages.data.MediaMessageState

@Composable
fun PdfMessageContent(
    message: MediaMessage,
    mediaState: MediaMessageState?,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = "PDF",
                tint = textColor
            )
            
            Column {
                Text(
                    text = message.attachment?.fileName ?: "PDF Document",
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                when (mediaState) {
                    is MediaMessageState.Uploading -> {
                        LinearProgressIndicator(
                            progress = mediaState.progress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Uploading... ${mediaState.progress}%",
                            color = textColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is MediaMessageState.Downloading -> {
                        LinearProgressIndicator(
                            progress = mediaState.progress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Opening PDF... ${mediaState.progress}%",
                            color = textColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is MediaMessageState.Error -> {
                        Text(
                            text = mediaState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is MediaMessageState.Sent -> {
                        if (message.attachment?.fileSize != null) {
                            Text(
                                text = "PDF • ${formatFileSize((message.attachment?.fileSize ?: 0).toLong())}",
                                color = textColor.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    else -> {
                        if (message.attachment?.fileSize != null) {
                            Text(
                                text = formatFileSize((message.attachment?.fileSize ?: 0).toLong()),
                                color = textColor.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
} 