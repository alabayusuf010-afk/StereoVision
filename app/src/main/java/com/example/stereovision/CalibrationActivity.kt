package com.example.stereovision

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import org.opencv.calib3d.Calib3d
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfPoint3f
import org.opencv.core.Point3
import org.opencv.core.Size
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CalibrationActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private val boardSize = Size(9.0, 6.0)
    private val imagePoints = mutableListOf<Mat>()
    private val objectPoints = mutableListOf<Mat>()
    
    private var lastDetectedCorners: MatOfPoint2f? = null
    private var imageSize: Size? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            StereoVisionTheme {
                var posesCount by remember { mutableStateOf(0) }
                var cornersDetected by remember { mutableStateOf(false) }

                Scaffold { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        CalibrationPreview(
                            modifier = Modifier.fillMaxSize(),
                            onCornersDetected = { detected, points, size ->
                                cornersDetected = detected
                                if (detected && points != null) {
                                    lastDetectedCorners = points
                                    imageSize = size
                                }
                            }
                        )

                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = if (cornersDetected) "Board Found!" else "Searching for board...")
                            Text(text = "Poses collected: $posesCount")
                            Button(
                                onClick = { 
                                    if (cornersDetected && lastDetectedCorners != null) {
                                        addPose()
                                        posesCount = imagePoints.size
                                    } else {
                                        Toast.makeText(this@CalibrationActivity, "No board detected!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text("Add Pose")
                            }
                            Button(
                                onClick = { calibrateAndSave() },
                                enabled = posesCount >= 5,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text("Calibrate (Min 10 recommended)")
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

    private fun addPose() {
        val corners = MatOfPoint2f()
        lastDetectedCorners?.copyTo(corners)
        imagePoints.add(corners)

        val obj = MatOfPoint3f()
        val points = mutableListOf<Point3>()
        for (i in 0 until boardSize.height.toInt()) {
            for (j in 0 until boardSize.width.toInt()) {
                points.add(Point3(j.toDouble(), i.toDouble(), 0.0))
            }
        }
        obj.fromList(points)
        objectPoints.add(obj)
        Toast.makeText(this, "Pose added!", Toast.LENGTH_SHORT).show()
    }

    private fun calibrateAndSave() {
        val size = imageSize ?: return
        val cameraMatrix = Mat.eye(3, 3, CvType.CV_64F)
        val distCoeffs = Mat.zeros(8, 1, CvType.CV_64F)
        val rvecs = mutableListOf<Mat>()
        val tvecs = mutableListOf<Mat>()

        val rms = Calib3d.calibrateCamera(
            objectPoints, imagePoints, size, cameraMatrix, distCoeffs, rvecs, tvecs
        )

        Log.d("Calibration", "RMS error: $rms")
        val file = File(filesDir, "calib.yml")
        StereoUtils.saveCalibration(file, cameraMatrix, distCoeffs)
        
        Toast.makeText(this, "Calibration Saved! RMS: ${String.format("%.4f", rms)}", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun CalibrationPreview(
    modifier: Modifier = Modifier,
    onCornersDetected: (Boolean, MatOfPoint2f?, Size) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val preview = Preview.Builder().build()
    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
    
    val boardSize = Size(9.0, 6.0)

    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
        val yBuffer = imageProxy.planes[0].buffer
        val data = ByteArray(yBuffer.remaining())
        yBuffer.get(data)
        
        val mat = Mat(imageProxy.height, imageProxy.width, CvType.CV_8UC1)
        mat.put(0, 0, data)
        
        val corners = MatOfPoint2f()
        val found = Calib3d.findChessboardCorners(mat, boardSize, corners)
        
        onCornersDetected(found, if (found) corners else null, Size(mat.cols().toDouble(), mat.rows().toDouble()))
        
        imageProxy.close()
    }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                preview.setSurfaceProvider(previewView.surfaceProvider)
            } catch (exc: Exception) {
                Log.e("CalibrationPreview", "Binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
