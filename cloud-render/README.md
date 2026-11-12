# G.I KO KO Cloud Video Renderer

Free-tier-friendly cloud rendering prototype using Remotion + GitHub Actions.

## Output profiles

| Profile | Landscape | Portrait |
|---|---:|---:|
| 360p | 640×360 | 360×640 |
| 480p | 854×480 | 480×854 |
| 720p | 1280×720 | 720×1280 |

Output is H.264 MP4 for broad Android compatibility.

## Render on GitHub

1. Open the repository's **Actions** tab.
2. Select **G.I KO KO Remotion Video Render**.
3. Press **Run workflow**.
4. Enter a title and script, choose resolution/orientation, and set duration (5–300 seconds).
5. Wait for the job to finish.
6. Download the MP4 from the workflow run's **Artifacts** section.

No Remotion API key is required. This workflow uses GitHub-hosted CPU runners; it is not GPU rendering and speed depends on runner availability, video duration, and effects. GitHub Actions quotas and usage limits still apply. Artifact files are temporary (3-day retention).

## Local development

```bash
npm install
npx remotion studio src/index.ts
```

## Important scope

This first milestone renders a branded animated title/script video. It does not yet upload media from the Android app, generate AI images, synthesize narration, or provide a public render API. Those require separate integration work. Existing Android source files are left unchanged.

## License note

Remotion's free license is subject to its current eligibility and terms. Review https://www.remotion.dev/license before commercial or multi-user deployment. This repository does not include a Remotion API key or paid rendering service.
