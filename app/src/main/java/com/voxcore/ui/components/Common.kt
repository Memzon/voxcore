package com.voxcore.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.voxcore.services.AudioService
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.voxcore.ui.theme.VoxColors
import java.util.Locale

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(bottom = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = VoxColors.Text)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim)
    }
}

@Composable
fun Panel(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(VoxColors.Charcoal, MaterialTheme.shapes.medium)
            .border(1.dp, VoxColors.Outline, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = VoxColors.Text)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    unit: String = "",
    decimals: Int = 0,
    enabled: Boolean = true,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim)
            Text(
                String.format(Locale.US, "%." + decimals + "f", value) + unit,
                style = MaterialTheme.typography.labelMedium, color = VoxColors.Purple
            )
        }
        Slider(
            value = value, onValueChange = onChange, valueRange = range, enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = VoxColors.Purple, activeTrackColor = VoxColors.Purple,
                inactiveTrackColor = VoxColors.Outline
            )
        )
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = VoxColors.Text)
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = VoxColors.Purple, checkedThumbColor = VoxColors.Black)
        )
    }
}

/** Motoru henüz bağlanmamış özellikler için dürüst durum bildirimi. */
@Composable
fun PendingNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = VoxColors.TextDim,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(VoxColors.Surface, MaterialTheme.shapes.small)
            .padding(10.dp)
    )
}

/** Seviye göstergesi (0..1). Motor bağlanana kadar sıfır gösterir; sahte animasyon yok. */
@Composable
fun LevelBars(levels: List<Float>, modifier: Modifier = Modifier) {
    val animated = levels.map { animateFloatAsState(it.coerceIn(0f, 1f), tween(90), label = "lvl").value }
    Canvas(modifier.fillMaxWidth().height(64.dp)) {
        val n = animated.size
        val gap = 4.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        animated.forEachIndexed { i, v ->
            val h = (size.height * v).coerceAtLeast(3.dp.toPx())
            drawRoundRect(
                color = if (v > 0.01f) VoxColors.Purple else VoxColors.Outline,
                topLeft = Offset(i * (w + gap), size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
    }
}

fun hasMicPermission(ctx: Context): Boolean =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/** "m:ss" biçimi. */
fun formatTime(ms: Int): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
}

/** İzin gerekiyorsa ister, sonra ses motorunu ön plan servisiyle başlatan bir işlev döndürür. */
@Composable
fun rememberEngineStarter(): () -> Unit {
    val ctx = LocalContext.current
    fun launchService() {
        ContextCompat.startForegroundService(ctx, Intent(ctx, AudioService::class.java))
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.RECORD_AUDIO] == true) launchService()
    }
    return remember(ctx) {
        val f: () -> Unit = {
            if (hasMicPermission(ctx)) {
                launchService()
            } else {
                val perms = buildList {
                    add(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                }
                launcher.launch(perms.toTypedArray())
            }
        }
        f
    }
}
