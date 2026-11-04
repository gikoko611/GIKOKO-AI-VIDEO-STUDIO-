package com.gikoko.aivideostudio

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudioRoot() }
    }
}

@Composable
private fun StudioRoot() {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2400)
        showSplash = false
    }
    if (showSplash) SplashScreen() else StudioHome()
}

@Composable
private fun SplashScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF050713)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.giko_logo),
                contentDescription = "G.I KO KO AI VIDEO STUDIO",
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentScale = ContentScale.Fit
            )
            Text(
                "Powered by အိုင်ကိုကို",
                color = Color(0xFFE6D7FF),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 18.dp)
            )
        }
    }
}

@Composable
private fun StudioHome() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("studio_settings", Context.MODE_PRIVATE) }
    var engine by remember { mutableStateOf(prefs.getString("render_engine", "github") ?: "github") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.giko_logo),
                    contentDescription = "G.I KO KO logo",
                    modifier = Modifier.size(150.dp),
                    contentScale = ContentScale.Fit
                )
                Text("G.I KO KO AI VIDEO STUDIO", style = MaterialTheme.typography.titleLarge)
                Text("Hybrid Cloud Video Creator • Android 10 / ARMv7")
                Text("SELECT RENDER ENGINE", style = MaterialTheme.typography.titleMedium)

                EngineCard(
                    title = "GitHub Actions Cloud",
                    description = "Automatic CPU + FFmpeg rendering. Start a cloud render workflow and download the MP4 artifact.",
                    selected = engine == "github",
                    onSelect = {
                        engine = "github"
                        prefs.edit().putString("render_engine", engine).apply()
                    }
                )
                EngineCard(
                    title = "Google Colab T4",
                    description = "GPU rendering when a free T4 is assigned. Start the Colab notebook manually; GPU and session time are not guaranteed.",
                    selected = engine == "colab",
                    onSelect = {
                        engine = "colab"
                        prefs.edit().putString("render_engine", engine).apply()
                    }
                )
                Text(
                    "Selected: " + if (engine == "github") "GitHub Actions" else "Google Colab T4",
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = { /* Project editor and job submission are the next implementation phase. */ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Create New Video")
                }
                Text("Cloud render setup is included in the repository. The app-to-cloud job connection is still being implemented.")
                Text("Powered by အိုင်ကိုကို", modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun EngineCard(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
