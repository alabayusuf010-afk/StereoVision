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
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.DMatch
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.Features2d
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import java.io.File

class ProcessingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StereoVisionTheme {
                var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var matchesBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var rectifiedBitmap by remember { mutableStateOf<Bitmap?>(null) }
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
                                    matchesBitmap = null
                                    rectifiedBitmap = null
                                    resultBitmap = null
                                    resultBitmap = processStereo(
                                        onUpdateStatus = { statusText = it },
                                        onUpdateMatches = { matchesBitmap = it },
                                        onUpdateRectified = { rectifiedBitmap = it }
                                    )
                                    isProcessing = false
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text("Run Pipeline (T3-T7)")
                        }

                        matchesBitmap?.let {
                            Text("T5: ORB Matches", modifier = Modifier.padding(top = 8.dp))
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "ORB Matches",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        rectifiedBitmap?.let {
                            Text("T6: Rectified Pair + Epipolar Lines", modifier = Modifier.padding(top = 8.dp))
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Rectified Pair",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        resultBitmap?.let {
                            Text("T7: Disparity Map", modifier = Modifier.padding(top = 8.dp))
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

    private suspend fun processStereo(
        onUpdateStatus: (String) -> Unit,
        onUpdateMatches: (Bitmap) -> Unit,
        onUpdateRectified: (Bitmap) -> Unit
    ): Bitmap? = withContext(Dispatchers.Default) {
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
            val orb = ORB.create(2000) // Increased features
            val keypointsL = MatOfKeyPoint()
            val keypointsR = MatOfKeyPoint()
            val descriptorsL = Mat()
            val descriptorsR = Mat()
            orb.detectAndCompute(grayL, Mat(), keypointsL, descriptorsL)
            orb.detectAndCompute(grayR, Mat(), keypointsR, descriptorsR)

            if (descriptorsL.empty() || descriptorsR.empty()) {
                onUpdateStatus("Error: No ORB features found. Ensure images have texture and light.")
                return@withContext null
            }

            val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
            val matches = MatOfDMatch()
            matcher.match(descriptorsL, descriptorsR, matches)

            // Filter matches
            val matchesList = matches.toList()
            val goodMatches = matchesList.sortedBy { it.distance }.take(200.coerceAtMost(matchesList.size))
            
            // T6: Fundamental Matrix F with RANSAC
            onUpdateStatus("T6: Estimating F Matrix (RANSAC)...")
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

            val mask = Mat()
            val fMatrix = Calib3d.findFundamentalMat(mpL, mpR, Calib3d.FM_RANSAC, 1.0, 0.99, mask)

            // Filter matches to only show and use RANSAC inliers
            val inlierPtsL = mutableListOf<org.opencv.core.Point>()
            val inlierPtsR = mutableListOf<org.opencv.core.Point>()
            val inlierMatches = mutableListOf<DMatch>()
            val maskData = ByteArray(mask.rows() * mask.cols())
            mask.get(0, 0, maskData)
            
            for (i in 0 until goodMatches.size) {
                if (maskData[i].toInt() != 0) {
                    inlierMatches.add(goodMatches[i])
                    inlierPtsL.add(ptsL[i])
                    inlierPtsR.add(ptsR[i])
                }
            }

            if (inlierMatches.size < 10) {
                onUpdateStatus("Error: Too few stable matches (${inlierMatches.size}). Try images with more detail.")
                return@withContext null
            }

            // Draw ONLY the inliers for T5
            val matchesImg = Mat()
            val inlierMatchesMat = MatOfDMatch()
            inlierMatchesMat.fromList(inlierMatches)
            Features2d.drawMatches(grayL, keypointsL, grayR, keypointsR, inlierMatchesMat, matchesImg)
            onUpdateMatches(StereoUtils.matToBitmap(matchesImg))

            // T7: Rectification and Disparity
            onUpdateStatus("T7: Generating Disparity Map...")
            
            val poseFile = File(filesDir, "pose.txt")
            var rectL = Mat()
            var rectR = Mat()

            if (poseFile.exists() && calib != null) {
                onUpdateStatus("T7: Rectifying using IMU Pose (VIO)...")
                val (k, dist) = calib
                val poseData = poseFile.readText().split(",").map { it.toFloat() }
                
                // Translation vector t
                val t = Mat(3, 1, CvType.CV_64F)
                t.put(0, 0, poseData[0].toDouble())
                t.put(1, 0, poseData[1].toDouble())
                t.put(2, 0, poseData[2].toDouble())
                
                // Rotation matrix R
                val R = StereoUtils.eulerToRotationMatrix(poseData[3], poseData[4], poseData[5])
                
                val baseline = kotlin.math.sqrt(poseData[0]*poseData[0] + poseData[1]*poseData[1] + poseData[2]*poseData[2])
                onUpdateStatus("T7: IMU Baseline: ${"%.2f".format(baseline)}m. Rectifying...")

                val rectified = StereoUtils.rectifyWithIMU(grayL, grayR, k, dist, R, t)
                rectL = rectified.first
                rectR = rectified.second
            } else {
                onUpdateStatus("T7: IMU Pose not found, falling back to Uncalibrated Rectification...")
                val mInlierL = MatOfPoint2f()
                val mInlierR = MatOfPoint2f()
                mInlierL.fromList(inlierPtsL)
                mInlierR.fromList(inlierPtsR)

                val h1 = Mat()
                val h2 = Mat()
                Calib3d.stereoRectifyUncalibrated(mInlierL, mInlierR, fMatrix, grayL.size(), h1, h2)
                
                Imgproc.warpPerspective(grayL, rectL, h1, grayL.size())
                Imgproc.warpPerspective(grayR, rectR, h2, grayR.size())
            }

            // Visualize Rectified Pair with Epipolar Lines
            val combinedRect = Mat(rectL.rows(), rectL.cols() * 2, CvType.CV_8U)
            val leftRectSub = combinedRect.submat(0, rectL.rows(), 0, rectL.cols())
            val rightRectSub = combinedRect.submat(0, rectL.rows(), rectL.cols(), rectL.cols() * 2)
            rectL.copyTo(leftRectSub)
            rectR.copyTo(rightRectSub)

            // Convert to color to draw green lines
            val colorCombined = Mat()
            Imgproc.cvtColor(combinedRect, colorCombined, Imgproc.COLOR_GRAY2BGR)

            val step = colorCombined.rows() / 15
            for (i in 1..14) {
                val y = (i * step).toDouble()
                Imgproc.line(colorCombined, org.opencv.core.Point(0.0, y), org.opencv.core.Point(colorCombined.cols().toDouble(), y), Scalar(0.0, 255.0, 0.0), 1)
            }
            onUpdateRectified(StereoUtils.matToBitmap(colorCombined))

            val stereoBM = StereoBM.create(64, 21)
            val disparity = Mat()
            stereoBM.compute(rectL, rectR, disparity)
            
            val disp8 = Mat()
            Core.normalize(disparity, disp8, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)
            
            val dispFile = File(filesDir, "disparity.png")
            org.opencv.imgcodecs.Imgcodecs.imwrite(dispFile.absolutePath, disparity)

            onUpdateStatus("Done! Inliers: ${inlierMatches.size}")
            return@withContext StereoUtils.matToBitmap(disp8)

        } catch (e: Exception) {
            Log.e("Processing", "Pipeline failed", e)
            onUpdateStatus("Error: ${e.message}")
            return@withContext null
        }
    }
}
