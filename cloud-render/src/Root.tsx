import React from 'react';
import {AbsoluteFill, Audio, Composition, staticFile, useCurrentFrame, useVideoConfig, interpolate} from 'remotion';
import {CAPTIONS} from './myanmar-captions';

type Props={title:string;script:string;resolution:'360p'|'480p'|'720p';orientation:'landscape'|'portrait';durationSec:number};
const GOLD='#f6c76b';
const clamp={extrapolateLeft:'clamp' as const,extrapolateRight:'clamp' as const};

const StarField:React.FC<{frame:number}> = ({frame}) => <AbsoluteFill>{Array.from({length:75},(_,i)=>{
 const x=(i*73+17)%100, y=(i*47+11)%100, tw=1.5+(i%3), drift=(frame*(0.08+(i%4)*0.025)+i*19)%110;
 return <div key={i} style={{position:'absolute',left:x+'%',top:((y+drift)%105)+'%',width:tw,height:tw,borderRadius:50,background:i%7===0?GOLD:'#d7eaff',opacity:.2+(i%5)*.12,boxShadow:i%7===0?'0 0 9px #ffc65c':undefined}}/>;
})}</AbsoluteFill>;

const City:React.FC<{frame:number}> = ({frame}) => <div style={{position:'absolute',bottom:'12%',left:'-5%',width:'110%',height:'43%',display:'flex',alignItems:'end',gap:7,transform:`translateX(${Math.sin(frame/75)*1.5}%)`}}>
 {Array.from({length:19},(_,i)=>{const h=18+(i*37%75);return <div key={i} style={{height:h+'%',flex:1,background:'linear-gradient(90deg,#111426,#35203a)',border:'1px solid rgba(255,190,95,.22)',boxShadow:'0 0 24px rgba(30,170,255,.12)',position:'relative'}}>
 <div style={{position:'absolute',inset:'8% 15%',backgroundImage:'repeating-linear-gradient(0deg,transparent 0 13px,rgba(255,190,80,.5) 14px 16px),repeating-linear-gradient(90deg,transparent 0 14px,rgba(60,210,255,.35) 15px 17px)',opacity:.7}}/></div>})}
 </div>;

const Hero:React.FC<{frame:number;pose?:'fall'|'fly'|'stand'}> = ({frame,pose='stand'})=>{
 const bob=Math.sin(frame/7)*9, rotate=pose==='fall'?Math.sin(frame/5)*24:pose==='fly'?-12:Math.sin(frame/18)*3;
 return <div style={{position:'absolute',left:'50%',top:pose==='fall'?'34%':'51%',width:170,height:270,transform:`translate(-50%,-50%) translateY(${bob}px) rotate(${rotate}deg)`,filter:'drop-shadow(0 0 28px rgba(255,182,65,.55))'}}>
  <div style={{position:'absolute',left:42,top:0,width:86,height:86,borderRadius:'48% 48% 42% 42%',background:'linear-gradient(135deg,#ffe4b0,#bd793b 70%,#57351f)',border:'4px solid #f7c66c'}}/>
  <div style={{position:'absolute',left:58,top:38,width:15,height:10,borderRadius:8,background:'#17131a',boxShadow:'32px 0 #17131a'}}/>
  <div style={{position:'absolute',left:66,top:60,width:34,height:7,borderRadius:8,background:'#5b1f18'}}/>
  <div style={{position:'absolute',left:29,top:82,width:112,height:125,borderRadius:'25px 25px 17px 17px',background:'linear-gradient(120deg,#fff0c7,#c68a32 45%,#6b391c)',border:'4px solid #f7c66c'}}/>
  <div style={{position:'absolute',left:0,top:96,width:38,height:108,borderRadius:18,background:'linear-gradient(#f4c46a,#8d4e26)',transform:`rotate(${Math.sin(frame/9)*18-20}deg)`}}/>
  <div style={{position:'absolute',right:0,top:96,width:38,height:108,borderRadius:18,background:'linear-gradient(#f4c46a,#8d4e26)',transform:`rotate(${Math.sin(frame/9)*18+20}deg)`}}/>
  <div style={{position:'absolute',left:39,top:197,width:39,height:72,borderRadius:15,background:'#2b3048'}}/>
  <div style={{position:'absolute',right:39,top:197,width:39,height:72,borderRadius:15,background:'#2b3048'}}/>
  <div style={{position:'absolute',left:68,top:120,width:32,height:32,borderRadius:50,background:'#ffe17a',boxShadow:'0 0 28px #ffbf3f'}}/>
  {pose==='fly'&&<div style={{position:'absolute',left:54,top:250,width:60,height:90,background:'linear-gradient(#fff7a0,#ff6a21,transparent)',filter:'blur(10px)'}}/>}
 </div>
};

