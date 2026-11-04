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
    var githubToken by remember { mutableStateOf("") }
    var sourceVideoUrl by remember { mutableStateOf("") }
    var voiceoverUrl by remember { mutableStateOf("") }
    var subtitleUrl by remember { mutableStateOf("") }
    var trimStart by remember { mutableStateOf("0") }
    var trimEnd by remember { mutableStateOf("") }
    var aspectRatio by remember { mutableStateOf("9:16") }
    var renderRunUrl by remember { mutableStateOf("") }
    var renderRunStatus by remember { mutableStateOf("No render job checked yet.") }
    var renderBusy by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
            if (result == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }

    val subtitleExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(buildSrt(subtitles)) }
                status = "SRT subtitle file exported."
            } catch (error: Exception) { status = "SRT export failed: ${error.message}" }
        }
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
                listOf("Create", "Media", "Voice", "Render", "Settings").forEachIndexed { index, label ->
                    NavigationBarItem(selected = selectedTab == index, onClick = { selectedTab = index },
                        icon = { Text(listOf("✦", "▣", "♫", "⇧", "⚙")[index]) }, label = { Text(label) })
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
                        OutlinedButton(onClick = { if (subtitles.isBlank()) status = "Enter subtitle text first." else subtitleExporter.launch("GIKOKO_subtitles.srt") }, modifier = Modifier.fillMaxWidth()) { Text("Export timed SRT subtitles") }
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
                    3 -> {
                        Text("CLOUD VIDEO RENDER", color = Gold, fontWeight = FontWeight.Bold)
                        Text("Send a publicly downloadable MP4 URL to the GitHub Actions FFmpeg renderer. A phone-only local file cannot be reached by GitHub until uploaded to a file host that provides a direct download URL.", color = Color.LightGray)
                        OutlinedTextField(value = sourceVideoUrl, onValueChange = { sourceVideoUrl = it }, label = { Text("Direct MP4 download URL (HTTPS)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        OutlinedTextField(value = voiceoverUrl, onValueChange = { voiceoverUrl = it }, label = { Text("Optional voiceover MP3/WAV URL") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        OutlinedTextField(value = subtitleUrl, onValueChange = { subtitleUrl = it }, label = { Text("Optional SRT subtitle URL") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        OutlinedTextField(value = trimStart, onValueChange = { trimStart = it }, label = { Text("Trim start (seconds, default 0)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = trimEnd, onValueChange = { trimEnd = it }, label = { Text("Trim end (seconds, optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = githubToken, onValueChange = { githubToken = it }, label = { Text("GitHub token — session only") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = aspectRatio, onValueChange = { aspectRatio = it }, label = { Text("Aspect ratio: 9:16 / 16:9 / 1:1") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(onClick = {
                            if (githubToken.isBlank()) status = "Enter a GitHub token with Actions read/write permission."
                            else if (!sourceVideoUrl.startsWith("https://")) status = "Enter a direct HTTPS MP4 download URL."
                            else if (aspectRatio !in listOf("9:16", "16:9", "1:1")) status = "Aspect ratio must be 9:16, 16:9, or 1:1."
                            else {
                                renderBusy = true; status = "Submitting GitHub render job…"
                                runTask({ dispatchCloudRender(githubToken, sourceVideoUrl, aspectRatio, voiceoverUrl, subtitleUrl, trimStart, trimEnd) }) { _, error ->
                                    renderBusy = false
                                    status = error?.let { "Cloud render dispatch failed: $it" } ?: "Render job submitted. Tap Check latest job to view progress."
                                }
                            }
                        }, enabled = !renderBusy, modifier = Modifier.fillMaxWidth()) { Text(if (renderBusy) "Submitting…" else "Start Cloud Render") }
                        OutlinedButton(onClick = {
                            if (githubToken.isBlank()) status = "Enter GitHub token first."
                            else {
                                renderBusy = true; status = "Checking latest GitHub Actions job…"
                                runTask({ latestCloudRun(githubToken) }) { result, error ->
                                    renderBusy = false
                                    if (error != null) status = "Job check failed: $error"
                                    else {
                                        val run = result as? JSONObject
                                        renderRunStatus = "Status: " + run?.optString("status") + " | Conclusion: " + run?.optString("conclusion").ifNullOrEmpty("pending")
                                        renderRunUrl = run?.optString("html_url").orEmpty()
                                        status = renderRunStatus
                                    }
                                }
                            }
                        }, enabled = !renderBusy, modifier = Modifier.fillMaxWidth()) { Text("Check latest render job") }
                        Text(renderRunStatus, color = Emerald)
                        if (renderRunUrl.isNotBlank()) {
                            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(renderRunUrl))) }, modifier = Modifier.fillMaxWidth()) { Text("Open GitHub job / download MP4 artifact") }
                        }
                        Text("Use a fine-grained GitHub token limited to this repository with Actions: Read and write and Metadata: Read. The token stays in memory only; never share it.", color = Gold, style = MaterialTheme.typography.bodySmall)
                        Text("GitHub Actions renders with CPU FFmpeg. Free minutes and artifact retention limits apply; GPU is not guaranteed.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
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


private fun String?.ifNullOrEmpty(fallback: String): String =
    if (this.isNullOrBlank()) fallback else this

private fun dispatchCloudRender(token: String, videoUrl: String, ratio: String, voiceoverUrl: String, subtitleUrl: String, trimStart: String, trimEnd: String): String {
    require(token.isNotBlank()) { "GitHub token is required." }
    require(videoUrl.startsWith("https://")) { "Only HTTPS source URLs are accepted." }
    require(ratio in listOf("9:16", "16:9", "1:1")) { "Unsupported aspect ratio." }
    val body = JSONObject()
        .put("ref", "main")
        .put("inputs", JSONObject()
            .put("video_url", videoUrl)
            .put("aspect_ratio", ratio)
            .put("watermark", "Powered by I KO KO")
            .put("voiceover_url", voiceoverUrl.trim())
            .put("subtitle_url", subtitleUrl.trim())
            .put("trim_start", trimStart.trim().ifBlank { "0" })
            .put("trim_end", trimEnd.trim()))
    githubRequest(token, "/repos/gikoko611/GIKOKO-AI-VIDEO-STUDIO-/actions/workflows/cloud-video-render.yml/dispatches", "POST", body.toString())
    return "Workflow dispatch accepted."
}

private fun latestCloudRun(token: String): JSONObject {
    val response = githubRequest(
        token,
        "/repos/gikoko611/GIKOKO-AI-VIDEO-STUDIO-/actions/workflows/cloud-video-render.yml/runs?branch=main&per_page=1",
        "GET",
        null
    )
    val runs = response.optJSONArray("workflow_runs")
    if (runs == null || runs.length() == 0) throw IllegalStateException("No cloud render runs found yet. Wait a few seconds after dispatch.")
    return runs.getJSONObject(0)
}

private fun githubRequest(token: String, path: String, method: String, body: String?): JSONObject {
    val connection = URL("https://api.github.com$path").openConnection() as HttpURLConnection
    try {
        connection.requestMethod = method
        connection.connectTimeout = 20000
        connection.readTimeout = 45000
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw IllegalStateException("GitHub HTTP " + code + ": " + responseText.take(500))
        return if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
    } finally { connection.disconnect() }
}

private fun buildSrt(text: String): String {
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    var cursorMs = 0L
    return lines.mapIndexed { index, line ->
        val duration = ((line.split(Regex("\\s+")).size / 2.5) * 1000).toLong().coerceAtLeast(1800L)
        val start = cursorMs
        val end = cursorMs + duration
        cursorMs = end + 250L
        (index + 1).toString() + "\n" + srtTime(start) + " --> " + srtTime(end) + "\n" + line
    }.joinToString("\n\n")
}

private fun srtTime(milliseconds: Long): String {
    val hours = milliseconds / 3_600_000
    val minutes = (milliseconds % 3_600_000) / 60_000
    val seconds = (milliseconds % 60_000) / 1_000
    val millis = milliseconds % 1_000
    return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
}
