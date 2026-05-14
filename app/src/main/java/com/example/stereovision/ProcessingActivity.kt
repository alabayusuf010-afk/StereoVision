package com.example.stereovision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.stereovision.ui.theme.StereoVisionTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.calib3d.Calib3d
import org.opencv.calib3d.StereoBM
import org.opencv.core.CvType
import org.opencv.core.DMatch
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Size
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import java.io.File

class ProcessingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StereoVisionTheme {
                var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var statusText by remember { mutableStateOf("Ready to process") }
                var isProcessing by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(text = "Stereo Pipeline Processing")
                        Text(text = statusText, modifier = Modifier.padding(vertical = 8.dp))
                        
                        if (isProcessing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    resultBitmap = processStereo { statusText = it }
                                    isProcessing = false
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text("Run Pipeline (T3-T7)")
                        }

                        resultBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Processing Result",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(onClick = { finish() }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Back")
                        }
                    }
                }
            }
        }
    }

    private suspend fun processStereo(onUpdateStatus: (String) -> Unit): Bitmap? = withContext(Dispatchers.Default) {
        try {
            // T3: Load pair
            onUpdateStatus("T3: Loading images...")
            val fileL = File(filesDir, "I_L.jpg")
            val fileR = File(filesDir, "I_R.jpg")
            if (!fileL.exists() || !fileR.exists()) {
                onUpdateStatus("Error: Images I_L.jpg or I_R.jpg not found. Capture them first.")
                return@withContext null
            }

            var matL = StereoUtils.bitmapToMat(BitmapFactory.decodeFile(fileL.absolutePath))
            var matR = StereoUtils.bitmapToMat(BitmapFactory.decodeFile(fileR.absolutePath))
            
            // Convert to Grayscale for processing
            val grayL = Mat()
            val grayR = Mat()
            Imgproc.cvtColor(matL, grayL, Imgproc.COLOR_BGR2GRAY)
            Imgproc.cvtColor(matR, grayR, Imgproc.COLOR_BGR2GRAY)

            // T4: Undistort
            onUpdateStatus("T4: Undistorting (if calib.yml exists)...")
            val calibFile = File(filesDir, "calib.yml")
            val calib = StereoUtils.loadCalibration(calibFile)
            if (calib != null) {
                val (k, dist) = calib
                val undL = Mat()
                val undR = Mat()
                Calib3d.undistort(grayL, undL, k, dist)
                Calib3d.undistort(grayR, undR, k, dist)
                undL.copyTo(grayL)
                undR.copyTo(grayR)
            } else {
                Log.w("Processing", "calib.yml not found, skipping undistortion")
            }

            // T5: ORB and Matching
            onUpdateStatus("T5: Detecting features (ORB)...")
            val orb = ORB.create(1000)
            val keypointsL = MatOfKeyPoint()
            val keypointsR = MatOfKeyPoint()
            val descriptorsL = Mat()
            val descriptorsR = Mat()
            orb.detectAndCompute(grayL, Mat(), keypointsL, descriptorsL)
            orb.detectAndCompute(grayR, Mat(), keypointsR, descriptorsR)

            val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
            val matches = MatOfDMatch()
            matcher.match(descriptorsL, descriptorsR, matches)

            // Filter matches
            val matchesList = matches.toList()
            val goodMatches = matchesList.sortedBy { it.distance }.take(matchesList.size / 4)
            
            // T6: Fundamental Matrix F
            onUpdateStatus("T6: Estimating F Matrix...")
            val ptsL = mutableListOf<org.opencv.core.Point>()
            val ptsR = mutableListOf<org.opencv.core.Point>()
            val kpL = keypointsL.toList()
            val kpR = keypointsR.toList()
            for (m in goodMatches) {
                ptsL.add(kpL[m.queryIdx].pt)
                ptsR.add(kpR[m.trainIdx].pt)
            }
            
            val mpL = MatOfPoint2f()
            val mpR = MatOfPoint2f()
            mpL.fromList(ptsL)
            mpR.fromList(ptsR)

            val fMatrix = Calib3d.findFundamentalMat(mpL, mpR, Calib3d.FM_RANSAC, 3.0, 0.99)

            // T7: Rectification and Disparity
            onUpdateStatus("T7: Generating Disparity Map...")
            val h1 = Mat()
            val h2 = Mat()
            Calib3d.stereoRectifyUncalibrated(mpL, mpR, fMatrix, grayL.size(), h1, h2)
            
            val rectL = Mat()
            val rectR = Mat()
            Imgproc.warpPerspective(grayL, rectL, h1, grayL.size())
            Imgproc.warpPerspective(grayR, rectR, h2, grayR.size())

            val stereoBM = StereoBM.create(16, 15)
            val disparity = Mat()
            stereoBM.compute(rectL, rectR, disparity)
            
            // Normalize disparity for visualization
            val disp8 = Mat()
            disparity.convertTo(disp8, CvType.CV_8U, 255.0 / 16.0)
            
            // Save disparity for T8
            val dispFile = File(filesDir, "disparity.png")
            org.opencv.imgcodecs.Imgcodecs.imwrite(dispFile.absolutePath, disparity)

            onUpdateStatus("Done! Disparity map generated.")
            return@withContext StereoUtils.matToBitmap(disp8)

        } catch (e: Exception) {
            Log.e("Processing", "Pipeline failed", e)
            onUpdateStatus("Error: ${e.message}")
            return@withContext null
        }
    }
}