const Crystal:React.FC<{frame:number}> = ({frame})=><div style={{position:'absolute',left:'50%',top:'40%',width:100,height:100,transform:`translate(-50%,-50%) rotate(${frame*1.5}deg) scale(${1+Math.sin(frame/8)*.1})`,background:'linear-gradient(135deg,#a6fff4,#7b67ff 45%,#ffce70)',clipPath:'polygon(50% 0,100% 35%,75% 100%,25% 100%,0 35%)',boxShadow:'0 0 70px #75eaff'}}/>;

const Drone:React.FC<{x:string;y:string;frame:number;delay:number}> = ({x,y,frame,delay})=><div style={{position:'absolute',left:x,top:y,width:74,height:44,transform:`translateY(${Math.sin((frame+delay)/5)*16}px) rotate(${Math.sin(frame/8+delay)*8}deg)`,borderRadius:18,background:'linear-gradient(135deg,#e6f5ff,#586a88 60%,#202b43)',border:'2px solid #8ceaff',boxShadow:'0 0 24px #28dfff'}}>
 <div style={{position:'absolute',left:26,top:13,width:22,height:12,borderRadius:50,background:'#ff324f',boxShadow:'0 0 16px #ff324f'}}/></div>;

const Scene:React.FC<{frame:number;act:number}> = ({frame,act})=>{
 const flash=act===4?interpolate(frame%180,[0,10,22,180],[0,.7,0,0],{extrapolateLeft:'clamp',extrapolateRight:'clamp'}):0;
 return <AbsoluteFill style={{overflow:'hidden',background:act===3?'radial-gradient(ellipse at 50% 42%,#234b61,#11142c 48%,#080914)':act>=4?'radial-gradient(ellipse at 50% 35%,#513018,#170d1a 48%,#06060e)':'radial-gradient(ellipse at 50% 38%,#28536d,#11162b 50%,#090a17)'}}>
 <StarField frame={frame}/><City frame={frame}/>
 {act===0&&<><div style={{position:'absolute',top:'18%',left:'10%',fontSize:22,color:'#72eaff',letterSpacing:8}}>NEW YANGON · 2147</div><Hero frame={frame} pose="fall"/><div style={{position:'absolute',top:'65%',left:'20%',width:240,height:8,background:'#ff9c36',filter:'blur(16px)',transform:'rotate(-20deg)'}}/></>}
 {act===1&&<><Crystal frame={frame}/><Hero frame={frame}/><div style={{position:'absolute',top:'17%',width:'100%',textAlign:'center',color:'#a8fff6',fontSize:19,letterSpacing:5}}>UNKNOWN SIGNAL DETECTED</div></>}
 {act===2&&<><Hero frame={frame} pose="fly"/><Drone x="13%" y="31%" frame={frame} delay={0}/><Drone x="68%" y="26%" frame={frame} delay={13}/><Drone x="58%" y="59%" frame={frame} delay={25}/><div style={{position:'absolute',top:'18%',left:'8%',fontSize:28,fontWeight:900,color:'#ffce6e',textShadow:'0 0 18px #ff8b30'}}>CHARGE!</div></>}
 {act===3&&<><Crystal frame={frame}/><div style={{position:'absolute',left:'10%',top:'20%',width:80,height:150,border:'3px solid #7ef7ff',borderRadius:12,boxShadow:'0 0 35px #36eaff'}}/><div style={{position:'absolute',right:'10%',top:'25%',fontSize:18,color:'#f8d48c',writingMode:'vertical-rl'}}>DR. ZETA</div><Hero frame={frame}/></>}
 {act===4&&<><div style={{position:'absolute',left:'24%',top:'18%',width:'52%',height:'42%',border:'12px solid #38435d',borderRadius:'45% 45% 15% 15%',background:'linear-gradient(#180e1e,#090912)',boxShadow:'0 0 65px #ff522f'}}/><div style={{position:'absolute',left:'37%',top:'28%',width:'26%',height:'24%',borderRadius:'50%',background:'#ff3c3c',filter:'blur(20px)'}}/><Hero frame={frame} pose="fly"/><Drone x="10%" y="35%" frame={frame} delay={2}/><Drone x="77%" y="37%" frame={frame} delay={19}/></>}
 {act===5&&<><div style={{position:'absolute',left:'50%',top:'38%',width:260,height:260,borderRadius:50,transform:`translate(-50%,-50%) scale(${1+Math.sin(frame/10)*.15})`,background:'radial-gradient(circle,#fffbd0 0%,#ffcb58 18%,rgba(255,134,36,.2) 58%,transparent 70%)',filter:'blur(3px)'}}/><Hero frame={frame}/><div style={{position:'absolute',top:'18%',width:'100%',textAlign:'center',fontSize:24,letterSpacing:5,color:GOLD}}>THE FIRST LIGHT</div></>}
 <AbsoluteFill style={{background:`rgba(255,226,155,${flash*.18})`}}/>
 <div style={{position:'absolute',top:32,left:30,right:30,display:'flex',justifyContent:'space-between',fontSize:15,letterSpacing:3,color:'#f8d89b'}}><span>THE LAST LIGHT</span><span>G.I KO KO</span></div>
 </AbsoluteFill>
};

