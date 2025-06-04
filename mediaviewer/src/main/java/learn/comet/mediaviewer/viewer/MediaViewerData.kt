package learn.comet.mediaviewer.viewer

import android.net.Uri
import java.util.Date

data class MediaViewerData(
    val uri: Uri,
    val type: MediaType,
    val name: String,
    val timestamp: Date = Date(),
    val headers: Map<String, String> = emptyMap()
)

enum class MediaType {
    PDF,
    IMAGE
} 