package com.voxcore.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.voxcore.audio.VoxEngine
import com.voxcore.audio.VoxSettings
import com.voxcore.database.PresetEntity
import com.voxcore.database.VoxDatabase
import com.voxcore.player.SoundPlayer
import com.voxcore.presets.PresetCodec
import com.voxcore.recorder.Recorder
import com.voxcore.ui.components.*
import com.voxcore.ui.theme.VoxColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private fun safeFileName(s: String): String = s.replace(Regex("[\\\\/:*?\"<>|]"), "_")

@Composable
fun PresetsScreen() {
    val ctx = LocalContext.current
    val dao = remember { VoxDatabase.get(ctx).presets() }
    val presets by dao.all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var showSave by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf<PresetEntity?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val text = runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (text == null || text.size > 200_000) {
                message = "Dosya okunamadı ya da çok büyük."
                return@launch
            }
            val preset = PresetCodec.decode(String(text, Charsets.UTF_8)).getOrElse {
                message = "İçe aktarılamadı: ${it.message}"
                return@launch
            }
            val existing = dao.byName(preset.name)
            dao.insert(
                PresetEntity(
                    id = existing?.id ?: 0L,
                    name = preset.name,
                    json = PresetCodec.encode(preset.name, preset.values),
                    createdAt = System.currentTimeMillis()
                )
            )
            message = if (existing != null) "\"${preset.name}\" güncellendi." else "\"${preset.name}\" içe aktarıldı."
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        val p = exporting
        exporting = null
        if (uri != null && p != null) scope.launch(Dispatchers.IO) {
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(p.json.toByteArray(Charsets.UTF_8)) } ?: error("açılamadı")
            }.isSuccess
            message = if (ok) "Dışa aktarıldı." else "Dışa aktarılamadı."
        }
    }

    ScreenTitle("Ön Ayarlar", "Tüm ses ayarlarını tek dokunuşla yükle")
    OutlinedTextField(
        value = query, onValueChange = { query = it },
        label = { Text("Ön ayar ara") }, singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
    Panel("Kayıtlı ön ayarlar") {
        val shown = presets.filter { it.name.contains(query.trim(), ignoreCase = true) }
        if (shown.isEmpty()) {
            Text(
                if (presets.isEmpty()) "Henüz ön ayar yok." else "Eşleşen ön ayar yok.",
                color = VoxColors.TextDim, style = MaterialTheme.typography.bodyMedium
            )
        } else {
            shown.forEach { p ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(p.name, color = VoxColors.Text, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            PresetCodec.decode(p.json)
                                .onSuccess { PresetCodec.apply(it.values); message = "\"${p.name}\" yüklendi." }
                                .onFailure { message = "Yüklenemedi: ${it.message}" }
                        }) { Text("Yükle", color = VoxColors.Purple) }
                        TextButton(onClick = {
                            exporting = p
                            exportLauncher.launch(safeFileName(p.name) + ".json")
                        }) { Text("Dışa aktar", color = VoxColors.Purple) }
                        TextButton(onClick = {
                            scope.launch { dao.delete(p); message = "\"${p.name}\" silindi." }
                        }) { Text("Sil", color = VoxColors.TextDim) }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { newName = ""; showSave = true }) { Text("Mevcut ayarı kaydet") }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }) {
                Text("JSON içe aktar")
            }
        }
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = VoxColors.Text, style = MaterialTheme.typography.bodyMedium)
        }
        PendingNote("Ön ayar; efekt, perde, bas/tiz, eko, yankı, ana ses, 10 bant ekolayzır, kapı ve kompresör değerlerini saklar. Aynı adla kaydederseniz üzerine yazılır. İçe aktarılan değerler geçerli aralığa kırpılır; bozuk dosyalar reddedilir.")
    }

    if (showSave) {
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text("Ön ayarı kaydet") },
            text = {
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it.take(60) },
                    label = { Text("Ön ayar adı") }, singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isEmpty()) return@TextButton
                    showSave = false
                    val json = PresetCodec.encode(name, PresetCodec.capture())
                    scope.launch {
                        val existing = dao.byName(name)
                        dao.insert(PresetEntity(id = existing?.id ?: 0L, name = name, json = json, createdAt = System.currentTimeMillis()))
                        message = if (existing != null) "\"$name\" güncellendi." else "\"$name\" kaydedildi."
                    }
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("Vazgeç") } }
        )
    }
}

