package com.woodenfish.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woodenfish.app.WoodenFishState
import com.woodenfish.app.WoodenFishViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

// 6 语言：未提供的 fr/ru/es 回退英文（默认参数），核心文案全部给出
private fun t(lang: String, zhCN: String, zhTW: String, en: String, fr: String = en, ru: String = en, es: String = en): String = when (lang) {
    "zh-TW" -> zhTW; "en" -> en; "fr" -> fr; "ru" -> ru; "es" -> es; else -> zhCN
}

/** 骰子六面点布局（相对坐标 0..1） */
private val diceDots = mapOf(
    1 to listOf(0.5f to 0.5f),
    2 to listOf(0.32f to 0.32f, 0.68f to 0.68f),
    3 to listOf(0.32f to 0.32f, 0.5f to 0.5f, 0.68f to 0.68f),
    4 to listOf(0.32f to 0.32f, 0.68f to 0.32f, 0.32f to 0.68f, 0.68f to 0.68f),
    5 to listOf(0.32f to 0.32f, 0.68f to 0.32f, 0.5f to 0.5f, 0.32f to 0.68f, 0.68f to 0.68f),
    6 to listOf(0.32f to 0.30f, 0.32f to 0.5f, 0.32f to 0.70f, 0.68f to 0.30f, 0.68f to 0.5f, 0.68f to 0.70f),
)

