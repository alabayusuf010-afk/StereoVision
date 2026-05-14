package com.example.stereovision

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.stereovision.ui.theme.StereoVisionTheme
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            StereoVisionTheme {
                var captureMode by remember { mutableStateOf("LEFT") } // LEFT or RIGHT

                Scaffold { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        CameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            onImageCaptureReady = { imageCapture = it }
                        )

                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Capture Mode: $captureMode", modifier = Modifier.padding(8.dp))
                            Row {
                                Button(
                                    onClick = {
                                        takePhoto(captureMode) {
                                            captureMode = if (captureMode == "LEFT") "RIGHT" else "LEFT"
                                        }
                                    },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text("CAPTURE $captureMode")
                                }
                                Button(onClick = { finish() }, modifier = Modifier.padding(8.dp)) {
                                    Text("Back")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun takePhoto(mode: String, onComplete: () -> Unit) {
        val imageCapture = imageCapture ?: return

        val name = if (mode == "LEFT") "I_L.jpg" else "I_R.jpg"
        val photoFile = File(filesDir, name)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CaptureActivity", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo saved: ${photoFile.absolutePath}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val preview = Preview.Builder().build()
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                preview.setSurfaceProvider(previewView.surfaceProvider)
                onImageCaptureReady(imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraPreview", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