@Composable
fun RecorderScreen() {
    val ctx = LocalContext.current
    val startEngine = rememberEngineStarter()
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) { Recorder.refresh() }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri: Uri? ->
        val f = exporting
        exporting = null
        if (uri != null && f != null) scope.launch(Dispatchers.IO) {
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { out -> f.inputStream().use { it.copyTo(out) } } ?: error("açılamadı")
            }.isSuccess
            message = if (ok) "WAV dışa aktarıldı." else "Dışa aktarılamadı."
        }
    }

    ScreenTitle("Kayıt", "İşlenmiş (efektli) sesi WAV olarak kaydeder")
    Panel("Mikrofon kaydı") {
        Text(formatTime(Recorder.seconds * 1000), style = MaterialTheme.typography.headlineMedium, color = VoxColors.Text)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (Recorder.recording || Recorder.armed) {
                    Recorder.stop()
                } else {
                    Recorder.arm(if (VoxEngine.running) VoxEngine.activeSampleRate else 0)
                    if (!VoxEngine.running) startEngine()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (Recorder.recording || Recorder.armed) "Kaydı durdur" else "Kaydı başlat") }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(44100, 48000).forEach { r ->
                FilterChip(
                    selected = VoxSettings.sampleRate == r, onClick = { VoxSettings.sampleRate = r },
                    label = { Text(if (r == 44100) "44.1 kHz" else "48 kHz") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VoxColors.PurpleDim, selectedLabelColor = VoxColors.Text)
                )
            }
        }
        Recorder.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        PendingNote("Kayıt, ses motorunun çıkışını (efektler dahil) kaydeder; motor kapalıysa kaydı başlatmak motoru da açar. Örnekleme hızı bir sonraki motor başlatışında uygulanır; dosya, motorun gerçek hızıyla yazılır. Kayıt sırasında kulaklık kullanın.")
        if (Recorder.recording && VoxEngine.activeSampleRate != VoxSettings.sampleRate) {
            Text(
                "Not: motor şu an ${VoxEngine.activeSampleRate} Hz ile çalışıyor.",
                color = VoxColors.TextDim, style = MaterialTheme.typography.labelMedium
            )
        }
    }
    Panel("Kayıt geçmişi") {
        if (Recorder.files.isEmpty()) {
            Text("Henüz kayıt yok.", color = VoxColors.TextDim, style = MaterialTheme.typography.bodyMedium)
        } else {
            Recorder.files.toList().forEach { f ->
                val info = remember(f.name, f.length()) { Recorder.info(f) }
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(f.name, color = VoxColors.Text, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (info != null) "${formatTime(info.first * 1000)}  •  ${info.second} Hz  •  ${f.length() / 1024} KB" else "Okunamadı",
                        color = VoxColors.TextDim, style = MaterialTheme.typography.labelMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { SoundPlayer.playExternal(ctx, Uri.fromFile(f), f.name) }) { Text("Çal", color = VoxColors.Purple) }
                        TextButton(onClick = { SoundPlayer.stop() }) { Text("Durdur", color = VoxColors.Purple) }
                        TextButton(onClick = { exporting = f; exportLauncher.launch(f.name) }) { Text("Dışa aktar", color = VoxColors.Purple) }
                        TextButton(onClick = { SoundPlayer.stop(); f.delete(); Recorder.refresh() }) { Text("Sil", color = VoxColors.TextDim) }
                    }
                }
            }
        }
        message?.let { Text(it, color = VoxColors.Text, style = MaterialTheme.typography.bodyMedium) }
    }
}
