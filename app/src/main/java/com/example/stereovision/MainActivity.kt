package com.example.stereovision

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.stereovision.ui.theme.StereoVisionTheme
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                Log.d("MainActivity", "All permissions granted")
            } else {
                Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Load OpenCV
        if (OpenCVLoader.initLocal()) {
            Log.d("MainActivity", "OpenCV loaded successfully")
        } else {
            Log.e("MainActivity", "OpenCV load failed")
        }

        checkPermissions()

        setContent {
            StereoVisionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainMenu(
                        onNavigate = { activityClass ->
                            startActivity(Intent(this, activityClass))
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            requestPermissionLauncher.launch(permissions)
        }
    }
}

@Composable
fun MainMenu(onNavigate: (Class<*>) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "StereoVIO App",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Visual-Inertial Odometry Mode",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        MenuButton("1. Calibration", onClick = { onNavigate(CalibrationActivity::class.java) })
        MenuButton("2. Capture", onClick = { onNavigate(CaptureActivity::class.java) })
        MenuButton("3. Processing", onClick = { onNavigate(ProcessingActivity::class.java) })
        MenuButton("4. Depth / Results", onClick = { onNavigate(DepthActivity::class.java) })
        MenuButton("5. AI Pipeline", onClick = { onNavigate(AiPipelineActivity::class.java) })
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = text)
    }
}
