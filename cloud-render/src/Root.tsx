import React from 'react';
import {
  AbsoluteFill,
  Composition,
  Sequence,
  interpolate,
  spring,
  useCurrentFrame,
  useVideoConfig,
} from 'remotion';

type Props = {
  title: string;
  script: string;
  resolution: '360p' | '480p' | '720p';
  orientation: 'landscape' | 'portrait';
  durationSec: number;
};

const dimensions = (resolution: Props['resolution'], orientation: Props['orientation']) => {
  const sizes = {
    '360p': {width: 640, height: 360},
    '480p': {width: 854, height: 480},
    '720p': {width: 1280, height: 720},
  } as const;
  const size = sizes[resolution] ?? sizes['480p'];
  return orientation === 'portrait'
    ? {width: size.height, height: size.width}
    : size;
};

const GoldScene: React.FC<Props> = ({title, script}) => {
  const frame = useCurrentFrame();
  const {fps, durationInFrames} = useVideoConfig();
  const intro = spring({frame, fps, config: {damping: 18, stiffness: 90}});
  const fade = interpolate(frame, [0, 18, durationInFrames - 18, durationInFrames], [0, 1, 1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  const words = script.trim().split(/\s+/).filter(Boolean);
  const chunkSize = Math.max(1, Math.ceil(words.length / Math.max(1, Math.ceil(durationInFrames / (fps * 6)))));
  const scenes: string[] = [];
  for (let i = 0; i < words.length; i += chunkSize) scenes.push(words.slice(i, i + chunkSize).join(' '));
  if (scenes.length === 0) scenes.push('Your story begins here.');
  const sceneDuration = Math.max(1, Math.floor(durationInFrames / scenes.length));
  const activeIndex = Math.min(scenes.length - 1, Math.floor(frame / sceneDuration));
  const activeText = scenes[activeIndex];

  return (
    <AbsoluteFill style={{background: 'radial-gradient(ellipse at 50% 42%, #493018 0%, #1b1009 52%, #080604 100%)', color: '#fff4dc', fontFamily: 'Arial, sans-serif', opacity: fade}}>
      <AbsoluteFill style={{border: '2px solid rgba(255,190,82,.55)', margin: 18, borderRadius: 18}} />
      <AbsoluteFill style={{justifyContent: 'center', alignItems: 'center', padding: '8%', textAlign: 'center'}}>
        <div style={{fontSize: 18, letterSpacing: 8, color: '#e9b85e', marginBottom: 28, transform: `translateY(${(1-intro)*24}px)`}}>G.I KO KO STUDIO</div>
        <div style={{fontSize: 'clamp(34px, 7vw, 76px)', lineHeight: 1.12, fontWeight: 800, color: '#ffe0a0', textShadow: '0 5px 30px rgba(255,157,42,.28)', transform: `scale(${0.92+intro*0.08})`}}>{title || 'Untitled Story'}</div>
        <div style={{width: 110, height: 3, background: '#d99b42', margin: '34px auto'}} />
        <Sequence from={0} durationInFrames={durationInFrames}>
          <div style={{maxWidth: 1050, fontSize: 'clamp(20px, 3.2vw, 38px)', lineHeight: 1.45, color: '#fff1d6', minHeight: 130, display: 'flex', alignItems: 'center', justifyContent: 'center', textShadow: '0 2px 14px #000', transition: 'opacity .2s'}}>{activeText}</div>
        </Sequence>
        <div style={{position: 'absolute', bottom: 28, fontSize: 13, letterSpacing: 3, color: '#b99a70'}}>POWERED BY I KO KO</div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

export const RemotionRoot: React.FC = () => (
  <Composition
    id="MainVideo"
    component={GoldScene}
    defaultProps={{
      title: 'G.I KO KO VIDEO STUDIO',
      script: 'Your story begins here. Add your narration script in the GitHub Actions render form.',
      resolution: '480p',
      orientation: 'landscape',
      durationSec: 30,
    }}
    width={854}
    height={480}
    fps={30}
    durationInFrames={900}
    calculateMetadata={({props}) => {
      const p = props as Props;
      const size = dimensions(p.resolution || '480p', p.orientation || 'landscape');
      const durationSec = Math.min(300, Math.max(5, Number(p.durationSec) || 30));
      return {width: size.width, height: size.height, durationInFrames: durationSec * 30, fps: 30};
    }}
  />
);
