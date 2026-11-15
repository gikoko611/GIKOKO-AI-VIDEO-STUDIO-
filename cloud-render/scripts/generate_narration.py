#!/usr/bin/env python3
import json, pathlib, subprocess, sys, time

ROOT = pathlib.Path(__file__).resolve().parents[1]
segments = json.loads((ROOT / "assets/narration_segments.json").read_text(encoding="utf-8"))
out_dir = ROOT / "public"
tmp_dir = ROOT / "tmp_voice"
out_dir.mkdir(exist_ok=True)
tmp_dir.mkdir(exist_ok=True)
voice = "en-US-AriaNeural"
inputs = []
filters = []
labels = []

for index, item in enumerate(segments):
    clip = tmp_dir / f"voice_{index:02d}.mp3"
    command = ["edge-tts", "--voice", voice, "--rate", "+0%", "--text", item["text"], "--write-media", str(clip)]
    last_error = None
    for attempt in range(3):
        result = subprocess.run(command, capture_output=True, text=True)
        if result.returncode == 0 and clip.exists() and clip.stat().st_size > 1000:
            last_error = None
            break
        last_error = result.stderr[-1200:]
        time.sleep(3 * (attempt + 1))
    if last_error:
        raise RuntimeError(f"Voice generation failed for segment {index}: {last_error}")
    inputs += ["-i", str(clip)]
    delay = int(float(item["start"]) * 1000)
    label = f"v{index}"
    filters.append(f"[{index}:a]adelay={delay}|{delay},volume=1.0[{label}]")
    labels.append(f"[{label}]")

graph = ";".join(filters) + ";" + "".join(labels) + f"amix=inputs={len(labels)}:duration=longest:dropout_transition=0,apad,atrim=0:300[out]"
output = out_dir / "narration.mp3"
cmd = ["ffmpeg", "-y", *inputs, "-filter_complex", graph, "-map", "[out]", "-c:a", "libmp3lame", "-b:a", "160k", "-t", "300", str(output)]
subprocess.run(cmd, check=True)
print(f"Created {output} ({output.stat().st_size} bytes)")