// ═══════════════ 骰子主界面 ═══════════════
@Composable
fun DiceScreen(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(t(lang, "擲骰", "擲骰", "Dice", "Dé", "Кость", "Dado"),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp), fontFamily = KaiTiFont, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(t(lang, "一擲定乾坤", "一擲定乾坤", "Let fate decide", "Le sort en est jeté", "Жребий брошен", "La suerte está echada"),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))

        Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
            DiceRoller(state = state, viewModel = viewModel, lang = lang, modifier = Modifier.size(170.dp))
        }

        Spacer(Modifier.height(16.dp))
        // 结果定义 / 提示区（固定高度防跳动）
        Box(Modifier.height(64.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                state.diceRolling -> HintText(t(lang, "骰子滾動中…", "骰子滾動中…", "Rolling…", "En cours…", "Бросок…", "Lanzando…"))
                state.diceTick > 0 -> {
                    val label = state.diceLabels.getOrNull(state.diceResult - 1)?.trim().orEmpty()
                    if (label.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                            Text(t(lang, "骰出 ${state.diceResult} 點", "骰出 ${state.diceResult} 點", "Rolled ${state.diceResult}", "Résultat : ${state.diceResult}", "Выпало: ${state.diceResult}", "Sacaste ${state.diceResult}"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(t(lang, "骰出 ${state.diceResult} 點", "骰出 ${state.diceResult} 點", "Rolled ${state.diceResult}", "Résultat : ${state.diceResult}", "Выпало: ${state.diceResult}", "Sacaste ${state.diceResult}"),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    }
                }
                state.diceTriggerMode == "shake" -> HintText(t(lang, "搖一搖手機擲骰", "搖一搖手機擲骰", "Shake your phone to roll", "Secouez le téléphone pour lancer", "Встряхните телефон, чтобы бросить", "Agita el teléfono para lanzar"))
                else -> HintText(t(lang, "點擊骰子擲骰", "點擊骰子擲骰", "Tap the die to roll", "Touchez le dé pour lancer", "Нажмите на кость, чтобы бросить", "Toca el dado para lanzar"))
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

// ═══════════════ 拟真骰子（滚动 + 换面 + 弹跳 + 落定晃动） ═══════════════
@Composable
private fun DiceRoller(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String, modifier: Modifier = Modifier) {
    val rolling = state.diceRolling
    var face by remember { mutableIntStateOf(state.diceFace) }
    val rotZ = remember { Animatable(0f) }
    val bounce = remember { Animatable(0f) }   // 0..1 弹跳进度（1=着地）
    val tilt = remember { Animatable(0f) }     // 落定后的轻微晃动

    LaunchedEffect(state.diceTick) {
        if (rolling) {
            // 并行：换面 / 旋转 / 弹跳
            rotZ.snapTo(0f); bounce.snapTo(0f); tilt.snapTo(0f)
            // 旋转（两圈，快起慢停）
            launch { rotZ.animateTo(720f, tween(1250, easing = FastOutSlowInEasing)) }
            // 弹跳：先重后轻（振幅递减）
            launch {
                repeat(6) { i ->
                    bounce.animateTo(1f, tween(70 + i * 30, easing = FastOutSlowInEasing))
                    if (i < 5) bounce.animateTo(0f, tween(60 + i * 25, easing = LinearOutSlowInEasing))
                }
            }
            // 换面：慢→快→慢，最后落定结果面（与旋转同步结束）
            launch {
                var interval = 90
                repeat(20) { i ->
                    if (i < 20 - 1) face = Random.nextInt(6) + 1 else face = state.diceResult
                    delay(interval.toLong())
                    interval = when {
                        i < 6 -> interval - 8                    // 加速
                        i < 15 -> 38                             // 高速滚动
                        else -> interval + 16                    // 减速
                    }
                }
            }
            // 落定晃动（旋转结束后）
            delay(1250)
            tilt.animateTo(1f, tween(160)); tilt.animateTo(-0.7f, tween(120)); tilt.animateTo(0.35f, tween(110)); tilt.animateTo(0f, tween(130))
        } else {
            face = state.diceFace
            rotZ.snapTo(0f); bounce.snapTo(1f); tilt.snapTo(0f)
        }
    }

    val clickable = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
        if (state.diceTriggerMode == "shake") {
            viewModel.toast(t(lang, "搖一搖手機擲骰", "搖一搖手機擲骰", "Shake your phone to roll", "Secouez le téléphone pour lancer", "Встряхните телефон, чтобы бросить", "Agita el teléfono para lanzar"))
        } else {
            viewModel.rollDice()
        }
    }

    Box(
        modifier.graphicsLayer {
            // 弹跳：Y 压缩 + 离地位移（bounce 0→1 时压扁后弹起）
            val comp = if (rolling) abs(1f - bounce.value) * 0.28f else 0f
            scaleX = 1f + comp * 0.6f
            scaleY = 1f - comp
            translationY = -bounce.value * 26f * (1f - bounce.value) * 3.2f
            rotationZ = rotZ.value + tilt.value * 9f
            val faceShake = if (rolling) (Random.nextInt(7) - 3).toFloat() * (1f - rotZ.value / 720f) * 2.2f else 0f
            rotationX = faceShake
        }.then(clickable),
        contentAlignment = Alignment.Center
    ) {
        DiceFace(face)
    }
}

/** 伪 3D 骰子：主面 + 右下厚度 + 阴影 + 点数 */
@Composable
private fun DiceFace(face: Int) {
    val dieWhite = Color(0xFFFAF6EE)
    val dieEdge = Color(0xFF8B7355)
    val diePip = Color(0xFF1A1A1A)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        // 地面阴影
        drawOval(color = Color(0xFF140801).copy(alpha = 0.20f), topLeft = Offset(w * 0.14f, h * 0.86f), size = Size(w * 0.72f, h * 0.10f))
        // 厚度（右下偏移的深色层）
        drawRoundRect(color = dieEdge, topLeft = Offset(w * 0.10f, h * 0.10f), size = Size(w * 0.80f, h * 0.80f), cornerRadius = CornerRadius(w * 0.14f))
        // 主面
        drawRoundRect(color = dieWhite, topLeft = Offset(w * 0.16f, h * 0.16f), size = Size(w * 0.68f, h * 0.68f), cornerRadius = CornerRadius(w * 0.11f))
        // 内阴影（顶部轻微）
        drawRoundRect(color = Color(0xFF000000).copy(alpha = 0.04f), topLeft = Offset(w * 0.16f, h * 0.16f), size = Size(w * 0.68f, h * 0.68f), cornerRadius = CornerRadius(w * 0.11f))
        // 点数
        val dots = diceDots[face] ?: diceDots[1]!!
        val r = w * 0.085f
        dots.forEach { (dx, dy) ->
            drawCircle(color = diePip, radius = r, center = Offset(w * (0.16f + 0.68f * dx), h * (0.16f + 0.68f * dy)))
        }
    }
}

// ═══════════════ 触发模式设置页（抽签模式 / 掷骰模式共用） ═══════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerModePage(
    title: String,
    current: String,
    onSelect: (String) -> Unit,
    tapLabel: String, shakeLabel: String,
    desc: String,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = current == "tap", onClick = { onSelect("tap") }, label = { Text(tapLabel, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
            FilterChip(selected = current == "shake", onClick = { onSelect("shake") }, label = { Text(shakeLabel, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
            Text(desc, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ═══════════════ 骰子设置页（概率权重 + 点数定义） ═══════════════
@Composable
fun DiceSettingsPage(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String) {
    val weights = state.diceWeights
    val total = weights.sum().coerceAtLeast(1)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(t(lang, "點數概率", "點數概率", "Dice Probabilities", "Probabilités", "Вероятности", "Probabilidades"),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(t(lang, "拖動滑塊調整各點數出現的權重，權重越高越容易出現。", "拖動滑塊調整各點數出現的權重，權重越高越容易出現。", "Drag the sliders to adjust each face's weight — higher weight means more likely.", "Réglez le poids de chaque face — plus le poids est élevé, plus la face est probable.", "Перетащите ползунки, чтобы задать вес каждой грани — чем больше вес, тем чаще выпадает.", "Arrastra los controles para ajustar el peso de cada cara: a mayor peso, más probable."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        weights.forEachIndexed { i, w ->
            DiceWeightRow(i, w, total, lang) { newW -> viewModel.setDiceWeight(i, newW) }
        }
        Divider()
        Text(t(lang, "點數定義", "點數定義", "Face Labels", "Définitions", "Определения", "Definiciones"),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(t(lang, "給每個點數起個名字，擲骰結果會直接顯示定義，例如把 1 定義成「打籃球」。", "給每個點數起個名字，擲骰結果會直接顯示定義，例如把 1 定義成「打籃球」。", "Give each face a name; the result shows it directly. E.g. define 1 as \"Basketball\".", "Nommez chaque face ; le résultat l'affichera. Ex. : 1 = « Basketball ».", "Дайте имя каждой грани; результат покажет его. Напр.: 1 = «Баскетбол».", "Pon nombre a cada cara; el resultado lo mostrará. P. ej.: 1 = «Baloncesto»."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.diceLabels.forEachIndexed { i, label ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) { DiceFace(i + 1) }
                Text("${i + 1}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(14.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { viewModel.setDiceLabel(i, it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(t(lang, "如：打籃球", "如：打籃球", "e.g. Basketball", "ex. : Basketball", "напр.: Баскетбол", "p. ej.: Baloncesto"), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
        Button(onClick = {
            viewModel.resetDiceSettings()
            viewModel.toast(t(lang, "已重置骰子設置", "已重置骰子設置", "Dice settings reset", "Réglages du dé réinitialisés", "Настройки кости сброшены", "Ajustes del dado restablecidos"))
        }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
            Text(t(lang, "重置", "重置", "Reset", "Réinitialiser", "Сбросить", "Restablecer"))
        }
    }
}

@Composable
private fun DiceWeightRow(i: Int, weight: Int, total: Int, lang: String, onChange: (Int) -> Unit) {
    var local by remember(i, weight) { mutableIntStateOf(weight) }
    val pct = (local * 100f / total).toInt()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${i + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(18.dp))
        Slider(
            value = local.toFloat(),
            onValueChange = { local = it.toInt() },
            onValueChangeFinished = { onChange(local) },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
        )
        Text("$local", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
        Text(t(lang, "概率 $pct%", "概率 $pct%", "$pct%", "$pct%", "$pct%", "$pct%"),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(56.dp))
    }
}
