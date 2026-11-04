package com.gikoko.aivideostudio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val HelpGold = Color(0xFFFFC857)
private val HelpGreen = Color(0xFF00C389)
private val HelpPanel = Color(0xFF1B2721)

@Composable
fun HelpGuideContent(onClose: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Color(0xFF101713)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("အသုံးပြုနည်း လမ်းညွှန်", color = HelpGold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("G.I KO KO VIDEO STUDIO PRO • OFFLINE USER GUIDE", color = HelpGreen, style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onClose) { Text("ပိတ်မည် ✕", color = HelpGold) }
            }
            HorizontalDivider(color = HelpGreen)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HelpSection("၁။ စတင်အသုံးပြုခြင်း", listOf(
                    "အောက်ခြေရှိ Edit ကိုနှိပ်ပါ။ Import Video from Phone ကိုနှိပ်ပြီး ဖုန်းထဲက video ကိုရွေးပါ။",
                    "Android file picker ပေါ်လာလျှင် Videos သို့မဟုတ် သက်ဆိုင်ရာ folder ထဲက ဖိုင်ကိုရွေးပါ။",
                    "Preview မှာ video ကိုဖွင့်ကြည့်နိုင်သည်။ Player controls ဖြင့် Play / Pause လုပ်ပါ။",
                    "ဖိုင်မပေါ်လျှင် ဖုန်း၏ Files app မှတစ်ဆင့် ရှာဖွေရွေးချယ်ပါ။"
                ))
                HelpSection("၂။ Video Trim (အစ/အဆုံး ဖြတ်ခြင်း)", listOf(
                    "Edit စာမျက်နှာတွင် Trim start slider ကိုရွှေ့ပြီး စတင်မည့်အချိန်ကို သတ်မှတ်ပါ။",
                    "Trim end slider ကိုရွှေ့ပြီး အဆုံးသတ်မည့်အချိန်ကို သတ်မှတ်ပါ။",
                    "Reset Trim ကိုနှိပ်လျှင် မူလ video အရှည်အတိုင်း ပြန်ထားမည်။",
                    "လက်ရှိ build တွင် clip များကို timeline ပေါ် drag လုပ်ခြင်း၊ split/join လုပ်ခြင်း မပါသေးပါ။"
                ))
                HelpSection("၃။ Watermark Tools", listOf(
                    "အောက်ခြေ Watermark ကိုနှိပ်ပါ။ Crop ကိုရွေးပြီး slider ဖြင့် ဘေးလေးဖက်မှ ဖြတ်မည့်ပမာဏကို ချိန်ပါ။",
                    "Blur သည် Gaussian blur ဖြင့် ပုံတစ်ခုလုံးကို မှုန်ဝါးစေသည်။ Mosaic သည် ပိုပြင်းသော blur ပုံစံသာဖြစ်ပြီး true pixel mosaic မဟုတ်ပါ။",
                    "လက်ရှိ Blur/Mosaic သည် frame တစ်ခုလုံးကို သက်ရောက်သည်။ Watermark နေရာတစ်ခုတည်းကို rectangle ရွေးဖျောက်ခြင်းနှင့် motion tracking မပါသေးပါ။",
                    "AI Watermark Remover သည် ယခု build တွင် မပါဝင်သေးပါ။ AI model ထည့်သွင်းပြီး အလုပ်လုပ်နေပြီဟု မယူဆပါနှင့်။",
                    "ကိုယ်ပိုင်ပိုင်ဆိုင်သော သို့မဟုတ် ပြုပြင်ခွင့်ရှိသော video များတွင်သာ အသုံးပြုပါ။"
                ))
                HelpSection("၄။ Audio Studio", listOf(
                    "Audio ကိုနှိပ်ပြီး Add Audio Tracks ဖြင့် ဖုန်းထဲမှ audio ဖိုင်တစ်ခု သို့မဟုတ် အများအပြား ရွေးပါ။",
                    "ထည့်ထားသော track များကို Remove ဖြင့် ဖယ်ရှားနိုင်သည်။ လက်ရှိ build တွင် track အားလုံးသည် project အစ (0:00) မှ စတင်ရောနှောသည်။",
                    "Master Volume သည် export အသံအဆင့်ကို ချိန်သည်။ 100% သည် ပုံမှန်အဆင့်ဖြစ်သည်။",
                    "LOW သည် bass၊ MID သည် အလယ်အသံ၊ HIGH သည် treble ကို ချိန်သည်။ စတင်စမ်းသပ်ရာတွင် 0 dB ထားပါ။",
                    "Compressor Threshold ကို လျှော့လျှင် အသံကျယ်ပိုင်းများကို ပိုမိုထိန်းညှိမည်။ Ratio မြင့်လျှင် compression ပိုပြင်းမည်။",
                    "Noise Gate သည် တိတ်ဆိတ်သောအပိုင်းရှိ အသံနိမ့် background noise ကို လျှော့ပေးသည်။ AI noise removal သို့မဟုတ် voice isolation မဟုတ်ပါ။"
                ))
                HelpSection("၅။ Export လုပ်ခြင်း", listOf(
                    "Export ကိုနှိပ်ပြီး 480p သို့မဟုတ် 720p ကိုရွေးပါ။",
                    "Redmi 9A အတွက် 480p ကို အကြံပြုသည်။ 720p သည် ပိုကြာနိုင်ပြီး memory ပိုသုံးနိုင်သည်။",
                    "Export Offline MP4 ကိုနှိပ်ပါ။ Android Save dialog ပေါ်လာလျှင် ဖိုင်အမည်နှင့် သိမ်းမည့် folder ကိုရွေးပြီး Save လုပ်ပါ။",
                    "Output သည် H.264 video + AAC audio ပါသော MP4 ဖြစ်သည်။ Export ပြီးဆုံးသည်အထိ app ကို foreground တွင်ဖွင့်ထားပါ။",
                    "ဖုန်းတွင် storage လွတ်နေစေပြီး export အချိန်တွင် အခြား heavy app များကို ပိတ်ထားပါ။"
                ))
                HelpSection("၆။ အကြံပြု Settings (Redmi 9A)", listOf(
                    "ပထမဆုံး 10–20 စက္ကန့် video တစ်ခုဖြင့် 480p export စမ်းပါ။",
                    "ကြာချိန်ရှည်သော video များကို အပိုင်းတိုများအဖြစ် စမ်းသပ်ပါ။ ဖုန်းပူလာလျှင် ခဏနားပါ။",
                    "မူရင်းဖိုင်ကို မဖျက်ပါနှင့်။ Export ရလဒ်ကို ဖွင့်စစ်ပြီးမှ မူရင်းကို စီမံပါ။",
                    "720p export မအောင်မြင်လျှင် 480p ဖြင့် ပြန်စမ်းပါ။"
                ))
                HelpSection("၇။ Error ဖြေရှင်းနည်း", listOf(
                    "Video duration မဖတ်နိုင်ပါက အခြား MP4 ဖိုင်တစ်ခုဖြင့် စမ်းပါ။ မထောက်ပံ့သော codec ဖြစ်နိုင်သည်။",
                    "Export failed ဖြစ်လျှင် app ကိုပြန်ဖွင့်၊ storage လွတ်နေမှုစစ်ပြီး 480p ဖြင့် စမ်းပါ။",
                    "ဖိုင်သိမ်းမရလျှင် Save dialog တွင် အခြား folder တစ်ခုရွေးပါ။",
                    "အသံမထွက်လျှင် source video/audio ကို အခြား player ဖြင့်စစ်ပြီး နောက်တစ်ကြိမ် export စမ်းပါ။",
                    "ဖုန်းပိတ်သွားခြင်း၊ app ကို force stop လုပ်ခြင်းသည် export ကို ရပ်တန့်စေနိုင်သည်။"
                ))
                HelpSection("၈။ Offline နှင့် လက်ရှိကန့်သတ်ချက်များ", listOf(
                    "လက်ရှိ editing နှင့် export လုပ်ငန်းစဉ်များသည် ဖုန်းပေါ်တွင် local အလုပ်လုပ်သည်။ API key၊ cloud upload သို့မဟုတ် Internet မလိုပါ။",
                    "လက်ရှိ build တွင် drag-and-drop timeline၊ video split/join၊ transition၊ text/subtitle overlay၊ PIP၊ keyframe၊ speed/reverse မပါသေးပါ။",
                    "Audio track တစ်ခုချင်းစီ၏ start offset၊ volume automation နှင့် waveform editor မပါသေးပါ။",
                    "ဒီ guide သည် လက်ရှိ app ထဲတွင် အမှန်တကယ်ပါဝင်သော feature များကိုသာ ဖော်ပြထားသည်။"
                ))
                Text("Powered By I KO KO", color = HelpGreen, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, points: List<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = HelpPanel), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = HelpGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            points.forEach { point ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", color = HelpGreen, fontWeight = FontWeight.Bold)
                    Text(point, color = Color(0xFFE4EAE6), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
