package com.gikoko.aivideostudio

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipInputStream

private val Gold = Color(0xFFFFC857)
private val Emerald = Color(0xFF00C389)
private val Ink = Color(0xFF101713)
private val Panel = Color(0xFF1B2721)

private data class TimelineClip(
    val id: Int,
    val track: String,
    val title: String,
    val startSeconds: String,
    val durationSeconds: String,
    val source: String
)

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
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf("5 min") }
    var customDuration by remember { mutableStateOf("5") }
    var apiKey by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("Create an engaging short video about an amazing fact from Myanmar history.") }
    var script by remember { mutableStateOf("") }
    var subtitles by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready. Configure an AI provider to begin.") }
    var busy by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val timelineClips = remember { mutableStateListOf<TimelineClip>() }
    var nextTimelineClipId by remember { mutableIntStateOf(1) }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var allowPublicUpload by remember { mutableStateOf(false) }
    var uploadedMediaUrl by remember { mutableStateOf("") }
    var discoveredModels by remember { mutableStateOf(listOf<String>()) }
    var freeOnly by remember { mutableStateOf(true) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    val voiceExportTarget = remember { AtomicReference<Uri?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var githubToken by remember { mutableStateOf("") }
    var sourceVideoUrl by remember { mutableStateOf("") }
    var voiceoverUrl by remember { mutableStateOf("") }
    var subtitleUrl by remember { mutableStateOf("") }
    var autoSubtitles by remember { mutableStateOf(true) }
    var trimStart by remember { mutableStateOf("0") }
    var trimEnd by remember { mutableStateOf("") }
    var aspectRatio by remember { mutableStateOf("9:16") }
    var renderRunUrl by remember { mutableStateOf("") }
    var renderRunId by remember { mutableStateOf(0L) }
    var renderRunStatus by remember { mutableStateOf("No render job checked yet.") }
    var renderBusy by remember { mutableStateOf(false) }

    var textVideoDuration by remember { mutableStateOf("5") }
    var textVideoResolution by remember { mutableStateOf("720p") }
    var textVideoAspect by remember { mutableStateOf("9:16") }
    var textVideoAudio by remember { mutableStateOf(true) }
    var textVideoRunId by remember { mutableStateOf(0L) }
    var textVideoRunUrl by remember { mutableStateOf("") }
    var textVideoRunStatus by remember { mutableStateOf("No AI video generation job submitted yet.") }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
            if (result == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "gikoko-voice-export") {
                    val target = voiceExportTarget.getAndSet(null)
                    if (target != null) {
                        Thread {
                            try {
                                val audioFile = File(context.cacheDir, "gikoko-voice.wav")
                                context.contentResolver.openOutputStream(target)?.use { output ->
                                    audioFile.inputStream().use { input -> input.copyTo(output) }
                                } ?: throw IllegalStateException("Could not open destination file.")
                                mainHandler.post { status = "WAV narration exported successfully." }
                            } catch (error: Exception) {
                                mainHandler.post { status = "WAV export failed: " + (error.message ?: "unknown error") }
                            }
                        }.start()
                    }
                }
            }
            override fun onError(utteranceId: String?) {
                if (utteranceId == "gikoko-voice-export") {
                    voiceExportTarget.set(null)
                    mainHandler.post { status = "WAV synthesis failed. Check the installed English TTS voice." }
                }
            }
        })
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }

    val voiceExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri ->
        if (uri != null) {
            if (script.isBlank()) status = "Enter or generate a script first."
            else {
                val audioFile = File(context.cacheDir, "gikoko-voice.wav")
                audioFile.delete()
                voiceExportTarget.set(uri)
                val result = tts?.synthesizeToFile(script.take(9000), Bundle(), audioFile, "gikoko-voice-export")
                    ?: TextToSpeech.ERROR
                if (result != TextToSpeech.SUCCESS) {
                    voiceExportTarget.set(null)
                    status = "Could not start WAV synthesis. Check Android English TTS settings."
                } else status = "Creating WAV narration… keep the app open until export completes."
            }
        }
    }

    val mp3Exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { uri ->
        if (uri != null) {
            if (githubToken.isBlank() || renderRunId == 0L) status = "Check a successful render job first and enter the GitHub token."
            else {
                renderBusy = true; status = "Downloading rendered MP3 audio…"
                runTask({ downloadCloudAudioArtifact(githubToken, renderRunId, uri, context) }) { result, error ->
                    renderBusy = false
                    status = error?.let { "MP3 download failed: $it" } ?: (result as? String ?: "MP3 downloaded.")
                }
            }
        }
    }

    val mp4Exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
        if (uri != null) {
            if (githubToken.isBlank() || renderRunId == 0L) status = "Check a render job first and enter the GitHub token."
            else {
                renderBusy = true; status = "Downloading and extracting rendered MP4…"
                runTask({ downloadCloudArtifact(githubToken, renderRunId, uri, context) }) { result, error ->
                    renderBusy = false
                    status = error?.let { "MP4 download failed: $it" } ?: (result as? String ?: "MP4 downloaded.")
                }
            }
        }
    }


    val textVideoExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
        if (uri != null) {
            if (githubToken.isBlank() || textVideoRunId == 0L) status = "Submit and check a Text-to-Video job first."
            else {
                renderBusy = true; status = "Downloading AI-generated MP4…"
                runTask({ downloadCloudArtifact(githubToken, textVideoRunId, uri, context) }) { result, error ->
                    renderBusy = false
                    status = error?.let { "AI video download failed: $it" } ?: (result as? String ?: "AI video downloaded.")
                }
            }
        }
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
            uploadedMediaUrl = ""
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
                Text("AI VIDEO STUDIO  •  v0.0.1 development", color = Emerald, style = MaterialTheme.typography.labelMedium)
            }
        }, bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Create", "Media", "Voice", "Timeline", "Render", "Settings").forEachIndexed { index, label ->
                    NavigationBarItem(selected = selectedTab == index, onClick = { selectedTab = index },
                        icon = { Text(listOf("✦", "▣", "♫", "▤", "⇧", "⚙")[index]) }, label = { Text(label) })
                }
            }
        }) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (selectedTab) {
                    0 -> {
                        Text("AI SCRIPT GENERATOR", color = Gold, fontWeight = FontWeight.Bold)

                        HorizontalDivider(color = Emerald.copy(alpha = 0.35f))
                        Text("TEXT-TO-VIDEO GENERATOR • WAN 3.0", color = Gold, fontWeight = FontWeight.Bold)
                        Text("Generate a real MP4 from text only. This calls the Wan 3.0 video model through fal.ai using a protected GitHub Actions secret; it is separate from AI script generation.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        Text("VIDEO PROMPT", color = Emerald, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Describe subject, action, camera, lighting, style and sound") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = textVideoDuration, onValueChange = { textVideoDuration = it.filter(Char::isDigit).take(2) }, label = { Text("Seconds (5/10/15)") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = textVideoResolution, onValueChange = { textVideoResolution = it }, label = { Text("Resolution") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        OutlinedTextField(value = textVideoAspect, onValueChange = { textVideoAspect = it }, label = { Text("Aspect ratio: 9:16 / 16:9 / 1:1 / 4:3 / 3:4") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("GENERATE SYNCHRONIZED AUDIO", color = Emerald, fontWeight = FontWeight.Bold)
                                Text("Wan 3.0 can generate sound with the video.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                            }
                            Checkbox(checked = textVideoAudio, onCheckedChange = { textVideoAudio = it })
                        }
                        OutlinedTextField(value = githubToken, onValueChange = { githubToken = it }, label = { Text("GitHub token — session only") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(onClick = {
                            if (githubToken.isBlank()) status = "Enter a GitHub token with Actions read/write permission."
                            else if (prompt.isBlank()) status = "Write a video prompt first."
                            else if (textVideoDuration !in listOf("5", "10", "15")) status = "Duration must be 5, 10, or 15 seconds."
                            else if (textVideoResolution !in listOf("480p", "720p", "1080p")) status = "Resolution must be 480p, 720p, or 1080p."
                            else if (textVideoAspect !in listOf("9:16", "16:9", "1:1", "4:3", "3:4")) status = "Choose a supported aspect ratio."
                            else {
                                renderBusy = true; status = "Submitting prompt to Wan 3.0…"
                                runTask({ dispatchTextVideo(githubToken, prompt, textVideoResolution, textVideoAspect, textVideoDuration, textVideoAudio) }) { _, error ->
                                    renderBusy = false
                                    status = error?.let { "Text-to-Video dispatch failed: $it" } ?: "AI video request submitted. Check generation status in a few seconds."
                                }
                            }
                        }, enabled = !renderBusy, modifier = Modifier.fillMaxWidth()) { Text(if (renderBusy) "Submitting…" else "✦ Generate AI Video from Prompt") }
                        OutlinedButton(onClick = {
                            if (githubToken.isBlank()) status = "Enter GitHub token first."
                            else {
                                renderBusy = true; status = "Checking latest Wan 3.0 generation…"
                                runTask({ latestTextVideoRun(githubToken) }) { result, error ->
                                    renderBusy = false
                                    if (error != null) status = "Text-to-Video check failed: $error"
                                    else {
                                        val run = result as? JSONObject
                                        textVideoRunId = run?.optLong("id") ?: 0L
                                        textVideoRunUrl = run?.optString("html_url").orEmpty()
                                        textVideoRunStatus = "Status: " + run?.optString("status") + " | Conclusion: " + run?.optString("conclusion").ifNullOrEmpty("pending")
                                        status = textVideoRunStatus
                                    }
                                }
                            }
                        }, enabled = !renderBusy, modifier = Modifier.fillMaxWidth()) { Text("Check AI Video Generation") }
                        Text(textVideoRunStatus, color = Emerald)
                        if (textVideoRunUrl.isNotBlank()) {
                            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(textVideoRunUrl))) }, modifier = Modifier.fillMaxWidth()) { Text("Open generation job") }
                            OutlinedButton(onClick = { textVideoExporter.launch("GIKOKO_AI_VIDEO.mp4") }, enabled = !renderBusy && textVideoRunId != 0L, modifier = Modifier.fillMaxWidth()) { Text("Download AI-generated MP4") }
                        }
                        Text("IMPORTANT: Add FAL_KEY as a GitHub repository Actions secret before generating. fal.ai API generations are pay-per-use; free sandbox daily generations are browser-only and are not guaranteed through this API workflow. Review the price before using.", color = Gold, style = MaterialTheme.typography.bodySmall)
                        Text("VIDEO DURATION", color = Gold, fontWeight = FontWeight.Bold)
                        var durationMenuExpanded by remember { mutableStateOf(false) }
                        val durationOptions = listOf("30 sec", "1 min", "3 min", "5 min", "10 min", "15 min", "20 min", "30 min", "45 min", "60 min", "Custom")
                        Box {
                            OutlinedButton(onClick = { durationMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Duration: $duration  ▾") }
                            DropdownMenu(expanded = durationMenuExpanded, onDismissRequest = { durationMenuExpanded = false }) {
                                durationOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { duration = option; durationMenuExpanded = false })
                                }
                            }
                        }
                        if (duration == "Custom") {
                            OutlinedTextField(value = customDuration, onValueChange = { customDuration = it.filter(Char::isDigit).take(3) }, label = { Text("Custom duration (minutes, max 180)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Video idea / prompt") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                        Button(onClick = {
                            if (apiKey.isBlank()) status = "Add your API key in Settings first."
                            else if (model.isBlank()) status = "Select a model first."
                            else if (duration == "Custom" && (customDuration.toIntOrNull() !in 1..180)) status = "Custom duration must be between 1 and 180 minutes."
                            else {
                                busy = true; status = "Generating script…"
                                runTask({ callChat(baseUrl, apiKey, model, "Create a video script designed for $duration ${if (duration == "Custom") customDuration + " minutes" else ""}. Include scene directions, narration, and pacing appropriate to the selected duration. Return plain text. User idea: $prompt") }) { result, error ->
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
                        Text("Import video, audio, or image files. Upload each asset to temporary public hosting, then add it to the matching Timeline track.", color = Color.LightGray)
                        Button(onClick = { mediaPicker.launch(arrayOf("video/*", "audio/*", "image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Import media") }
                        Text(mediaUri?.toString() ?: "No media selected", color = Emerald)
                        Text("Temporary upload uses Litterbox by Catbox. Uploaded files are publicly accessible and automatically expire after 72 hours. Do not upload private, sensitive, or copyrighted material you do not have permission to share.", color = Gold, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("I agree to public temporary hosting", modifier = Modifier.weight(1f), color = Color.White)
                            Checkbox(checked = allowPublicUpload, onCheckedChange = { allowPublicUpload = it })
                        }
                        Button(onClick = {
                            val uri = mediaUri
                            if (uri == null) status = "Import a video or audio file first."
                            else if (!allowPublicUpload) status = "Confirm public temporary upload consent first."
                            else {
                                val mime = context.contentResolver.getType(uri).orEmpty()
                                if (!mime.startsWith("video/") && !mime.startsWith("audio/") && !mime.startsWith("image/")) status = "Choose a video, audio, or image file."
                                else {
                                    renderBusy = true; status = "Uploading media to temporary hosting…"
                                    runTask({ uploadToLitterbox(context, uri) }) { result, error ->
                                        renderBusy = false
                                        if (error != null) status = "Media upload failed: $error"
                                        else {
                                            uploadedMediaUrl = result as? String ?: ""
                                            if (mime.startsWith("video/")) {
                                                sourceVideoUrl = uploadedMediaUrl
                                                status = "Video uploaded. Direct URL added to Cloud Render."
                                            } else if (mime.startsWith("audio/")) {
                                                voiceoverUrl = uploadedMediaUrl
                                                status = "Audio uploaded. Add it to the VOICE track; repeat to mix more tracks."
                                            } else {
                                                status = "Image uploaded. Add it to the OVERLAY track."
                                            }
                                        }
                                    }
                                }
                            }
                        }, enabled = !renderBusy && mediaUri != null, modifier = Modifier.fillMaxWidth()) { Text(if (renderBusy) "Uploading…" else "Upload selected media (72h link)") }
                        if (uploadedMediaUrl.isNotBlank()) {
                            Text("Uploaded URL", color = Gold, fontWeight = FontWeight.Bold)
                            Text(uploadedMediaUrl, color = Emerald)
                            OutlinedButton(onClick = { android.content.ClipData.newPlainText("Uploaded media URL", uploadedMediaUrl).let { clip -> (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip) }; status = "Upload URL copied." }, modifier = Modifier.fillMaxWidth()) { Text("Copy uploaded URL") }
                        }
                        Text("Use media you own or are licensed to use. The temporary link is public; anyone with the URL may access it.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
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
                        OutlinedButton(onClick = { if (script.isBlank()) status = "Generate or enter a script first." else voiceExporter.launch("GIKOKO_narration.wav") }, modifier = Modifier.fillMaxWidth()) { Text("Export narration as WAV") }
                        Text("WAV export uses the Android system English TTS voice. Keep the app open while synthesis finishes.")
                    }
                    3 -> {
                        Text("MULTI-TRACK TIMELINE", color = Gold, fontWeight = FontWeight.Bold)
                        Text("Upload each video, audio, or image separately. Add videos to VIDEO, audio files to VOICE for multi-track mixing, and images to OVERLAY. Start and duration control clip timing. MP3 audio export is also available.", color = Color.LightGray)
                        val trackNames = listOf("VIDEO", "VOICE", "SUBTITLE", "OVERLAY")
                        trackNames.forEach { trackName ->
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(trackName, color = Gold, fontWeight = FontWeight.Bold)
                                    val trackClips = timelineClips.filter { it.track == trackName }
                                    if (trackClips.isEmpty()) Text("No clips on this track", color = Color.Gray)
                                    trackClips.forEach { clip ->
                                        val clipIndex = timelineClips.indexOfFirst { it.id == clip.id }
                                        if (clipIndex >= 0) {
                                            OutlinedTextField(value = clip.title, onValueChange = { timelineClips[clipIndex] = timelineClips[clipIndex].copy(title = it) }, label = { Text("Clip name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(value = clip.startSeconds, onValueChange = { timelineClips[clipIndex] = timelineClips[clipIndex].copy(startSeconds = it.filter { ch -> ch.isDigit() }.take(6)) }, label = { Text("Start (sec)") }, modifier = Modifier.weight(1f), singleLine = true)
                                                OutlinedTextField(value = clip.durationSeconds, onValueChange = { timelineClips[clipIndex] = timelineClips[clipIndex].copy(durationSeconds = it.filter { ch -> ch.isDigit() }.take(6)) }, label = { Text("Length (sec)") }, modifier = Modifier.weight(1f), singleLine = true)
                                            }
                                            Text("Source: ${clip.source}", color = Color.LightGray, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                            TextButton(onClick = { timelineClips.removeAll { it.id == clip.id } }) { Text("Remove clip", color = Color(0xFFFF8A80)) }
                                        }
                                    }
                                    when (trackName) {
                                        "VIDEO" -> OutlinedButton(onClick = {
                                            val selectedMime = mediaUri?.let { context.contentResolver.getType(it).orEmpty() }.orEmpty()
                                            if (mediaUri == null || !selectedMime.startsWith("video/")) status = "Import a video in Media first."
                                            else if (!uploadedMediaUrl.startsWith("https://")) status = "Upload this selected video in Media first. Its temporary HTTPS URL is required."
                                            else { timelineClips.add(TimelineClip(nextTimelineClipId++, trackName, "Video clip", "0", "30", uploadedMediaUrl)); status = "Uploaded video URL added to Video track." }
                                        }, modifier = Modifier.fillMaxWidth()) { Text("＋ Add uploaded video") }
                                        "VOICE" -> OutlinedButton(onClick = {
                                            if (!voiceoverUrl.startsWith("https://")) status = "Import and upload an audio file first. Then add it to the VOICE track."
                                            else { timelineClips.add(TimelineClip(nextTimelineClipId++, trackName, "Audio track ${timelineClips.count { it.track == "VOICE" } + 1}", "0", "30", voiceoverUrl)); status = "Audio clip added. Upload another audio file to add another track." }
                                        }, modifier = Modifier.fillMaxWidth()) { Text("＋ Add audio clip") }
                                        "SUBTITLE" -> OutlinedButton(onClick = {
                                            if (subtitles.isBlank()) status = "Create subtitle text in Create first."
                                            else { timelineClips.add(TimelineClip(nextTimelineClipId++, trackName, "Subtitle captions", "0", "30", "Editable subtitle text")); status = "Subtitle clip added." }
                                        }, modifier = Modifier.fillMaxWidth()) { Text("＋ Add subtitles") }
                                        "OVERLAY" -> OutlinedButton(onClick = {
                                            if (uploadedMediaUrl.startsWith("https://") && mediaUri?.let { context.contentResolver.getType(it).orEmpty().startsWith("image/") } == true) { timelineClips.add(TimelineClip(nextTimelineClipId++, trackName, "Image overlay", "0", "30", uploadedMediaUrl)); status = "Uploaded image overlay added." } else { timelineClips.add(TimelineClip(nextTimelineClipId++, trackName, "Powered by I KO KO", "0", "30", "Text watermark overlay")); status = "Text watermark overlay added. Import and upload an image first to use image overlays." }
                                        }, modifier = Modifier.fillMaxWidth()) { Text("＋ Add watermark overlay") }
                                    }
                                }
                            }
                        }
                        val timelineEnd = timelineClips.maxOfOrNull { (it.startSeconds.toLongOrNull() ?: 0L) + (it.durationSeconds.toLongOrNull() ?: 0L) } ?: 0L
                        Text("VIDEO clips are joined in start-time order. VOICE clips are mixed with individual start/duration. OVERLAY images appear at the top-right during their clip interval.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        Text("Clips: ${timelineClips.size}   •   Timeline end: ${timelineEnd}s", color = Emerald, fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = { timelineClips.clear(); status = "Timeline cleared." }, modifier = Modifier.fillMaxWidth(), enabled = timelineClips.isNotEmpty()) { Text("Clear timeline") }
                    }
                    4 -> {
                        Text("CLOUD VIDEO RENDER", color = Gold, fontWeight = FontWeight.Bold)
                        Text("Send a publicly downloadable MP4 URL to the GitHub Actions FFmpeg renderer. A phone-only local file cannot be reached by GitHub until uploaded to a file host that provides a direct download URL.", color = Color.LightGray)
                        OutlinedTextField(value = sourceVideoUrl, onValueChange = { sourceVideoUrl = it }, label = { Text("Direct MP4 download URL (HTTPS)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        OutlinedTextField(value = voiceoverUrl, onValueChange = { voiceoverUrl = it }, label = { Text("Optional voiceover MP3/WAV URL") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        OutlinedTextField(value = subtitleUrl, onValueChange = { subtitleUrl = it }, label = { Text("Optional SRT subtitle URL (manual SRT overrides auto)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("AUTO SPEECH-ALIGNED SUBTITLES", color = Gold, fontWeight = FontWeight.Bold)
                                Text("Whisper transcribes the voiceover, or the source video audio if no voiceover is supplied. A manual SRT URL takes priority.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                            }
                            Checkbox(checked = autoSubtitles, onCheckedChange = { autoSubtitles = it })
                        }
                        OutlinedTextField(value = trimStart, onValueChange = { trimStart = it }, label = { Text("Trim start (seconds, default 0)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = trimEnd, onValueChange = { trimEnd = it }, label = { Text("Trim end (seconds, optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = githubToken, onValueChange = { githubToken = it }, label = { Text("GitHub token — session only") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = aspectRatio, onValueChange = { aspectRatio = it }, label = { Text("Aspect ratio: 9:16 / 16:9 / 1:1") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(onClick = {
                            if (githubToken.isBlank()) status = "Enter a GitHub token with Actions read/write permission."
                            else if (!sourceVideoUrl.startsWith("https://") && timelineClips.none { it.track == "VIDEO" && it.source.startsWith("https://") }) status = "Enter a direct HTTPS MP4 URL or add an uploaded VIDEO clip to the Timeline."
                            else if (aspectRatio !in listOf("9:16", "16:9", "1:1")) status = "Aspect ratio must be 9:16, 16:9, or 1:1."
                            else {
                                renderBusy = true; status = "Submitting GitHub render job…"
                                val renderSourceUrl = sourceVideoUrl.takeIf { it.startsWith("https://") } ?: timelineClips.first { it.track == "VIDEO" && it.source.startsWith("https://") }.source
                                val timelinePayload = JSONArray().apply {
                                    timelineClips.forEach { clip ->
                                        put(JSONObject()
                                            .put("track", clip.track)
                                            .put("title", clip.title)
                                            .put("start", clip.startSeconds.toDoubleOrNull() ?: 0.0)
                                            .put("duration", clip.durationSeconds.toDoubleOrNull() ?: 0.0)
                                            .put("source", clip.source))
                                    }
                                }.toString()
                                runTask({ dispatchCloudRender(githubToken, renderSourceUrl, aspectRatio, voiceoverUrl, subtitleUrl, trimStart, trimEnd, autoSubtitles, timelinePayload) }) { _, error ->
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
                                        renderRunId = run?.optLong("id") ?: 0L
                                        status = renderRunStatus
                                    }
                                }
                            }
                        }, enabled = !renderBusy, modifier = Modifier.fillMaxWidth()) { Text("Check latest render job") }
                        OutlinedButton(onClick = { mp3Exporter.launch("GIKOKO_audio.mp3") }, enabled = !renderBusy, modifier = Modifier.fillMaxWidth()) { Text("Export rendered audio as MP3") }
                        Text("MP3 is generated from the completed cloud-rendered video. Timeline VOICE tracks are mixed during render.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        Text(renderRunStatus, color = Emerald)
                        if (renderRunUrl.isNotBlank()) {
                            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(renderRunUrl))) }, modifier = Modifier.fillMaxWidth()) { Text("Open GitHub job") }
                            OutlinedButton(onClick = { mp4Exporter.launch("GIKOKO_RENDERED.mp4") }, enabled = !renderBusy && renderRunId != 0L, modifier = Modifier.fillMaxWidth()) { Text("Download MP4 into phone storage") }
                        }
                        Text("Use a fine-grained GitHub token limited to this repository with Actions: Read and write and Metadata: Read. The token stays in memory only; never share it.", color = Gold, style = MaterialTheme.typography.bodySmall)
                        Text("GitHub Actions renders with CPU FFmpeg. Free minutes and artifact retention limits apply; GPU is not guaranteed.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                    else -> {
                        Text("AI PROVIDER SETTINGS", color = Gold, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key (session only)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("FREE MODELS ONLY", color = Emerald, fontWeight = FontWeight.Bold)
                                Text("Hide models unless the provider marks them free or reports zero input/output pricing.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                            }
                            Checkbox(checked = freeOnly, onCheckedChange = { freeOnly = it; discoveredModels = emptyList(); model = "" })
                        }
                        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Selected Model ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = true)
                        OutlinedButton(onClick = {
                            if (baseUrl.isBlank()) status = "Enter the provider Base URL first."
                            else if (apiKey.isBlank()) status = "Enter API Key first."
                            else {
                                busy = true; status = "Connecting and loading model list…"
                                runTask({ fetchModels(baseUrl, apiKey, freeOnly) }) { result, error ->
                                    busy = false
                                    if (error != null) status = "Connection/model loading failed: $error"
                                    else {
                                        discoveredModels = (result as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                        if (discoveredModels.isNotEmpty()) {
                                            if (model !in discoveredModels) model = discoveredModels.first()
                                            modelMenuExpanded = true
                                            status = "Connected. Loaded ${discoveredModels.size} models. Choose one below."
                                        } else status = if (freeOnly) "No verifiably free models found. This provider may not expose free-tier pricing in /models." else "Provider returned no models in the OpenAI-compatible /models format."
                                    }
                                }
                            }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Connecting…" else "Run: Connect & Auto-load Models") }
                        Box {
                            OutlinedButton(onClick = { modelMenuExpanded = true }, enabled = discoveredModels.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(if (model.isBlank()) "Select a model" else "Choose Model: $model") }
                            DropdownMenu(expanded = modelMenuExpanded, onDismissRequest = { modelMenuExpanded = false }) {
                                discoveredModels.forEach { item ->
                                    DropdownMenuItem(text = { Text(item) }, onClick = {
                                        model = item; modelMenuExpanded = false
                                        prefs.edit().putString("base_url", baseUrl.trim()).putString("model", model.trim()).apply()
                                        status = "Selected model: $item"
                                    })
                                }
                            }
                        }
                        Button(onClick = {
                            if (baseUrl.isBlank() || model.isBlank()) status = "Enter provider URL and select a model first."
                            else {
                                prefs.edit().putString("base_url", baseUrl.trim()).putString("model", model.trim()).apply()
                                status = "Provider URL and model saved. API key remains in memory only."
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Save provider settings") }
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

private fun fetchModels(base: String, key: String, freeOnly: Boolean = true): List<String> {
    val response = requestJson(base.trimEnd('/') + "/models", key, "GET", null)
    val data = response.optJSONArray("data") ?: return emptyList()
    return (0 until data.length()).mapNotNull { index ->
        val item = data.optJSONObject(index) ?: return@mapNotNull null
        val id = item.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
        if (!freeOnly || isExplicitlyFreeModel(id, item)) id else null
    }.sorted()
}

private fun isExplicitlyFreeModel(id: String, item: JSONObject): Boolean {
    val normalized = id.lowercase()
    if (normalized.endsWith(":free") || normalized.contains("-free") || normalized.contains("_free")) return true
    val pricing = item.optJSONObject("pricing") ?: return false
    fun isZero(value: Any?): Boolean {
        val number = value?.toString()?.toDoubleOrNull()
        return number != null && number == 0.0
    }
    return isZero(pricing.opt("prompt")) && isZero(pricing.opt("completion"))
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

private fun uploadToLitterbox(context: android.content.Context, uri: Uri): String {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
    require(mime.startsWith("video/") || mime.startsWith("audio/") || mime.startsWith("image/")) { "Only video, audio, and image uploads are supported." }
    val rawName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: "gikoko-media.bin"
    val fileName = rawName.replace("\"", "_").replace("\r", "_").replace("\n", "_").take(120)
    val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
    require(size > 0L) { "Could not determine file size. Choose a normal local video or audio file." }
    require(size <= 1024L * 1024L * 1024L) { "Litterbox maximum upload size is 1 GB." }

    val boundary = "----GIKOKO" + java.util.UUID.randomUUID().toString().replace("-", "")
    val connection = URL("https://litterbox.catbox.moe/resources/internals/api.php").openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 30000
        connection.readTimeout = 30 * 60 * 1000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.setRequestProperty("Accept", "text/plain")
        val prefix = "--$boundary\r\nContent-Disposition: form-data; name=\"reqtype\"\r\n\r\nfileupload\r\n" +
            "--$boundary\r\nContent-Disposition: form-data; name=\"time\"\r\n\r\n72h\r\n" +
            "--$boundary\r\nContent-Disposition: form-data; name=\"fileToUpload\"; filename=\"$fileName\"\r\nContent-Type: $mime\r\n\r\n"
        val suffix = "\r\n--$boundary--\r\n"
        val prefixBytes = prefix.toByteArray(StandardCharsets.UTF_8)
        val suffixBytes = suffix.toByteArray(StandardCharsets.UTF_8)
        connection.setFixedLengthStreamingMode(prefixBytes.size.toLong() + size + suffixBytes.size)
        connection.outputStream.use { output ->
            output.write(prefixBytes)
            resolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    total += count
                }
                require(total == size) { "File changed during upload; please select it again." }
            } ?: throw IllegalStateException("Could not read the selected file.")
            output.write(suffixBytes)
            output.flush()
        }
        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText().trim() }.orEmpty()
        if (code !in 200..299) throw IllegalStateException("Upload server HTTP $code: " + response.take(300))
        require(response.startsWith("https://litter.catbox.moe/")) { "Upload service returned an unexpected response: " + response.take(250) }
        return response
    } finally {
        connection.disconnect()
    }
}


private fun dispatchTextVideo(token: String, prompt: String, resolution: String, ratio: String, duration: String, audio: Boolean): String {
    require(token.isNotBlank()) { "GitHub token is required." }
    require(prompt.isNotBlank() && prompt.length <= 5000) { "Prompt must contain 1–5000 characters." }
    require(resolution in listOf("480p", "720p", "1080p")) { "Unsupported resolution." }
    require(ratio in listOf("9:16", "16:9", "1:1", "4:3", "3:4")) { "Unsupported aspect ratio." }
    require(duration in listOf("5", "10", "15")) { "Duration must be 5, 10, or 15 seconds." }
    val body = JSONObject()
        .put("ref", "main")
        .put("inputs", JSONObject()
            .put("prompt", prompt.trim())
            .put("resolution", resolution)
            .put("aspect_ratio", ratio)
            .put("duration", duration)
            .put("audio", audio))
    githubRequest(token, "/repos/gikoko611/GIKOKO-AI-VIDEO-STUDIO-/actions/workflows/text-to-video.yml/dispatches", "POST", body.toString())
    return "Text-to-Video workflow dispatch accepted."
}

private fun latestTextVideoRun(token: String): JSONObject {
    val response = githubRequest(
        token,
        "/repos/gikoko611/GIKOKO-AI-VIDEO-STUDIO-/actions/workflows/text-to-video.yml/runs?branch=main&per_page=1",
        "GET",
        null
    )
    val runs = response.optJSONArray("workflow_runs")
    if (runs == null || runs.length() == 0) throw IllegalStateException("No Text-to-Video runs found yet. Wait a few seconds after dispatch.")
    return runs.getJSONObject(0)
}

private fun dispatchCloudRender(token: String, videoUrl: String, ratio: String, voiceoverUrl: String, subtitleUrl: String, trimStart: String, trimEnd: String, autoSubtitles: Boolean, timelineJson: String): String {
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
            .put("auto_subtitles", autoSubtitles)
            .put("subtitle_language", "auto")
            .put("timeline_json", timelineJson)
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


private fun downloadCloudAudioArtifact(token: String, runId: Long, targetUri: Uri, context: android.content.Context): String {
    require(token.isNotBlank()) { "GitHub token is required." }
    require(runId > 0L) { "Check a render job before downloading." }
    val response = githubRequest(token, "/repos/gikoko611/GIKOKO-AI-VIDEO-STUDIO-/actions/runs/" + runId + "/artifacts", "GET", null)
    val artifacts = response.optJSONArray("artifacts") ?: throw IllegalStateException("No render artifact found.")
    val artifact = (0 until artifacts.length()).map { artifacts.getJSONObject(it) }.firstOrNull { !it.optBoolean("expired", false) }
        ?: throw IllegalStateException("The render artifact has expired.")
    val url = artifact.optString("archive_download_url")
    require(url.startsWith("https://api.github.com/")) { "Invalid artifact URL." }
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.setRequestProperty("Authorization", "Bearer " + token)
    connection.instanceFollowRedirects = false
    val code = connection.responseCode
    val redirect = connection.getHeaderField("Location")
    connection.disconnect()
    require(code in 300..399 && !redirect.isNullOrBlank()) { "Artifact download did not return a redirect." }
    val zipConnection = URL(redirect).openConnection() as HttpURLConnection
    try {
        ZipInputStream(zipConnection.inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.lowercase().endsWith(".mp3")) {
                    context.contentResolver.openOutputStream(targetUri)?.use { zip.copyTo(it) }
                        ?: throw IllegalStateException("Could not open phone storage destination.")
                    return "Rendered MP3 saved to the selected phone location."
                }
                zip.closeEntry(); entry = zip.nextEntry
            }
        }
    } finally { zipConnection.disconnect() }
    throw IllegalStateException("The artifact ZIP did not contain an MP3 file.")
}

private fun downloadCloudArtifact(token: String, runId: Long, targetUri: Uri, context: android.content.Context): String {
    require(token.isNotBlank()) { "GitHub token is required." }
    require(runId > 0L) { "Check a render job before downloading." }
    val artifactResponse = githubRequest(
        token,
        "/repos/gikoko611/GIKOKO-AI-VIDEO-STUDIO-/actions/runs/" + runId + "/artifacts",
        "GET",
        null
    )
    val artifacts = artifactResponse.optJSONArray("artifacts")
    if (artifacts == null || artifacts.length() == 0) throw IllegalStateException("No artifact is available yet. Wait until the render succeeds.")
    val artifact = (0 until artifacts.length()).map { artifacts.getJSONObject(it) }.firstOrNull {
        !it.optBoolean("expired", false)
    } ?: throw IllegalStateException("The render artifact has expired.")
    val archiveUrl = artifact.optString("archive_download_url")
    if (!archiveUrl.startsWith("https://api.github.com/")) throw IllegalStateException("GitHub returned an invalid artifact URL.")

    val apiConnection = URL(archiveUrl).openConnection() as HttpURLConnection
    apiConnection.instanceFollowRedirects = false
    apiConnection.connectTimeout = 20000
    apiConnection.readTimeout = 60000
    apiConnection.setRequestProperty("Authorization", "Bearer " + token)
    apiConnection.setRequestProperty("Accept", "application/vnd.github+json")
    val code = apiConnection.responseCode
    val redirectUrl = if (code in 300..399) apiConnection.getHeaderField("Location") else null
    if (code !in 200..299 && redirectUrl.isNullOrBlank()) {
        val errorText = apiConnection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        apiConnection.disconnect()
        throw IllegalStateException("Artifact download HTTP " + code + ": " + errorText.take(300))
    }

    val zipConnection: HttpURLConnection?
    val zipStream: java.io.InputStream
    if (!redirectUrl.isNullOrBlank()) {
        apiConnection.disconnect()
        zipConnection = URL(redirectUrl).openConnection() as HttpURLConnection
        zipConnection.connectTimeout = 20000
        zipConnection.readTimeout = 120000
        zipStream = zipConnection.inputStream
    } else {
        zipConnection = null
        zipStream = apiConnection.inputStream
    }

    var extracted = false
    try {
        ZipInputStream(zipStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.lowercase().endsWith(".mp4")) {
                    context.contentResolver.openOutputStream(targetUri)?.use { output ->
                        zip.copyTo(output)
                    } ?: throw IllegalStateException("Could not open selected phone storage destination.")
                    extracted = true
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    } finally {
        apiConnection.disconnect()
        zipConnection?.disconnect()
    }
    if (!extracted) throw IllegalStateException("The artifact ZIP did not contain an MP4 file.")
    return "Rendered MP4 saved to the selected phone location."
}
