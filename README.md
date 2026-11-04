# G.I KO KO AI VIDEO STUDIO

Android-first AI video production app targeting Android 10+ and 32-bit ARMv7 devices.

## Implemented in the current development build

- Jetpack Compose app shell with Create, Media, Voice, Render, and Settings sections.
- OpenAI-compatible provider configuration (HTTPS base URL, API key, model ID).
- Provider connectivity check using the `/models` endpoint.
- Model discovery and selectable model IDs.
- AI script generation through `/chat/completions`.
- On-device media picker for video, audio, and image files.
- Editable subtitle text and SRT export. SRT timing is estimated from text length; it is not speech-aligned.
- Android system Text-to-Speech preview and WAV narration export (English voice depends on installed Android TTS voice data).
- GitHub Actions cloud-render dispatch from the Android app.
- Latest GitHub Actions render status check and link to the workflow run.
- In-app download of the rendered MP4 artifact, including extraction from GitHub's artifact ZIP into a user-selected phone storage location.
- Cloud FFmpeg options for 9:16 / 16:9 / 1:1 output, watermark, optional voiceover audio, optional SRT subtitle burn-in, and start/end trimming.
- API keys and GitHub token are kept in memory only and are not written to preferences.

The AI provider must support the OpenAI-compatible `/models` and `/chat/completions` endpoints. Model availability, pricing, free quotas, and rate limits are controlled by each provider.

## Cloud render workflow

Open the app's **Render** tab and provide:
- A direct HTTPS download URL for the source MP4.
- Optional direct HTTPS URL for an MP3/WAV voiceover.
- Optional direct HTTPS URL for an SRT subtitle file.
- Optional trim start/end values in seconds.
- A GitHub fine-grained personal access token limited to this repository with **Actions: Read and write** and **Metadata: Read** permissions.
- Output aspect ratio.

Tap **Start Cloud Render**, then **Check latest render job**. When the job finishes, use **Download MP4 into phone storage** to save the MP4 directly through Android's file picker. The app retrieves the GitHub artifact ZIP and extracts the MP4.

The source media must already be hosted at a direct downloadable URL. A file selected from the phone's local storage is not automatically uploaded to GitHub or a hosting provider. Do not paste or share your GitHub token with anyone. The app holds it in memory only; closing/restarting the app clears it.

The renderer uses GitHub-hosted CPU FFmpeg. GPU is not guaranteed. GitHub Actions minutes, artifact retention, and repository limits apply. The artifact retention period is three days.

## Google Colab

Open `cloud-renderer/GIKOKO_Colab_Renderer.ipynb` in Google Colab. Upload a source video, select the aspect ratio and watermark, run the cells, and download the rendered MP4. A free T4 GPU is not guaranteed; the notebook falls back to CPU encoding.

## Not yet implemented

These features are still required before calling this a complete v1.0.0 release:
- Automatic upload of selected phone media to cloud storage.
- Full multitrack timeline editor with drag/drop clip arrangement, transitions, and overlays.
- Precise speech-aligned subtitles and subtitle timing editor.
- Direct local MP4 export without a cloud render.
- MP3 voice export; current on-device TTS export is WAV.
- End-to-end tests on physical Android 10 / ARMv7 hardware.
- Production release signing and release APK validation.

Do not treat the current development APK as a finished production v1.0.0 release.

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
