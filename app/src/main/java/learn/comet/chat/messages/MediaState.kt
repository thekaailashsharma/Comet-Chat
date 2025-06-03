package learn.comet.chat.messages

import android.net.Uri

sealed class MediaState {
    object None : MediaState()
    data class Preview(
        val uri: Uri,
        val type: MediaType,
        val caption: String = ""
    ) : MediaState()
}

enum class MediaType {
    IMAGE,
    VIDEO
}

sealed class MediaPickerState {
    object Hidden : MediaPickerState()
    object Camera : MediaPickerState()
    object Gallery : MediaPickerState()
} 