# G.I KO KO AI VIDEO STUDIO

Android-first AI video production app targeting Android 10+ and 32-bit ARMv7 devices.

## Android app status

The current development build includes:
- Jetpack Compose app shell with Create, Media, Voice, and Settings sections.
- OpenAI-compatible provider configuration (HTTPS base URL, API key, model ID).
- Provider connectivity check using the `/models` endpoint.
- Model discovery and selectable model IDs.
- AI script generation through `/chat/completions`.
- On-device media picker for video, audio, and image files.
- Editable subtitle text draft derived from the generated script.
- Android system Text-to-Speech preview.
- API keys are held in memory only and are not written to preferences.

The API provider must support the OpenAI-compatible `/models` and `/chat/completions` endpoints. Model availability, pricing, free quotas, and rate limits are controlled by each provider.

## Not yet implemented in the Android app

These features are still required before calling this a complete v1.0.0 release:
- Direct upload of selected media to a cloud renderer.
- GitHub Actions workflow dispatch, job status polling, and in-app artifact download.
- Colab job hand-off from inside the app.
- Generated MP3/WAV voice export.
- Timed SRT/VTT subtitle generation and subtitle burn-in.
- A full timeline editor, transitions, overlays, and final MP4 export workflow.
- End-to-end tests on physical Android 10 / ARMv7 hardware.

Do not treat the current development APK as the finished v1.0.0 release.

## Cloud rendering routes

### GitHub Actions CPU + FFmpeg

Workflow: **Actions → Cloud Video Render (CPU) → Run workflow**.

Inputs:
- A direct downloadable MP4 URL (not a normal watch-page URL).
- Aspect ratio: 9:16, 16:9, or 1:1.
- Optional watermark (default: Powered by I KO KO).

The workflow renders an MP4 using FFmpeg/libx264 and uploads it as an artifact. The runner is CPU-based; a GPU is not guaranteed. Free Actions minutes and artifact retention/usage limits apply.

### Google Colab

Open `cloud-renderer/GIKOKO_Colab_Renderer.ipynb` in Google Colab. Upload a source video, select the aspect ratio and watermark, run the cells, and download the rendered MP4. A free T4 GPU is not guaranteed; the notebook falls back to CPU encoding.

## Build APK

Push to GitHub, then open **Actions → Android APK Build**. Download the generated debug APK from the workflow run's Artifacts section. The debug APK is intended for testing and is not a production-signed release.

## Device target

- Minimum Android API 29 (Android 10).
- ABI filter: `armeabi-v7a`.
- Kotlin + Jetpack Compose.
- Java/Kotlin JVM target 17.

## Media rights

Only import and process media you own or are authorized to use. Follow the terms of service for any media provider or hosting platform.

## License and attribution

When adapting third-party code, preserve its applicable license and required attribution.
