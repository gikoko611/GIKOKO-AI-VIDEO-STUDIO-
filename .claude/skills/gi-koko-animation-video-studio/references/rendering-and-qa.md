# Render engineering and quality assurance

## Render profiles
- Vertical short: 9:16, 1080×1920 master; 720×1280 or 480×854 fallback.
- Landscape documentary: 16:9 only when requested or required.
- H.264, yuv420p, AAC, MP4 fast-start for Android compatibility.
- Keep project frame rate consistent with source footage where possible.

## Render reliability
Use a low-resolution preview before the master. Render in deterministic scene chunks for long projects, then concatenate with matching codecs/time bases. Keep intermediate assets, logs and final exports separate. Use bounded retries for transient download/render errors. Validate available disk space and runner limits before starting.

## Thumbnail and metadata
Create a separate high-resolution thumbnail with one clear focal subject, concise title and safe branding. Avoid covering faces or important details. Use metadata title/description only when requested.

## Technical QA
Use ffprobe to check codec, dimensions, frame rate, duration, pixel format and audio stream. Decode the full video to detect corrupt frames. Inspect frames from start, middle and end. Listen to the opening, a middle passage and ending. Confirm subtitle timing and Unicode rendering. Verify the logo and credit do not overlap subtitles or key action.

## Honest status
A script, storyboard, title-card render, preview or mockup is not a finished video. State precisely which deliverables exist and which steps remain. Never fabricate file paths, render completion or tests.
