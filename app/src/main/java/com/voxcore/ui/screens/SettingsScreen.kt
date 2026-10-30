package com.voxcore.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.voxcore.audio.AudioIo
import com.voxcore.audio.VoxEngine
import com.voxcore.audio.VoxSettings
import com.voxcore.diagnostics.LatencyTest
import com.voxcore.ui.components.*
import com.voxcore.ui.theme.VoxColors

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VoxColors.PurpleDim, selectedLabelColor = VoxColors.Text)
    )
}

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(hasMicPermission(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var devicesTick by remember { mutableIntStateOf(0) }
    val outputs = remember(devicesTick) { AudioIo.outputDevices() }
    val inputs = remember(devicesTick) { AudioIo.inputDevices() }

    ScreenTitle("Ayarlar ve Tanılama")

    Panel("İzinler") {
        Text(
            if (granted) "Mikrofon izni verildi." else "Mikrofon izni verilmedi.",
            color = VoxColors.Text, style = MaterialTheme.typography.bodyMedium
        )
        if (!granted) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Mikrofon izni iste") }
        }
    }
    Panel("Ses motoru") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(44100, 48000).forEach { r ->
                Choice("$r Hz", VoxSettings.sampleRate == r) { VoxSettings.sampleRate = r }
            }
        }
        LabeledSlider("Blok boyutu", VoxSettings.blockFrames.toFloat(), { VoxSettings.blockFrames = it.toInt() }, 64f..1024f, " kare")
        Text(
            if (VoxEngine.running) "Motor durumu: çalışıyor, ${VoxEngine.activeSampleRate} Hz, tahmini gecikme ${VoxEngine.estimatedLatencyMs} ms" else "Motor durumu: kapalı",
            color = VoxColors.TextDim, style = MaterialTheme.typography.bodyMedium
        )
        PendingNote("Örnekleme hızı, blok boyutu ve ses yolu bir sonraki başlatmada uygulanır. Yukarıdaki gecikme arabellek boyutlarından hesaplanan bir tahmindir; aşağıdaki gecikme testi ölçüm yapar.")
    }
    Panel("Ses yolu") {
        Text("Mikrofon kaynağı", color = VoxColors.TextDim, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice("Mikrofon", VoxSettings.inputSource == 0) { VoxSettings.inputSource = 0 }
            Choice("Ses iletişimi", VoxSettings.inputSource == 1) { VoxSettings.inputSource = 1 }
        }
        Spacer(Modifier.height(8.dp))
        Text("Giriş cihazı", color = VoxColors.TextDim, style = MaterialTheme.typography.labelMedium)
        Choice("Sistem varsayılanı", VoxSettings.inputDeviceId == 0) { VoxSettings.inputDeviceId = 0 }
        inputs.forEach { d -> Choice(d.label, VoxSettings.inputDeviceId == d.id) { VoxSettings.inputDeviceId = d.id } }
        Spacer(Modifier.height(8.dp))
        Text("Çıkış cihazı", color = VoxColors.TextDim, style = MaterialTheme.typography.labelMedium)
        Choice("Sistem varsayılanı", VoxSettings.outputDeviceId == 0) { VoxSettings.outputDeviceId = 0 }
        outputs.forEach { d -> Choice(d.label, VoxSettings.outputDeviceId == d.id) { VoxSettings.outputDeviceId = d.id } }
        TextButton(onClick = { devicesTick++ }) { Text("Cihaz listesini yenile", color = VoxColors.Purple) }
        PendingNote("\"Ses iletişimi\" kaynağı bazı cihazlarda gürültü ve yankı önlemeyi açar ama öncelikli kaynak sayıldığı için çalışırken başka uygulamaların mikrofonunu susturabilir; desteklenmezse otomatik olarak normal mikrofona dönülür. Seçili cihaz çıkarılırsa sistem varsayılanına düşülür. Tercih sistem tarafından yok sayılabilir.")
    }
    Panel("Gecikme testi") {
        Button(
            onClick = { LatencyTest.start() },
            enabled = !LatencyTest.running && !VoxEngine.running,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (LatencyTest.running) "Ölçülüyor..." else "Gecikmeyi ölç") }
        LatencyTest.result?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = VoxColors.Text, style = MaterialTheme.typography.bodyMedium)
        }
        PendingNote("Hoparlörden kısa bir tıklama çalar, mikrofonda duyulma süresini ölçer. Sessiz ortam ve yüksek ses gerekir, kulaklık takılıyken sonuç alınamaz. Çıkış-giriş arasında kablo bağlıysa o yolu ölçer. Ses motoru kapalı olmalı.")
    }
    Panel("Discord ve başka uygulamalara aktarım") {
        Text(
            "Durum: gerçek Discord oturumunda test EDİLMEDİ.",
            color = VoxColors.Text, style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Android, bir uygulamanın başka bir uygulamanın mikrofon girişini değiştirmesine izin vermez (root olmadan). " +
                "VOXCORE sesi Discord'a doğrudan gönderemez. Aynı telefonda iki sıradan uygulama mikrofonu aynı anda alamaz: " +
                "biri ses alır, diğeri sessizlik alır. Sesli arama (Discord gibi) her zaman öncelik alır, bu yüzden Discord aramasındayken " +
                "VOXCORE'un mikrofonu sessiz kalabilir. \"Ses iletişimi\" kaynağı da öncelikli sayılır ve diğer uygulamayı susturabilir.",
            color = VoxColors.TextDim, style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Text("Denenebilecek yollar:", color = VoxColors.Text, style = MaterialTheme.typography.bodyMedium)
        Text(
            "1. İki cihaz: VOXCORE telefonunun kulaklık çıkışını kabloyla (3.5 mm ya da USB ses adaptörü) bilgisayarın hat/mikrofon girişine verin; Discord'da o girişi seçin. En güvenilir yol budur.\n" +
                "2. Bluetooth: VOXCORE çıkışını Bluetooth ses cihazına verip onu bilgisayarın girişine yönlendirmek cihaza özgüdür, garanti yok.\n" +
                "3. Akustik: hoparlör sesi başka bir cihazın mikrofonuna yakın tutmak. Kalite düşük, geri besleme riski yüksek.\n" +
                "4. Root ve sanal ses cihazı yöntemleri bu uygulamanın kapsamı dışındadır.",
            color = VoxColors.TextDim, style = MaterialTheme.typography.bodyMedium
        )
        PendingNote("Hangi yolun işlediğini yalnızca kendi cihazlarınızla deneyerek öğrenebilirsiniz. Denerken gecikme testini ve kulaklığı kullanın; sonucu bana bildirirseniz buraya yazarım.")
    }
    Panel("Dil ve tema") {
        Text("Dil: Türkçe. Tema: koyu. Başka seçenek şimdilik yok.", color = VoxColors.TextDim, style = MaterialTheme.typography.bodyMedium)
    }
}
