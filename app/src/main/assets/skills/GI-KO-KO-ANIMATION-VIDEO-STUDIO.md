# G.I KO KO Animation & Video Studio — Built-in Claude Skill

This skill is bundled with G.I KO KO AI VIDEO STUDIO. It instructs a connected Claude/AI assistant how to plan and produce 2D/3D animation, documentary, explainer, cinematic and social videos.

## Default production contract
- Vertical 9:16 output by default: 1080×1920 master, with 720×1280 or 480×854 fallbacks.
- Natural English voiceover, plus synchronized English and Myanmar Unicode subtitles.
- Separate English and Myanmar SRT files.
- G.I logo and exact “Powered by I ko ko” credit, positioned in safe areas and never covering subtitles, faces or key action.
- Separate 9:16 thumbnail.
- MP4 H.264/yuv420p/AAC output suitable for Android.
- Deliver script, storyboard, sources, assets/project and a README with the MP4.

## Supported production modes
2D character/cartoon/vector animation; 3D scenes, product visualization and environments; documentary (history, science, nature, technology, biography, travel); explainer and educational content; cinematic stories and short films; social shorts/Reels/Shorts/TikTok; infographic, whiteboard, kinetic typography, maps, timelines and data visualization.

## Required production workflow
1. Plan scenes with durations, shot sizes, camera movement, visuals, narration, sound and transitions.
2. For documentaries, research and cite claims; distinguish fact, interpretation, reconstruction and fiction.
3. Keep character, location, wardrobe, lighting and visual continuity consistent.
4. Write natural spoken English narration and natural Myanmar translation.
5. Generate English voice with a configured TTS provider or supplied recording. Check cost, quota and privacy first; never pretend an unconfigured TTS engine has produced audio.
6. Time both subtitle languages against the actual narration. Use Myanmar-capable Unicode fonts.
7. Render real animation/video assets; do not label a static text-card prototype as a completed animation.
8. Add logo and “Powered by I ko ko” in separate safe areas; verify neither overlaps captions.
9. Create a separate branded thumbnail.
10. Verify video dimensions, aspect ratio, audio, subtitles, glyph rendering, duration and playback before reporting completion.

## Quality and safety rules
- Use licensed, public-domain or user-provided media and music; document attribution when required.
- Do not invent documentary sources, quotes, facts or archive footage.
- Keep API keys out of code and logs. Use configured secrets and explain external provider requirements.
- Prefer preview renders, then final masters. Keep source assets unchanged.
- For Android compatibility use H.264, yuv420p and AAC. Default to 9:16.
- A skill is an instruction set, not a voice provider, image generator, 3D engine or GPU. Use only tools that are actually configured.

## Project deliverables
final-9x16.mp4; thumbnail-9x16.png; subtitles/english.srt; subtitles/myanmar.srt; narration scripts; storyboard; documentary source ledger; project files; README.
