package com.gikoko.aivideostudio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

private val Gold = Color(0xFFFFC857)
private val Emerald = Color(0xFF00C389)
private val Ink = Color(0xFF101713)
private val Panel = Color(0xFF1B2721)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudioApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("studio_settings", 0) }
    val scope = rememberCoroutineScope()
    val runTask: (suspend () -> Any, (Any?, String?) -> Unit) -> Unit = { task, callback ->
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { task() }
                callback(result, null)
            } catch (error: Exception) {
                callback(null, error.message ?: "Unknown error")
            }
        }
    }

    var baseUrl by remember { mutableStateOf(prefs.getString("base_url", "https://api.openai.com/v1") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("model", "gpt-4o-mini") ?: "") }
    var apiKey by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("Create an engaging short video about an amazing fact from Myanmar history.") }
    var script by remember { mutableStateOf("") }
    var subtitles by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready. Configure an AI provider to begin.") }
    var busy by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var discoveredModels by remember { mutableStateOf(listOf<String>()) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
            if (result == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            mediaUri = uri
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            status = "Media selected: ${uri.lastPathSegment ?: "file"}"
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Gold, secondary = Emerald, background = Ink, surface = Panel,
        onPrimary = Ink, onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(containerColor = Ink, topBar = {
            Column(Modifier.fillMaxWidth().background(Panel).padding(16.dp)) {
                Text("G.I KO KO", color = Gold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("AI VIDEO STUDIO  •  v1.0.0 development", color = Emerald, style = MaterialTheme.typography.labelMedium)
            }
        }, bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Create", "Media", "Voice", "Settings").forEachIndexed { index, label ->
                    NavigationBarItem(selected = selectedTab == index, onClick = { selectedTab = index },
                        icon = { Text(listOf("✦", "▣", "♫", "⚙")[index]) }, label = { Text(label) })
                }
            }
        }) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (selectedTab) {
                    0 -> {
                        Text("AI SCRIPT GENERATOR", color = Gold, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Video idea / prompt") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                        Button(onClick = {
                            if (apiKey.isBlank()) status = "Add your API key in Settings first."
                            else if (model.isBlank()) status = "Enter a model ID first."
                            else {
                                busy = true; status = "Generating script…"
                                runTask({ callChat(baseUrl, apiKey, model, "Write a concise, engaging video script with scene directions and narration. Return plain text. User idea: $prompt") }) { result, error ->
                                    busy = false
                                    if (error != null) status = "Generation failed: $error" else {
                                        script = result as? String ?: ""
                                        subtitles = script.lines().filter { it.isNotBlank() }.joinToString("\n")
                                        status = "Script generated. Review and edit before production."
                                    }
                                }
                            }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Working…" else "Generate Video Script") }
                        Text("GENERATED SCRIPT", color = Gold, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = script, onValueChange = { script = it }, modifier = Modifier.fillMaxWidth(), minLines = 7)
                        OutlinedTextField(value = subtitles, onValueChange = { subtitles = it }, label = { Text("Subtitle text (editable)") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                        OutlinedButton(onClick = { subtitles = script.lines().filter { it.isNotBlank() }.joinToString("\n"); status = "Subtitle draft refreshed from script." }, modifier = Modifier.fillMaxWidth()) { Text("Create subtitle draft from script") }
                    }
                    1 -> {
                        Text("MEDIA LIBRARY", color = Gold, fontWeight = FontWeight.Bold)
                        Text("Choose a video, audio, or image file from your device. The selected file remains on-device; cloud upload is not connected in this build.")
                        Button(onClick = { mediaPicker.launch(arrayOf("video/*", "audio/*", "image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Import media") }
                        Text(mediaUri?.toString() ?: "No media selected", color = Emerald)
                        Text("Use media you own or are licensed to use.")
                    }
                    2 -> {
                        Text("VOICE & NARRATION", color = Gold, fontWeight = FontWeight.Bold)
                        Text("Android system Text-to-Speech. English voice availability depends on device language packs.")
                        Button(onClick = {
                            if (script.isBlank()) status = "Generate or enter a script first."
                            else if (ttsReady) { tts?.speak(script.take(3500), TextToSpeech.QUEUE_FLUSH, null, "gikoko-preview"); status = "Speaking script preview." }
                            else status = "System TTS is not ready. Check Android Text-to-Speech settings."
                        }, modifier = Modifier.fillMaxWidth()) { Text("Play voice preview") }
                        OutlinedButton(onClick = { tts?.stop(); status = "Voice stopped." }, modifier = Modifier.fillMaxWidth()) { Text("Stop voice") }
                        Text("This is device TTS preview, not MP3 export.")
                    }
                    else -> {
                        Text("AI PROVIDER SETTINGS", color = Gold, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key (session only)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(onClick = {
                            prefs.edit().putString("base_url", baseUrl.trim()).putString("model", model.trim()).apply()
                            status = "Provider URL and model saved. API key remains in memory only."
                        }, modifier = Modifier.fillMaxWidth()) { Text("Save provider settings") }
                        OutlinedButton(onClick = {
                            if (apiKey.isBlank()) status = "Enter API key first."
                            else { busy = true; status = "Testing provider…"; runTask({ fetchModels(baseUrl, apiKey).joinToString(", ").ifBlank { "Connected; provider returned no model list." } }) { result, error -> busy = false; status = error?.let { "Connection failed: $it" } ?: (result as? String ?: "Connected") } }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Test connection") }
                        OutlinedButton(onClick = {
                            if (apiKey.isBlank()) status = "Enter API key first."
                            else { busy = true; status = "Discovering models…"; runTask({ fetchModels(baseUrl, apiKey) }) { result, error ->
                                busy = false
                                if (error != null) status = "Model discovery failed: $error"
                                else { discoveredModels = (result as? List<*>)?.filterIsInstance<String>() ?: emptyList(); status = "Found ${discoveredModels.size} models." }
                            } }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Discover models") }
                        discoveredModels.take(30).forEach { item ->
                            TextButton(onClick = { model = item; status = "Selected model: $item" }) { Text(item, color = Emerald) }
                        }
                        Text("API key is not saved to disk. Free model availability and quotas depend on your provider.", color = Gold, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider(color = Emerald.copy(alpha = 0.35f))
                Text("STATUS", color = Gold, fontWeight = FontWeight.Bold)
                Text(status, color = Color.LightGray)
                Text("Powered by I KO KO", color = Emerald, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun callChat(base: String, key: String, model: String, userPrompt: String): String {
    val endpoint = base.trimEnd('/') + "/chat/completions"
    val body = JSONObject().put("model", model).put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", userPrompt))).put("temperature", 0.7)
    val response = requestJson(endpoint, key, "POST", body.toString())
    return response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
        .ifBlank { throw IllegalStateException("Provider returned an empty response") }
}

private fun fetchModels(base: String, key: String): List<String> {
    val response = requestJson(base.trimEnd('/') + "/models", key, "GET", null)
    val data = response.optJSONArray("data") ?: return emptyList()
    return (0 until data.length()).mapNotNull { data.optJSONObject(it)?.optString("id")?.takeIf(String::isNotBlank) }.sorted()
}

private fun requestJson(endpoint: String, key: String, method: String, body: String?): JSONObject {
    require(endpoint.startsWith("https://")) { "Use an HTTPS provider URL." }
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = method
        connection.connectTimeout = 20000
        connection.readTimeout = 45000
        connection.setRequestProperty("Authorization", "Bearer $key")
        connection.setRequestProperty("Accept", "application/json")
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw IllegalStateException("HTTP $code: ${text.take(500)}")
        return JSONObject(text)
    } finally { connection.disconnect() }
}
