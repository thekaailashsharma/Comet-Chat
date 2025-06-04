package learn.comet.mediaviewer.viewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MediaViewer(
    data: MediaViewerData,
    onBackPressed: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (data.type) {
        MediaType.PDF -> {
            PdfViewer(
                data = data,
                onBackPressed = onBackPressed,
                onShare = onShare,
                modifier = modifier
            )
        }
        MediaType.IMAGE -> {
            ImageViewer(
                data = data,
                onBackPressed = onBackPressed,
                onShare = onShare,
                modifier = modifier
            )
        }
    }
} 