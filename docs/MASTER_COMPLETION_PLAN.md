# G.I KO KO VIDEO STUDIO PRO — Master Completion Plan

## Product goal
A practical, offline-first Android video editor for Android 10+ and 32-bit ARMv7, with Redmi 9A as the low-end reference device. Editing and export must not require a cloud account, API key, GPU service or network connection.

## UX architecture
Use a home Feature Hub with four visually distinct animated 3D-style cards:
1. Video Studio — import, preview, timeline, trim, split, join, reorder, crop and rotate.
2. Watermark Studio — crop, blur, true pixel mosaic, region mask, tracking and optional offline AI inpainting.
3. Audio Studio — multitrack, waveform, per-track gain, fades, EQ, compressor and noise gate.
4. Export Studio — 480p/720p, MP4 H.264/AAC, progress, cancel and output location.

Each card opens a focused workspace. Keep advanced controls inside the relevant workspace instead of putting every control on the home screen. Use low-memory-friendly lists and lazy rendering.

## Feature status at v0.1.1 development
- Implemented in code: local video import/preview, trim in/out, H.264/AAC MP4 export, 480p/720p selection, audio import/mixing, master gain, 3-band EQ, compressor and noise gate.
- Implemented in code: animated Feature Hub, crop export effect, Gaussian blur effect and blur-strength-based conceal mode.
- Not implemented: true pixel mosaic, movable region mask, watermark tracking, AI inpainting/model runtime, independent audio offsets, track waveforms, split/reorder timeline, transitions, overlays, subtitles, keyframes, speed ramp, reverse and PIP.
- Not verified: successful build after latest fixes and physical-device playback/export on Redmi 9A.

## Delivery gates
1. Build gate: GitHub Actions assembleDebug must succeed and produce the ARMv7 APK.
2. Static correctness gate: inspect manifest, ABI, permissions, Media3 effect configuration, file URI handling and error paths.
3. Functional gate: import a short MP4, preview, trim, export 480p, re-import result and verify audio/video sync.
4. Watermark gate: test crop/blur/mosaic against sample footage; add region masks and tracking only when they can be applied to actual frame coordinates during export.
5. Audio gate: test same-channel and differing-channel audio sources, duration clipping, per-track start offsets and export sync.
6. Device gate: install and test on Redmi 9A Android 10. Use 480p by default, limit concurrent decode/encode work, handle cancellation and low storage.
7. Release gate: only label features as complete after tests pass. Provide a signed release only after signing configuration is intentionally set up.

## Engineering constraints
- Keep ABI filter at armeabi-v7a and minSdk 29 unless a deliberate compatibility change is documented.
- Avoid shipping a large AI model inside the base APK. If an offline model is viable, make it an optional model download with a visible size estimate, checksum, storage check and explicit user consent.
- Do not market Gaussian blur as true mosaic or claim AI removal when no model is integrated.
- Watermark removal must be used only on media the user owns or is authorized to modify.
- Prefer reliable local Media3 processing over unsupported promises of a full desktop-class editor on a low-RAM phone.

## Definition of done
A testable APK with all advertised controls wired to real export operations, clear unavailable-feature labels, successful GitHub Actions build, and a documented Redmi 9A test checklist. Physical-device validation remains necessary; CI success alone does not prove runtime compatibility.
