# G.I KO KO AI VIDEO STUDIO

Android-first AI video creator for Android 10+ and ARMv7 devices, with two cloud rendering routes.

## Android app

- Kotlin + Jetpack Compose.
- Minimum Android API 29 (Android 10).
- ARMv7 target: `armeabi-v7a`.
- Render engine selector: GitHub Actions Cloud or Google Colab.

## Cloud rendering engines

### 1. GitHub Actions CPU + FFmpeg

Workflow: **Actions → Cloud Video Render (CPU) → Run workflow**.

Inputs:
- A direct downloadable MP4 URL (not a normal watch-page URL).
- Aspect ratio: 9:16, 16:9, or 1:1.
- Optional watermark (default: Powered by I KO KO).

The workflow downloads the source, uses FFmpeg/libx264 to render an MP4, then uploads `output.mp4` as a workflow artifact. Download the artifact from the completed run. This runner is CPU-based; it does not provide a guaranteed GPU. Free Actions minutes and artifact retention/usage limits apply.

### 2. Google Colab T4

Open `cloud-renderer/GIKOKO_Colab_Renderer.ipynb` in Google Colab. Upload a source video, choose the aspect ratio and watermark, run the cells, and download the rendered MP4. Colab may assign a T4 GPU, but free GPU availability, session duration, and quotas are not guaranteed. The notebook automatically uses GPU encoding only if an NVIDIA GPU is actually available; otherwise it falls back to CPU encoding.

## Current integration status

The Android app currently lets the user select and save a preferred render engine locally. The two cloud rendering workflows are provided in this repository. **Direct APK-to-cloud job submission, job progress polling, and in-app MP4 download are not yet connected**; those require the next backend/API integration phase. The GitHub Actions workflow currently accepts a direct downloadable source MP4 URL.

## AI providers and media

- Planned provider settings: custom base URL, API key, model ID, connection test, and model discovery.
- Free model discovery can identify models advertised as free, but provider quotas and access policies can change.
- Use only media you are authorized to download, edit, and publish. Respect Pinterest, Pexels, Pixabay, and other source terms.

## Build APK

Push to GitHub, then open **Actions → Android APK Build**. Download the generated APK from the workflow run's Artifacts section.

## License and attribution

This project is a new implementation. When adapting VUZA code or its Pinterest scraping feature, preserve the applicable MIT license and required attribution.
