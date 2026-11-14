import fs from "node:fs/promises";
import { Client, handle_file } from "@gradio/client";

const prompt = (process.env.LTX_PROMPT || "").trim();
if (!prompt) throw new Error("LTX_PROMPT is required.");

const imageUrl = (process.env.LTX_IMAGE_URL || "").trim();
const orientation = process.env.LTX_ORIENTATION === "landscape" ? "landscape" : "portrait";
const duration = Math.min(5, Math.max(1, Number(process.env.LTX_DURATION || 2)));
const seed = Math.max(0, Math.min(2147483647, Number(process.env.LTX_SEED || 42)));
const width = orientation === "portrait" ? 832 : 1472;
const height = orientation === "portrait" ? 1472 : 832;

console.log("Connecting to public Lightricks LTX-2.5 ZeroGPU Space...");
const options = { events: ["status", "data"] };
if (process.env.HF_TOKEN) options.token = process.env.HF_TOKEN;
const app = await Client.connect("Lightricks/LTX-2.5", options);

console.log("Submitting LTX-2.5 job...");
console.log(`Mode: ${imageUrl ? "Image-to-Video" : "Text-to-Video"} | ${orientation} | ${duration}s`);

const imageInput = imageUrl ? handle_file(imageUrl) : null;
const job = app.submit("/run", [
  prompt, imageInput, height, width, duration, seed,
  "conv", false, false, false
]);

let outputData = null;
for await (const message of job) {
  if (message.type === "status") {
    const status = message;
    const stage = status.stage || status.status || "working";
    const position = Number.isInteger(status.position) ? ` | queue position: ${status.position}` : "";
    const eta = Number.isFinite(status.eta) ? ` | ETA: ${Math.ceil(status.eta)}s` : "";
    console.log(`[LTX-2.5] ${stage}${position}${eta}`);
    if (status.stage === "error" || status.success === false) {
      throw new Error(status.message || "LTX-2.5 generation failed.");
    }
  }
  if (message.type === "data") outputData = message.data;
}

if (!outputData) throw new Error("The Space returned no output data.");
const video = outputData[0];
const videoUrl = typeof video === "string" ? video : video?.url;
if (!videoUrl) {
  console.error("Unexpected video output:", JSON.stringify(video));
  throw new Error("No downloadable video URL was returned by the LTX-2.5 Space.");
}

const response = await fetch(videoUrl);
if (!response.ok) throw new Error(`Video download failed: HTTP ${response.status}`);
const bytes = Buffer.from(await response.arrayBuffer());
if (bytes.length < 1024) throw new Error("Downloaded output is unexpectedly small.");

await fs.mkdir("out", { recursive: true });
await fs.writeFile("out/GI-KO-KO-LTX-2.5.mp4", bytes);
await fs.writeFile("out/generation-info.json", JSON.stringify({
  model: "Lightricks/LTX-2.5",
  mode: imageUrl ? "image-to-video" : "text-to-video",
  orientation, durationSecondsRequested: duration, seed, prompt,
  bytes: bytes.length, generatedAt: new Date().toISOString()
}, null, 2));
console.log(`SUCCESS: out/GI-KO-KO-LTX-2.5.mp4 (${(bytes.length / 1024 / 1024).toFixed(2)} MB)`);
