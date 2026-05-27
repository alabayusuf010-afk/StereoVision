package com.example.stereovision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stereovision.ui.theme.StereoVisionTheme

class AiPipelineActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StereoVisionTheme {
                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "AI Pipeline",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "This module will handle the AI-based Stereo Vision pipeline.",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Button(
                            onClick = { finish() },
                            modifier = Modifier.padding(top = 32.dp)
                        ) {
                            Text("Back to Main Menu")
                        }
                    }
                }
            }
        }
    }
}
