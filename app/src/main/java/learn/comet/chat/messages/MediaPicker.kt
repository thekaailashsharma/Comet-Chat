package learn.comet.chat.messages

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import learn.comet.chat.utils.FileUtils

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaPicker(
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit,
    onVideoSelected: (Uri) -> Unit,
    onPdfSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                onImageSelected(uri)
            }
        } else {
            Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
        onDismiss()
    }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let(onImageSelected)
        onDismiss()
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let(onVideoSelected)
        onDismiss()
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let(onPdfSelected)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Media") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        when (cameraPermission.status) {
                            PermissionStatus.Granted -> {
                                try {
                                    val photoFile = FileUtils.createTempImageFile(context)
                                    currentPhotoUri = FileUtils.getUriForFile(context, photoFile)
                                    cameraLauncher.launch(currentPhotoUri!!)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Failed to create camera capture file: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    e.printStackTrace()
                                }
                            }
                            is PermissionStatus.Denied -> {
                                cameraPermission.launchPermissionRequest()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take Photo")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Image",
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Image",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    IconButton(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = "Video",
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Video",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    IconButton(
                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF",
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "PDF",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Handle permission denied
    LaunchedEffect(cameraPermission.status) {
        when (cameraPermission.status) {
            is PermissionStatus.Denied -> {
                val status = cameraPermission.status as PermissionStatus.Denied
                if (status.shouldShowRationale) {
                    Toast.makeText(
                        context,
                        "Camera permission is needed to take photos",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            else -> {}
        }
    }
}