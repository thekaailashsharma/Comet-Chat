package learn.comet.mediaviewer.viewer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.rajat.pdfviewer.PdfRendererView
import com.rajat.pdfviewer.compose.PdfRendererViewCompose
import com.rajat.pdfviewer.util.PdfSource
import com.rajat.pdfviewer.HeaderData
import learn.comet.mediaviewer.viewer.components.ViewerFooter
import learn.comet.mediaviewer.viewer.components.ViewerHeader

@Composable
fun PdfViewer(
    data: MediaViewerData,
    onBackPressed: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var downloadProgress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var isZoomedIn by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableStateOf(1f) }

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
            val statusCallback = remember(currentPage, totalPages, isLoading, errorMessage, downloadProgress) {
                object : PdfRendererView.StatusCallBack {
                    override fun onPdfLoadStart() {
                        isLoading = true
                        errorMessage = null
                    }

                    override fun onPdfLoadProgress(progress: Int, downloadedBytes: Long, totalBytes: Long?) {
                        downloadProgress = progress
                        isLoading = progress < 100
                    }

                    override fun onPdfLoadSuccess(absolutePath: String) {
                        isLoading = false
                        errorMessage = null
                    }

                    override fun onError(error: Throwable) {
                        isLoading = false
                        errorMessage = error.message
                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    }

                    override fun onPageChanged(page: Int, total: Int) {
                        currentPage = page
                        totalPages = total
                    }
                }
            }

            val zoomListener = remember(isZoomedIn, zoomScale) {
                object : PdfRendererView.ZoomListener {
                    override fun onZoomChanged(zoomed: Boolean, scale: Float) {
                        isZoomedIn = zoomed
                        zoomScale = scale
                    }
                }
            }

            PdfRendererViewCompose(
                source = PdfSource.Remote(data.uri.toString()),
                lifecycleOwner = lifecycleOwner,
                modifier = Modifier.fillMaxSize(),
                headers = HeaderData(data.headers),
                statusCallBack = statusCallback,
                zoomListener = zoomListener
            )

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading PDF... $downloadProgress%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Error message
            errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading PDF",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBackPressed) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }

        ViewerFooter(timestamp = data.timestamp)
    }
} 