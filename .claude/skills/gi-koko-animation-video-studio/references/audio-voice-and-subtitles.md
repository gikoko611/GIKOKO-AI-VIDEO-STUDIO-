# Voice, audio mix and bilingual subtitle engineering

## English narration
Write for speech. Use contractions naturally, pronounceable phrasing and pauses around names/numbers. Maintain one consistent voice identity. For factual work, pronounce dates, units and names carefully. Keep a pronunciation glossary.

## TTS adapter
Before using a TTS provider, inspect its current API docs, supported languages/voices, rate limits, cost and data handling. Keep credentials in environment variables or secret stores, never in source code. Add retries with bounded backoff and cache generated audio by a hash of text + voice + provider settings. Do not claim a provider is free without verifying current terms.

## Audio pipeline
Keep narration as the anchor. Remove accidental long silences, apply gentle high-pass/EQ and compression only when needed, loudness-normalize consistently, duck music under speech and avoid clipping. Check mono phone playback as well as stereo. Music and sound effects must be licensed or user supplied.

## Bilingual captions
Maintain one canonical timeline with cue IDs and start/end times. Generate English and Myanmar SRT from that timeline. Ensure both are UTF-8; use a Myanmar-capable font such as Noto Sans Myanmar where legally available. Validate no negative, overlapping or out-of-duration cues. Keep captions concise, readable and away from UI overlays. For simultaneous bilingual display use two visually distinct lines; if too dense, create selectable language tracks while retaining both SRT files.

## Sync checks
Compare subtitle cues against actual speech, not just script duration. Re-time captions after any narration edit. Check the first, middle and final cues and inspect rendered frames for missing Myanmar glyphs.
