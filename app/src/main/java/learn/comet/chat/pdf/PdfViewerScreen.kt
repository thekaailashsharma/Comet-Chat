package learn.comet.chat.pdf

import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.rajat.pdfviewer.PdfViewerActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uri: Uri,
    onBackPressed: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(uri) {
        println("URLLLLLL is $uri")
        PdfViewerActivity.launchPdfFromUrl(
            context = context,
            pdfUrl = uri.toString(),
            pdfTitle = "PDF Document",
            enableDownload = true,
            saveTo = com.rajat.pdfviewer.util.saveTo.DOWNLOADS
        )
        onBackPressed()
    }

    // Show loading UI while launching PDF viewer
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
        )
    }
} 