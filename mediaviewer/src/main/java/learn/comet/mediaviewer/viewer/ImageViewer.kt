package learn.comet.mediaviewer.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import learn.comet.mediaviewer.viewer.components.ViewerFooter
import learn.comet.mediaviewer.viewer.components.ViewerHeader
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun ImageViewer(
    data: MediaViewerData,
    onBackPressed: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        ViewerHeader(
            title = data.name,
            onBackPressed = onBackPressed,
            onShare = onShare
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val zoomState = rememberZoomState()
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(data.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = data.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .zoomable(zoomState)
            )
        }

        ViewerFooter(timestamp = data.timestamp)
    }
} 