const Subtitle:React.FC<{frame:number}> = ({frame})=>{
 const cue=CAPTIONS.find(c=>frame/30>=c.start&&frame/30<c.end);
 if(!cue)return null;
 return <div style={{position:'absolute',left:'6%',right:'6%',bottom:'11%',padding:'18px 22px',borderRadius:14,background:'rgba(5,7,14,.82)',border:'1px solid rgba(246,199,107,.6)',color:'#fff7e7',fontFamily:'Noto Sans Myanmar, Myanmar Text, sans-serif',fontSize:30,lineHeight:1.55,textAlign:'center',whiteSpace:'pre-line',textShadow:'0 2px 4px #000',boxShadow:'0 8px 35px rgba(0,0,0,.4)'}}>{cue.text}</div>;
};

const TheLastLight:React.FC<Props>=({title})=>{
 const frame=useCurrentFrame(); const {durationInFrames}=useVideoConfig();
 const act=frame<1350?0:frame<3000?1:frame<4800?2:frame<6600?3:frame<8250?4:5;
 const fade=interpolate(frame,[0,30,durationInFrames-45,durationInFrames],[0,1,1,0],{extrapolateLeft:'clamp',extrapolateRight:'clamp'});
 const titleOpacity=interpolate(frame,[0,70,260,320],[1,1,0,0],{extrapolateLeft:'clamp',extrapolateRight:'clamp'});
 const endOpacity=interpolate(frame,[8250,8400,8850,9000],[0,0,1,1],{extrapolateLeft:'clamp',extrapolateRight:'clamp'});
 return <AbsoluteFill style={{background:'#080912',opacity:fade}}>
  <Scene frame={frame} act={act}/>
  <div style={{position:'absolute',inset:0,display:'flex',alignItems:'center',justifyContent:'center',flexDirection:'column',opacity:titleOpacity,textAlign:'center'}}>
   <div style={{fontSize:24,letterSpacing:10,color:GOLD}}>A G.I KO KO CINEMATIC STORY</div>
   <div style={{fontSize:76,fontWeight:900,color:'#fff0c9',textShadow:'0 0 35px #ffae42',marginTop:28}}>{title}</div>
   <div style={{fontSize:22,letterSpacing:7,color:'#a8f5ff',marginTop:28}}>ACTION · COMEDY · MYSTERY</div>
  </div>
  <Subtitle frame={frame}/>
  <div style={{position:'absolute',bottom:28,left:0,right:0,textAlign:'center',fontSize:18,letterSpacing:5,color:GOLD,opacity:endOpacity}}>POWERED BY I KO KO</div>
  <Audio src={staticFile('narration.mp3')} volume={0.95}/>
 </AbsoluteFill>;
};

export const RemotionRoot:React.FC=()=>(
 <Composition id="MainVideo" component={TheLastLight} defaultProps={{title:'THE LAST LIGHT',script:'',resolution:'720p',orientation:'portrait',durationSec:300}} width={720} height={1280} fps={30} durationInFrames={9000}/>
);
