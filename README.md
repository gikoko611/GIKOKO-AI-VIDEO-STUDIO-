# G.I KO KO AI VIDEO STUDIO

Android-first AI video creator for Android 10+ and ARMv7 devices, with cloud-assisted rendering.

## Architecture

- **Android app:** Kotlin + Jetpack Compose. Minimum Android API 29 (Android 10).
- **AI providers:** User-configurable OpenAI-compatible providers (custom base URL, API key, model ID), plus provider presets.
- **Model discovery:** Fetch models from a provider's `/models` endpoint where supported. A separate Free Model Finder queries public model catalogs (initially OpenRouter) and filters models advertised as free. Free availability and quotas can change.
- **Cloud API:** FastAPI service for job creation, status, and result metadata.
- **Video engine:** Python + FFmpeg worker. The worker downloads permitted media, creates voiceover/subtitles, assembles scenes, and exports MP4.
- **Build:** GitHub Actions builds a debug APK artifact.

## Cloud rendering plan (cost-conscious)

The initial prototype should keep the API and rendering worker separate. A free web-service tier can host a lightweight API, but it is not a reliable always-on video worker. Render's free web services sleep after 15 minutes idle, have ephemeral storage, and do not support one-off jobs; background workers are not available on the free instance tier. See https://render.com/docs/free.

For an initial $0 test workflow, use a manually started Google Colab notebook as the FFmpeg rendering worker (free GPU availability is not guaranteed; CPU rendering is possible). For unattended production rendering, move the worker to a paid background-worker/compute service or another provider with suitable free compute and persistent storage. Do not promise unlimited free rendering.

## Planned features

1. Script generation and scene breakdown.
2. Pinterest / Pexels / Pixabay media search and import (respect each source's terms and asset licenses).
3. Edge TTS voiceover; test voice/language availability at runtime.
4. Myanmar and English subtitle tracks.
5. FFmpeg video assembly, transitions, music, watermark, 9:16 / 16:9 / 1:1 export.
6. Provider settings, connection test, model list, and free-model discovery.
7. Cloud job queue, progress, cancellation, and download.

## Build APK

Push to GitHub, then open **Actions → Android APK Build**. Download the generated APK from the workflow run's Artifacts section.

## License and attribution

This project is a new implementation. When adapting VUZA code or its Pinterest scraping feature, preserve the applicable MIT license and required attribution. Only use media that you are authorized to download, edit, and publish.
