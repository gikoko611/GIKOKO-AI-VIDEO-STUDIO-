@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.gikoko.aivideostudio

import android.content.Context
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.os.Bundle
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val StudioGold = Color(0xFFFFC857)
private val StudioGreen = Color(0xFF00C389)
private val StudioBackground = Color(0xFF101713)
private val StudioPanel = Color(0xFF1B2721)

private data class StudioAudioTrack(val uri: Uri, val label: String)

@Composable
fun OfflineStudioApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }
    var trimStartMs by remember { mutableLongStateOf(0L) }
    var trimEndMs by remember { mutableLongStateOf(0L) }
    var resolution by remember { mutableStateOf("480p") }
    var status by remember { mutableStateOf("Offline editor ready. Import a video to begin.") }
    var exporting by remember { mutableStateOf(false) }
    var audioGain by remember { mutableFloatStateOf(1f) }
    var lowDb by remember { mutableFloatStateOf(0f) }
    var midDb by remember { mutableFloatStateOf(0f) }
    var highDb by remember { mutableFloatStateOf(0f) }
    var compressorThreshold by remember { mutableFloatStateOf(-12f) }
    var compressorRatio by remember { mutableFloatStateOf(3f) }
    var noiseGateDb by remember { mutableFloatStateOf(-55f) }
    val audioTracks = remember { mutableStateListOf<StudioAudioTrack>() }

    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(videoUri) {
        val uri = videoUri
        if (uri == null) {
            player.clearMediaItems()
            videoDurationMs = 0L
            trimStartMs = 0L
            trimEndMs = 0L
        } else {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = false
            val duration = withContext(Dispatchers.IO) { readDurationMs(context, uri) }
            videoDurationMs = duration
            trimStartMs = 0L
            trimEndMs = duration
            status = if (duration > 0L) "Video loaded. Set trim range and edit audio." else "Video loaded. Duration could not be read; check the file format."
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            videoUri = uri
        }
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            if (audioTracks.none { it.uri == uri }) {
                audioTracks.add(StudioAudioTrack(uri, uri.lastPathSegment?.substringAfterLast('/') ?: "Audio track"))
            }
        }
        if (uris.isNotEmpty()) status = uris.size.toString() + " audio track(s) added. Tracks are mixed together from the start of the project."
    }

    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { target ->
        val source = videoUri
        if (target != null && source != null) {
            val end = if (trimEndMs > 0L) trimEndMs else videoDurationMs
            if (end <= trimStartMs) {
                status = "Trim end must be later than trim start."
            } else {
                exporting = true
                status = "Preparing offline MP4 export…"
                startOfflineExport(
                    context = context,
                    scope = scope,
                    videoUri = source,
                    audioUris = audioTracks.map { it.uri },
                    startMs = trimStartMs,
                    endMs = end,
                    shortSide = if (resolution == "720p") 720 else 480,
                    audioSettings = StudioAudioSettings(audioGain, lowDb, midDb, highDb, compressorThreshold, compressorRatio, noiseGateDb),
                    targetUri = target,
                    onStatus = { status = it },
                    onFinished = { exporting = false; status = it }
                )
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = StudioGold,
        secondary = StudioGreen,
        background = StudioBackground,
        surface = StudioPanel,
        onPrimary = StudioBackground,
        onBackground = Color.White,
        onSurface = Color.White
    )) {
        Scaffold(
            containerColor = StudioBackground,
            topBar = {
                Column(Modifier.fillMaxWidth().background(StudioPanel).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("G.I KO KO VIDEO STUDIO PRO", color = StudioGold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("FULL OFFLINE EDITOR  •  Powered By I KO KO", color = StudioGreen, style = MaterialTheme.typography.labelSmall)
                }
            },
            bottomBar = {
                NavigationBar(containerColor = StudioPanel) {
                    listOf("Edit", "Audio", "Export").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Text(listOf("✂", "♫", "⇧")[index]) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    0 -> {
                        Text("VIDEO EDITOR", color = StudioGold, fontWeight = FontWeight.Bold)
                        if (videoUri == null) {
                            Surface(color = StudioPanel, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().height(210.dp)) {
                                Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) {
                                    Text("Your preview appears here", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Import a local video. No upload or Internet connection is used.", color = Color.LightGray)
                                }
                            }
                        } else {
                            AndroidView(
                                factory = { viewContext -> PlayerView(viewContext).apply { this.player = player; useController = true } },
                                update = { it.player = player },
                                modifier = Modifier.fillMaxWidth().height(220.dp)
                            )
                        }
                        Button(onClick = { videoPicker.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (videoUri == null) "＋ Import Video from Phone" else "Change Video")
                        }
                        Text(videoUri?.lastPathSegment ?: "No video selected", color = StudioGreen, maxLines = 2)
                        if (videoDurationMs > 0L) {
                            Text("Duration: " + String.format(Locale.US, "%.1f sec", videoDurationMs / 1000f), color = Color.LightGray)
                            Text("Trim start: " + String.format(Locale.US, "%.2f sec", trimStartMs / 1000f), color = StudioGold)
                            Slider(
                                value = trimStartMs.toFloat().coerceIn(0f, videoDurationMs.toFloat()),
                                onValueChange = { trimStartMs = it.toLong().coerceAtMost((trimEndMs - 500L).coerceAtLeast(0L)) },
                                valueRange = 0f..videoDurationMs.toFloat()
                            )
                            Text("Trim end: " + String.format(Locale.US, "%.2f sec", trimEndMs / 1000f), color = StudioGold)
                            Slider(
                                value = trimEndMs.toFloat().coerceIn(0f, videoDurationMs.toFloat()),
                                onValueChange = { trimEndMs = it.toLong().coerceAtLeast(trimStartMs + 500L).coerceAtMost(videoDurationMs) },
                                valueRange = 0f..videoDurationMs.toFloat()
                            )
                            OutlinedButton(onClick = { trimStartMs = 0L; trimEndMs = videoDurationMs; status = "Trim range reset." }, modifier = Modifier.fillMaxWidth()) {
                                Text("Reset Trim")
                            }
                        }
                        Text("Current offline core: local video preview, trim in/out, H.264 MP4 export and multi-track audio composition. Advanced visual effects, transitions, keyframes and draggable clip lanes are being added in later build stages.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                    1 -> {
                        Text("FULL AUDIO STUDIO", color = StudioGold, fontWeight = FontWeight.Bold)
                        Text("Audio tracks are imported locally and mixed together over the video. The selected EQ, compressor and noise gate are applied during export.", color = Color.LightGray)
                        Button(onClick = { audioPicker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Text("＋ Add Audio Tracks")
                        }
                        if (audioTracks.isEmpty()) {
                            Text("No external audio tracks added. The source video's audio will be retained.", color = Color.Gray)
                        } else {
                            audioTracks.forEachIndexed { index, track ->
                                Card(colors = CardDefaults.cardColors(containerColor = StudioPanel), modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text("AUDIO " + (index + 1), color = StudioGold, fontWeight = FontWeight.Bold)
                                            Text(track.label, color = Color.LightGray, maxLines = 2)
                                        }
                                        TextButton(onClick = { audioTracks.remove(track) }) { Text("Remove", color = Color(0xFFFF8A80)) }
                                    }
                                }
                            }
                        }
                        Text("MASTER VOLUME  " + (audioGain * 100).toInt() + "%", color = StudioGold)
                        Slider(value = audioGain, onValueChange = { audioGain = it }, valueRange = 0f..2f)
                        Text("3-BAND EQUALIZER", color = StudioGold, fontWeight = FontWeight.Bold)
                        AudioSlider("LOW", lowDb, -12f, 12f, { lowDb = it })
                        AudioSlider("MID", midDb, -12f, 12f, { midDb = it })
                        AudioSlider("HIGH", highDb, -12f, 12f, { highDb = it })
                        Text("COMPRESSOR  •  THRESHOLD " + compressorThreshold.toInt() + " dB", color = StudioGold)
                        Slider(value = compressorThreshold, onValueChange = { compressorThreshold = it }, valueRange = -30f..-3f)
                        Text("RATIO  " + String.format(Locale.US, "%.1f:1", compressorRatio), color = Color.LightGray)
                        Slider(value = compressorRatio, onValueChange = { compressorRatio = it }, valueRange = 1f..8f)
                        Text("NOISE GATE  •  " + noiseGateDb.toInt() + " dB", color = StudioGold)
                        Slider(value = noiseGateDb, onValueChange = { noiseGateDb = it }, valueRange = -80f..-30f)
                        Text("Noise Gate is a lightweight offline noise-reduction stage. It reduces low-level background noise; it is not AI source separation.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                    else -> {
                        Text("OFFLINE EXPORT", color = StudioGold, fontWeight = FontWeight.Bold)
                        Text("Export is performed on the phone using Android Media3 Transformer. No GitHub token, cloud renderer, API key or GPU service is used.", color = Color.LightGray)
                        Text("OUTPUT RESOLUTION", color = StudioGold, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("480p", "720p").forEach { item ->
                                FilterChip(selected = resolution == item, onClick = { resolution = item }, label = { Text(item) })
                            }
                        }
                        Text("H.264 video • AAC audio • MP4 container", color = StudioGreen)
                        Text("480p is recommended for Redmi 9A. 720p may take longer and use more memory. Export speed depends on source codec, clip length and hardware encoder support.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = {
                                if (videoUri == null) status = "Import a video first."
                                else if (videoDurationMs <= 0L) status = "Could not read video duration. Try another MP4 file."
                                else exportPicker.launch("GIKOKO_PRO_" + System.currentTimeMillis() + ".mp4")
                            },
                            enabled = !exporting && videoUri != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (exporting) "Exporting…" else "⇧ Export Offline MP4")
                        }
                        if (exporting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = StudioGreen)
                        Text("Export status: " + status, color = StudioGreen)
                    }
                }
                HorizontalDivider(color = StudioGreen.copy(alpha = 0.35f))
                Text(status, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                Text("Powered By I KO KO", color = StudioGreen, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AudioSlider(label: String, value: Float, min: Float, max: Float, onValue: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label + "  " + String.format(Locale.US, "%+.1f dB", value), color = Color.LightGray)
        Slider(value = value, onValueChange = onValue, valueRange = min..max)
    }
}

private fun readDurationMs(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (_: Exception) {
        0L
    } finally {
        try { retriever.release() } catch (_: Exception) {}
    }
}

internal data class StudioAudioSettings(
    val masterGain: Float,
    val lowDb: Float,
    val midDb: Float,
    val highDb: Float,
    val compressorThresholdDb: Float,
    val compressorRatio: Float,
    val noiseGateDb: Float
)

private fun startOfflineExport(
    context: Context,
    scope: CoroutineScope,
    videoUri: Uri,
    audioUris: List<Uri>,
    startMs: Long,
    endMs: Long,
    shortSide: Int,
    audioSettings: StudioAudioSettings,
    targetUri: Uri,
    onStatus: (String) -> Unit,
    onFinished: (String) -> Unit
) {
    val outputFile = File(context.cacheDir, "gikoko-offline-" + System.currentTimeMillis() + ".mp4")
    val clipping = MediaItem.ClippingConfiguration.Builder()
        .setStartPositionMs(startMs)
        .setEndPositionMs(endMs)
        .build()
    val videoItem = EditedMediaItem.Builder(
        MediaItem.Builder().setUri(videoUri).setClippingConfiguration(clipping).build()
    ).setEffects(
        Effects(
            listOf(StudioAudioProcessor(audioSettings)),
            listOf(Presentation.createForShortSide(shortSide))
        )
    ).build()

    val sequences = mutableListOf<EditedMediaItemSequence>()
    sequences.add(EditedMediaItemSequence.withAudioAndVideoFrom(listOf(videoItem)))
    audioUris.forEach { audioUri ->
        val audioItem = EditedMediaItem.Builder(MediaItem.fromUri(audioUri))
            .setEffects(Effects(listOf(StudioAudioProcessor(audioSettings)), emptyList()))
            .build()
        sequences.add(EditedMediaItemSequence.withAudioFrom(listOf(audioItem)))
    }

    val listener = object : Transformer.Listener {
        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
            onStatus("Encoding complete. Saving MP4 to phone storage…")
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { output ->
                        outputFile.inputStream().use { input -> input.copyTo(output) }
                    } ?: throw IllegalStateException("Could not open the selected output location.")
                    outputFile.delete()
                    withContext(Dispatchers.Main) { onFinished("Offline MP4 exported successfully.") }
                } catch (error: Exception) {
                    outputFile.delete()
                    withContext(Dispatchers.Main) { onFinished("Export finished but saving failed: " + (error.message ?: "unknown error")) }
                }
            }
        }

        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
            outputFile.delete()
            onFinished("Offline export failed: " + exportException.message.orEmpty().ifBlank { exportException.errorCodeName })
        }
    }

    try {
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()
        val composition = Composition.Builder(sequences).build()
        onStatus("Rendering locally. Keep the app open and avoid opening other heavy apps.")
        transformer.start(composition, outputFile.absolutePath)
    } catch (error: Exception) {
        outputFile.delete()
        onFinished("Could not start export: " + (error.message ?: "unknown error"))
    }
}
