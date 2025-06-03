package learn.comet.chat.messages

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
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
    state: MediaPickerState,
    onMediaSelected: (Uri, MediaType) -> Unit,
    onDismiss: () -> Unit,
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
                onMediaSelected(uri, MediaType.IMAGE)
            }
        } else {
            Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
        onDismiss()
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            Toast.makeText(context, "No media selected", Toast.LENGTH_SHORT).show()
        } else {
            val type = when {
                uri.toString().contains("image") -> MediaType.IMAGE
                uri.toString().contains("video") -> MediaType.VIDEO
                else -> null
            }
            if (type != null) {
                onMediaSelected(uri, type)
            } else {
                Toast.makeText(context, "Unsupported media type", Toast.LENGTH_SHORT).show()
            }
        }
        onDismiss()
    }

    if (state != MediaPickerState.Hidden) {
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

                    Button(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose from Gallery")
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
    }

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