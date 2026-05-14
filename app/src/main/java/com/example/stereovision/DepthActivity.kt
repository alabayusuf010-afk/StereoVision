package com.example.stereovision

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stereovision.ui.theme.StereoVisionTheme
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import java.io.File

class DepthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StereoVisionTheme {
                var baseline by remember { mutableStateOf("0.1") } // meters
                var statusText by remember { mutableStateOf("Ready to calculate depth") }

                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(text = "T8: Depth Calculation & PLY Export")
                        
                        TextField(
                            value = baseline,
                            onValueChange = { baseline = it },
                            label = { Text("Baseline (meters)") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )

                        Button(
                            onClick = { calculateAndExport(baseline.toDoubleOrNull() ?: 0.1) { statusText = it } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Generate Point Cloud (.ply)")
                        }

                        Text(text = statusText, modifier = Modifier.padding(top = 16.dp))

                        Button(onClick = { finish() }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Back")
                        }
                    }
                }
            }
        }
    }

    private fun calculateAndExport(b: Double, onUpdateStatus: (String) -> Unit) {
        try {
            val dispFile = File(filesDir, "disparity.png")
            if (!dispFile.exists()) {
                onUpdateStatus("Error: Disparity map not found. Run Processing first.")
                return
            }

            val disparity = Imgcodecs.imread(dispFile.absolutePath, Imgcodecs.IMREAD_UNCHANGED)
            val calibFile = File(filesDir, "calib.yml")
            val calib = StereoUtils.loadCalibration(calibFile)
            
            val f = if (calib != null) calib.first.get(0, 0)[0] else 1000.0 // focal length in pixels
            
            val plyFile = File(getExternalFilesDir(null), "cloud.ply")
            val writer = plyFile.bufferedWriter()
            
            // PLY Header
            val points = mutableListOf<String>()
            var count = 0
            
            for (r in 0 until disparity.rows()) {
                for (c in 0 until disparity.cols()) {
                    val d = disparity.get(r, c)[0] / 16.0 // divide by 16 if using StereoBM
                    if (d <= 0) continue
                    
                    val z = (f * b) / d
                    val x = (c - disparity.cols()/2.0) * z / f
                    val y = (r - disparity.rows()/2.0) * z / f
                    
                    points.add("$x $y $z")
                    count++
                }
            }
            
            writer.write("ply\n")
            writer.write("format ascii 1.0\n")
            writer.write("element vertex $count\n")
            writer.write("property float x\n")
            writer.write("property float y\n")
            writer.write("property float z\n")
            writer.write("end_header\n")
            for (p in points) {
                writer.write("$p\n")
            }
            writer.close()
            
            onUpdateStatus("Exported $count points to:\n${plyFile.absolutePath}")
            Toast.makeText(this, "Success! File saved.", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e("Depth", "Export failed", e)
            onUpdateStatus("Error: ${e.message}")
        }
    }
}
