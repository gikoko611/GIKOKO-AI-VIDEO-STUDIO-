# G.I KO KO VIDEO STUDIO PRO

A native Android offline-first video editor targeting Android 10+ and 32-bit ARMv7 devices.

## Product direction

The Android app is now focused on offline video editing. Prompt-to-video, paid video-generation APIs, GitHub Actions rendering, API keys, and cloud uploads are not part of the active app interface.

## Current Offline Core (v0.1.1 development build)

- Native Kotlin + Jetpack Compose editor interface.
- Local video import through Android Storage Access Framework.
- Local video preview with Media3 ExoPlayer.
- Trim start/end controls.
- Feature Hub with separate animated Video, Watermark, Audio and Export sections.
- Basic export-time crop, full-frame Gaussian blur and stronger blur-based mosaic-style conceal tools.
- Offline H.264 MP4 export through AndroidX Media3 Transformer.
- 480p and 720p output scaling while preserving source aspect ratio.
- Local multi-track audio composition: imported audio tracks are overlaid and mixed with the source video's audio from the beginning of the project.
- Offline audio controls applied during export: master gain, 3-band EQ, compressor and a lightweight noise gate.
- No API key, GitHub token, GPU service, cloud upload or Internet connection is required for the active editing workflow.

## Audio processing notes

The built-in audio processor works on decoded PCM16 audio. The noise gate reduces low-level noise; it is not neural noise removal or source separation. Imported audio sequences currently begin at project time zero. The current blur and mosaic-style effects affect the full frame; they do not yet isolate a user-selected rectangle. The AI remover is a planned capability, not implemented in the current APK. Per-track timeline offsets, independent clip trimming, automation, waveform editing and a full multitrack mixer UI are planned follow-up work. Media3 composition can mix overlapping audio sequences into the output audio track.

## Device compatibility

- Target: Redmi 9A / Android 10 / ARMv7 32-bit.
- Minimum API: 29.
- ABI filter: armeabi-v7a.
- 480p is the recommended export setting for the target device.
- 720p export depends on the source codec and available hardware/software encoders and may be slow.
- Keep the app in the foreground during export on low-memory devices.

## Planned next editor stages

- Drag-and-drop multitrack timeline with independent clip start/end positions.
- Video clip splitting, joining and reordering.
- Video filters, transitions, crop/rotate and overlays.
- Text, subtitles, watermark and picture-in-picture layers.
- Keyframes, speed control and reverse.
- Audio waveform, per-track gain/pan, fades and better noise reduction.
- Region-selectable watermark masks, true pixel mosaic and tracked masks.
- Offline AI inpainting model integration (only after model size, licensing and ARMv7 memory costs are validated).
- Physical-device tests on Redmi 9A before calling the app production-ready.

## Build APK

Push to GitHub, then open **Actions → Android APK Build**. Download the generated debug APK from the workflow run's Artifacts section. The debug APK is for testing and is not a production-signed release.

## Licensing

The editor uses AndroidX Media3 under its applicable license. Only edit media you own or are authorized to use.
