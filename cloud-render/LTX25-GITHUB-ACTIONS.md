# G.I KO KO — LTX-2.5 Free ZeroGPU workflow

This workflow adds an LTX-2.5 generation path without a ComfyUI Cloud API key or payment card.

## How it works

- GitHub Actions is the controller only (CPU runner).
- Video inference runs on the public `Lightricks/LTX-2.5` Hugging Face Space using its shared ZeroGPU queue.
- The workflow downloads the returned MP4 and saves it as a GitHub Actions artifact.
- An image URL switches the request to image-to-video. Leave it empty for text-to-video.

## Run

1. Open the repository's **Actions** tab.
2. Select **G.I KO KO LTX-2.5 Free ZeroGPU Render**.
3. Press **Run workflow**.
4. Enter a prompt. For I2V, provide a publicly accessible JPG/PNG URL.
5. Choose portrait/landscape and 1–5 seconds.
6. When complete, download the MP4 from **Artifacts**.

## Free-tier limits

This uses the official public Hugging Face Space, not a private GPU server. Hugging Face documents a free-account ZeroGPU quota of 5 GPU minutes per day; anonymous visitors receive 2 minutes. Queue availability and per-call reservations can still prevent a run, and the Space may return “no GPU available” or quota errors. A free HF account is recommended. No credit card is required for using an existing public ZeroGPU Space.

The official LTX-2.5 Space requests an xlarge ZeroGPU allocation, which consumes quota at 2x. Keep clips short and retry later if the shared queue is busy. This is not unlimited/free-at-any-time rendering.

## Optional HF token

The workflow works without a token by using the public Space as an anonymous visitor. To associate requests with your own free Hugging Face account quota, optionally add a read token as a repository Actions secret named `HF_TOKEN`. This is optional and is not a paid API key. Never put it in source code.

## Scope

This GitHub Actions milestone generates a clip and returns an artifact. It does not yet add an in-app upload/progress/download screen or persistent job API to the Studio UI. The repository's current cloud-render milestone is a Remotion renderer; this LTX workflow is a separate AI generation path.

## Official references

- https://huggingface.co/spaces/Lightricks/LTX-2.5
- https://huggingface.co/docs/hub/spaces-zerogpu
- https://gradio.app/docs/js-client
