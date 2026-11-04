package com.gikoko.aivideostudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showHelp by remember { mutableStateOf(false) }
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFFFC857), secondary = Color(0xFF00C389))) {
                Box(Modifier.fillMaxSize()) {
                    OfflineStudioApp()
                    FilledTonalButton(
                        onClick = { showHelp = true },
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp)
                    ) { Text("❔ Help") }
                    if (showHelp) {
                        Dialog(
                            onDismissRequest = { showHelp = false },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            HelpGuideContent(onClose = { showHelp = false })
                        }
                    }
                }
            }
        }
    }
}
