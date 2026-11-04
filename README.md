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


## Text-to-Video (Wan 3.0)

The **Create** tab includes a real prompt-to-MP4 workflow powered by `alibaba/wan-3.0/text-to-video` on fal.ai. It is separate from the LLM script generator.

### One-time setup

1. Create/sign in to a fal.ai account and create an API key.
2. In this GitHub repository, open **Settings → Secrets and variables → Actions → New repository secret**.
3. Name the secret exactly `FAL_KEY` and paste the fal.ai API key as its value.
4. In the app's Create tab, enter a GitHub fine-grained token with **Actions: Read and write** and **Metadata: Read** permissions.

The fal.ai key is used only inside GitHub Actions and is never entered into the Android app. Do not place it in source code or commit it to GitHub.

### Generate a clip

- Enter a prompt describing the subject, action, camera movement, lighting, visual style, and desired sound.
- Select 5, 10, or 15 seconds, resolution (480p / 720p / 1080p), aspect ratio, and synchronized audio.
- Tap **Generate AI Video from Prompt**.
- Tap **Check AI Video Generation** after a short wait. Open the job link to inspect logs if needed.
- When the run succeeds, tap **Download AI-generated MP4**.

The workflow waits for fal.ai's queued generation, downloads the returned MP4, validates it with FFprobe/FFmpeg, and uploads it as a GitHub Actions artifact retained for three days.

### Cost and limits

This is a real hosted video-generation API, not local rendering. fal.ai's Wan 3.0 API is pay-per-use; the API price depends on resolution and duration and can change. The five daily free Wan 3.0 generations described by fal.ai are available in its browser sandbox and are not automatically included in API usage. Review the current price in fal.ai before triggering a generation. GitHub Actions minutes and artifact limits also apply.

The app does not claim that text-to-video generation is free or unlimited. The first generation should be a short test clip (5 seconds, 480p) so the user can confirm the API setup and review the charge before producing longer clips.

## Free GPU Text-to-Video (Wan2.1)

The **Create** tab has a **FREE GPU TEXT-TO-VIDEO** section that opens the open-source Wan2.1 T2V-1.3B Google Colab notebook. It copies the current video prompt to the Android clipboard so it can be pasted into the notebook.

Notebook: [GIKOKO_Wan2_1_Free_T2V.ipynb](cloud-renderer/GIKOKO_Wan2_1_Free_T2V.ipynb)

### Free workflow

1. Open the notebook in Google Colab from the Android app.
2. In Colab, select **Runtime → Change runtime type → T4 GPU** if a free GPU is offered.
3. Run the notebook cells from top to bottom. The first run downloads the open-source model and can take time.
4. Paste the copied prompt into the prompt field and generate the short 480p MP4.
5. Download the MP4, return to the Android app, and choose **Media → Import media**.

### Important limitations

- This notebook uses the Wan-AI/Wan2.1-T2V-1.3B-Diffusers open-source model and does not call fal.ai, does not require FAL_KEY, and does not use a paid inference API.
- Colab Free does not guarantee GPU availability, T4 access, session duration, or a fixed daily quota. Generation can fail if the runtime disconnects or runs out of GPU memory.
- The notebook targets short 480p clips (832×480). It does not promise 720p, audio generation, or unlimited video generation.
- The Android app hands off the prompt and opens Colab; it does not run the model on the Redmi 9A. After the notebook downloads the MP4, import it manually through Media.
- The existing **OPTIONAL PAID API MODE • WAN 3.0** remains separate. It requires FAL_KEY and may incur charges. Do not use that button for the free workflow.

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
