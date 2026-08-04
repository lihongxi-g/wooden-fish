package com.woodenfish.app.ui

import android.graphics.Paint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woodenfish.app.SpinnerSegment
import com.woodenfish.app.WoodenFishState
import com.woodenfish.app.WoodenFishViewModel
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private fun t(lang: String, zhCN: String, zhTW: String, en: String, fr: String = en, ru: String = en, es: String = en): String = when (lang) {
    "zh-TW" -> zhTW; "en" -> en; "fr" -> fr; "ru" -> ru; "es" -> es; else -> zhCN
}

private val segmentColors = listOf(
    Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFB74D),
    Color(0xFFBA68C8), Color(0xFF4DB6AC), Color(0xFFF06292), Color(0xFFA1887F),
)

// ═══════════════ 转盘主界面 ═══════════════
@Composable
fun SpinnerScreen(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String) {
    val segs = state.spinnerSegments
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(t(lang, "轉盤", "轉盤", "Spinner", "Roue", "Колесо", "Rueda"),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp), fontFamily = KaiTiFont, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(t(lang, "一轉定分曉", "一轉定分曉", "Let the wheel decide", "La roue décidera", "Пусть колесо решит", "Que la rueda decida"),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            SpinnerWheel(state = state, viewModel = viewModel, segs = segs, lang = lang, modifier = Modifier.size(280.dp))
            // 顶部指针（固定不转）
            Canvas(Modifier.align(Alignment.TopCenter).size(36.dp)) {
                val w = size.width; val h = size.height
                val p = Path().apply { moveTo(w * 0.5f, h); lineTo(0f, 0f); lineTo(w, 0f); close() }
                drawPath(p, Color(0xFFD32F2F))
                drawCircle(Color(0xFFD32F2F), radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.86f))
            }
        }

        Spacer(Modifier.height(16.dp))
        Box(Modifier.height(64.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                state.spinnerSpinning -> HintText(t(lang, "轉動中…", "轉動中…", "Spinning…", "Tourne…", "Крутится…", "Girando…"))
                state.spinnerResult >= 0 -> {
                    val name = segs.getOrNull(state.spinnerResult)?.name?.trim().orEmpty()
                    Text(
                        if (name.isNotEmpty()) name else t(lang, "分區 ${state.spinnerResult + 1}", "分區 ${state.spinnerResult + 1}", "Segment ${state.spinnerResult + 1}", "Segment ${state.spinnerResult + 1}", "Сектор ${state.spinnerResult + 1}", "Segmento ${state.spinnerResult + 1}"),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center
                    )
                }
                else -> HintText(t(lang, "點擊轉盤開始", "點擊轉盤開始", "Tap the wheel to spin", "Touchez la roue pour tourner", "Нажмите на колесо, чтобы крутить", "Toca la rueda para girar"))
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

// ═══════════════ 转盘（扇形 + 名称 + 旋转动画） ═══════════════
@Composable
private fun SpinnerWheel(state: WoodenFishState, viewModel: WoodenFishViewModel, segs: List<SpinnerSegment>, lang: String, modifier: Modifier = Modifier) {
    val rot = remember { Animatable(state.spinnerAngle) }
    LaunchedEffect(state.spinnerTick) {
        if (state.spinnerSpinning) {
            rot.animateTo(state.spinnerAngle, tween(3600, easing = FastOutSlowInEasing))
        }
    }
    val n = segs.size
    val spin = modifier
        .graphicsLayer { rotationZ = rot.value }
        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.spinSpinner() }

    Canvas(spin) {
        val w = size.width; val h = size.height
        val cx = w / 2f; val cy = h / 2f
        val r = min(w, h) * 0.47f
        // 外圈阴影
        drawCircle(Color(0xFF140801).copy(alpha = 0.18f), radius = r * 1.06f, center = Offset(cx, cy + h * 0.02f))
        // 外圈
        drawCircle(Color(0xFF6D4C41), radius = r, center = Offset(cx, cy))
        if (n > 0) {
            val sweep = 360f / n
            segs.forEachIndexed { i, seg ->
                val start = i * sweep
                val segColor = segmentColors[i % segmentColors.size]
                drawArc(
                    color = segColor,
                    startAngle = start + 0.6f, sweepAngle = sweep - 1.2f, useCenter = true,
                    topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2)
                )
                // 分区名称（沿半径方向，从内向外）
                val label = seg.name.trim().ifEmpty { "${i + 1}" }
                drawIntoCanvas { canvas ->
                    val paint = Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textSize = min(r * 0.16f, 26f)
                        typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    val mid = start + sweep / 2f
                    val rad = (mid - 90f) * (Math.PI.toFloat() / 180f)
                    val tx = cx + cos(rad) * r * 0.62f
                    val ty = cy + sin(rad) * r * 0.62f
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.rotate(mid - 90f + 90f, tx, ty)
                    val shortened = if (label.length > 5) label.take(5) + "…" else label
                    canvas.nativeCanvas.drawText(shortened, tx, ty + paint.textSize * 0.35f, paint)
                    canvas.nativeCanvas.restore()
                }
            }
        }
        // 分隔线
        if (n > 1) {
            val sweep = 360f / n
            for (i in 0 until n) {
                val a = i * sweep * (Math.PI.toFloat() / 180f)
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(cx, cy),
                    end = Offset(cx + cos(a) * r, cy + sin(a) * r),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
        // 中心圆
        drawCircle(Color(0xFF6D4C41), radius = r * 0.10f, center = Offset(cx, cy))
        drawCircle(Color(0xFFF5E6C8), radius = r * 0.07f, center = Offset(cx, cy))
    }
}

// ═══════════════ 转盘设置页（分区名称 + 概率权重） ═══════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinnerSettingsPage(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String) {
    val segs = state.spinnerSegments
    val total = segs.sumOf { it.weight.coerceAtLeast(0) }.coerceAtLeast(1)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(t(lang, "分區與概率", "分區與概率", "Segments & Odds", "Segments et probabilités", "Секторы и вероятности", "Segmentos y probabilidades"),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(t(lang, "自定義轉盤分區：名稱決定分區數，權重決定被指中的概率。例如定義 3 個分區，圓盤就分為三部分。", "自定義轉盤分區：名稱決定分區數，權重決定被指中的概率。例如定義 3 個分區，圓盤就分為三部分。", "Customize the wheel: each segment's name is a slice, and its weight sets the odds. Define 3 segments and the wheel splits into three parts.", "Personnalisez la roue : chaque nom est une part, chaque poids fixe les chances. Définissez 3 segments et la roue se divise en trois.", "Настройте колесо: имя каждого сектора — его часть, вес задаёт шансы. Задайте 3 сектора — колесо разделится на три.", "Personaliza la rueda: cada nombre es una porción y su peso fija las probabilidades. Define 3 segmentos y la rueda se divide en tres."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        segs.forEachIndexed { i, seg ->
            val pct = (seg.weight.coerceAtLeast(0) * 100f / total).toInt()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(segmentColors[i % segmentColors.size]))
                OutlinedTextField(
                    value = seg.name,
                    onValueChange = { viewModel.updateSpinnerSegment(i, name = it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(t(lang, "分區 ${i + 1}", "分區 ${i + 1}", "Segment ${i + 1}", "Segment ${i + 1}", "Сектор ${i + 1}", "Segmento ${i + 1}"), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) },
                    shape = RoundedCornerShape(8.dp)
                )
                if (segs.size > 2) {
                    TextButton(onClick = { viewModel.removeSpinnerSegment(i) }) { Text("✕", color = MaterialTheme.colorScheme.error) }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.width(24.dp))
                Slider(
                    value = seg.weight.toFloat(),
                    onValueChangeFinished = { /* 通过 onValueChange 实时更新 */ },
                    onValueChange = { viewModel.updateSpinnerSegment(i, weight = it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Text("${seg.weight}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                Text(t(lang, "概率 $pct%", "概率 $pct%", "$pct%", "$pct%", "$pct%", "$pct%"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(56.dp))
            }
        }
        if (segs.size < 8) {
            OutlinedButton(onClick = { viewModel.addSpinnerSegment() }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Text(t(lang, "添加分區", "添加分區", "Add segment", "Ajouter un segment", "Добавить сектор", "Añadir segmento"))
            }
        }
        Button(onClick = {
            viewModel.resetSpinner()
            viewModel.toast(t(lang, "已重置轉盤設置", "已重置轉盤設置", "Spinner settings reset", "Réglages de la roue réinitialisés", "Настройки колеса сброшены", "Ajustes de la rueda restablecidos"))
        }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
            Text(t(lang, "重置", "重置", "Reset", "Réinitialiser", "Сбросить", "Restablecer"))
        }
    }
}
