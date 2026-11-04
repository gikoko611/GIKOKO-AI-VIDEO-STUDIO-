package com.gikoko.aivideostudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudioHome() }
    }
}

@Composable
private fun StudioHome() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("G.I KO KO", style = MaterialTheme.typography.headlineLarge)
                Text("AI VIDEO STUDIO", style = MaterialTheme.typography.titleLarge)
                Text("Hybrid AI video creator • ARMv7 ready")
                Button(onClick = { /* Project creation UI will be added next. */ }) {
                    Text("Create New Video")
                }
                Text("Powered by I KO KO", modifier = Modifier.padding(top = 24.dp))
            }
        }
    }
}
