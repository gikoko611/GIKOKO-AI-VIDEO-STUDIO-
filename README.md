# G.I KO KO AI STUDIO
A new Android 10+ ARMv7 AI workspace with Midnight Gold / Luxury Gold Cyber UI.

## Included in this foundation
- OpenAI-compatible Chat Completions, Anthropic Messages and Gemini generateContent adapters.
- API provider settings; API credentials encrypted with Android Keystore.
- TXT / ZIP skill import into private app storage (ZIP scripts are not executed).
- MCP endpoint registration and JSON-RPC initialize test.
- Local video playback and save-copy through Android's document picker.
- ARMv7-only APK target, minSdk 29.

## Important capability boundaries
- Arbitrary provider APIs are not all identical. Select the correct API format and model ID.
- MCP servers vary in transport, authentication and protocol version. This foundation registers and tests endpoints; complete tool discovery, user approvals and tool invocation are subsequent integration work.
- Text-only LLMs create storyboards/prompts, not MP4 files. Actual AI video generation requires a dedicated video API or a local rendering engine.

## Build
GitHub Actions -> Android APK Build -> Run workflow.
