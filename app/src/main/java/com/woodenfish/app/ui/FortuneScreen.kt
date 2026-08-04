package com.woodenfish.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woodenfish.app.FortuneData
import com.woodenfish.app.FortuneStick
import com.woodenfish.app.WoodenFishState
import com.woodenfish.app.WoodenFishViewModel
import kotlin.math.PI
import kotlin.math.sin

// 字体：楷书（签面所有文字统一用，纯黑，古代风格）
val KaiTiFont = FontFamily(Font(com.woodenfish.app.R.font.kaiti))

// 签面文字颜色：纯黑（木签米黄底上对比清晰，古代木刻感）
val InkBlack = Color(0xFF1A1A1A)

private fun t(lang: String, zhCN: String, zhTW: String, en: String, fr: String = en, ru: String = en, es: String = en) = when (lang) { "zh-TW" -> zhTW; "en" -> en; "fr" -> fr; "ru" -> ru; "es" -> es; else -> zhCN }

/** 抽签主界面：phase 0=静置 1=摇晃中 2=签已弹出 3=已翻面 */
@Composable
fun FortuneScreen(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String) {
    val phase = state.fortunePhase
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        // 标题
        Text(t(lang, "求籤", "求籤", "Fortune", "Tirage", "Гадание", "Sortilegio"), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp), fontFamily = KaiTiFont, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(t(lang, "誠心所念，必有迴響", "誠心所念，必有迴響", "Ask with a sincere heart", "Demandez avec un cœur sincère", "Просите с искренним сердцем", "Pide con un corazón sincero"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))

        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            FortuneTube(
                phase = phase, tick = state.fortuneTick,
                modifier = Modifier.size(280.dp).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    if (state.fortuneTriggerMode == "shake") {
                        viewModel.toast(t(lang, "搖一搖手機抽籤", "搖一搖手機抽籤", "Shake your phone to draw", "Secouez le téléphone pour tirer", "Встряхните телефон для гадания", "Agita el teléfono para sacar"))
                    } else {
                        viewModel.tapFortuneTube()
                    }
                }
            )
            if (phase >= 2 && state.fortuneStick != null) {
                DrawnStick(
                    stick = state.fortuneStick!!, phase = phase, tick = state.fortuneTick, lang = lang,
                    onClick = { viewModel.flipFortuneStick() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        // 提示 / 操作区（固定高度防跳动）
        Box(Modifier.height(64.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (phase) {
                0 -> if (state.fortuneTriggerMode == "shake")
                    HintText(t(lang, "搖一搖手機抽籤", "搖一搖手機抽籤", "Shake your phone to draw", "Secouez le téléphone pour tirer", "Встряхните телефон для гадания", "Agita el teléfono para sacar"))
                else
                    HintText(t(lang, "點擊籤筒，搖一籤", "點擊籤筒，搖一籤", "Tap the tube to draw", "Touchez le tube pour tirer", "Нажмите на стаканчик", "Toca el tubo para sacar"))
                1 -> HintText(t(lang, "搖籤中…", "搖籤中…", "Shaking…", "En cours…", "Встряска…", "Agitando…"))
                2 -> HintText(t(lang, "點擊木籤查看籤文", "點擊木籤查看籤文", "Tap the stick to see it", "Touchez le bâton pour voir", "Нажмите на палочку", "Toca el palo para verlo"))
                else -> Button(
                    onClick = { viewModel.resetFortune() },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text(t(lang, "再抽一籤", "再抽一籤", "Draw again", "Retirer", "Ещё раз", "Sacar de nuevo"), fontSize = 15.sp) }
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

// ═══════════════ 签筒 ═══════════════
@Composable
fun FortuneTube(phase: Int, tick: Int, modifier: Modifier = Modifier) {
    val shake = remember { Animatable(0f) }
    val isShaking = phase == 1
    LaunchedEffect(tick) {
        if (isShaking) {
            shake.snapTo(0f)
            // 摇晃：先快后慢，幅度渐小（摇签感）
            var amp = 1f
            repeat(7) {
                shake.animateTo(amp, tween(95, easing = LinearEasing))
                shake.animateTo(-amp, tween(95, easing = LinearEasing))
                amp *= 0.85f
            }
            shake.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
        } else {
            shake.snapTo(0f)
        }
    }
    Canvas(modifier = modifier.graphicsLayer {
        rotationZ = shake.value * 10f
        translationY = shake.value * 5.dp.toPx()
    }) {
        val w = size.width; val h = size.height
        // 地面阴影
        drawOval(color = Color(0xFF140801).copy(alpha = 0.18f), topLeft = Offset(w * 0.22f, h * 0.80f), size = Size(w * 0.56f, h * 0.09f))
        // 筒内签头（5 根，露出筒口）
        val stickColors = listOf(Color(0xFF8C3B1E), Color(0xFFD4AF37), Color(0xFFC8A27A), Color(0xFF8C3B1E), Color(0xFFD4AF37))
        val shown = if (phase >= 2) 4 else 5
        val mouthY = h * 0.34f
        stickColors.take(shown).forEachIndexed { i, c ->
            val cx = w * 0.40f + i * (w * 0.05f)
            val tilt = ((i % 3) - 1) * 3f
            drawContext.canvas.save()
            drawContext.canvas.translate(cx, mouthY + h * 0.10f)
            drawContext.canvas.rotate(tilt)
            drawContext.canvas.translate(-cx, -(mouthY + h * 0.10f))
            drawRoundRect(
                color = c,
                topLeft = Offset(cx - w * 0.012f, mouthY - h * 0.14f),
                size = Size(w * 0.024f, h * 0.17f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.008f)
            )
            drawContext.canvas.restore()
        }
        // 桶身（上宽下窄梯形）
        val topL = Offset(w * 0.26f, h * 0.40f); val topR = Offset(w * 0.74f, h * 0.40f)
        val botL = Offset(w * 0.31f, h * 0.82f); val botR = Offset(w * 0.69f, h * 0.82f)
        val body = Path().apply {
            moveTo(topL.x, topL.y); lineTo(topR.x, topR.y); lineTo(botR.x, botR.y); lineTo(botL.x, botL.y); close()
        }
        drawPath(body, brush = Brush.linearGradient(
            listOf(Color(0xFFE8B87E), Color(0xFFC88A4A), Color(0xFF8B5A2B), Color(0xFF5C3317)),
            start = Offset(w * 0.30f, 0f), end = Offset(w * 0.72f, 0f)
        ))
        // 桶口内壁（椭圆开口）
        drawOval(color = Color(0xFF3E2210), topLeft = Offset(w * 0.26f, h * 0.36f), size = Size(w * 0.48f, h * 0.09f))
        drawOval(color = Color(0xFF5C3317), topLeft = Offset(w * 0.26f, h * 0.385f), size = Size(w * 0.48f, h * 0.06f))
        // 桶口外沿高光
        drawArc(color = Color(0xFFF5D9A8).copy(alpha = 0.5f), startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.26f, h * 0.355f), size = Size(w * 0.48f, h * 0.06f), style = Stroke(width = w * 0.006f))
        // 竹节纹
        listOf(0.52f, 0.64f, 0.76f).forEach { y ->
            drawArc(
                color = Color(0xFF4A2A12).copy(alpha = 0.45f),
                startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(w * 0.295f, h * y - h * 0.015f), size = Size(w * 0.41f, h * 0.03f),
                style = Stroke(width = w * 0.006f)
            )
        }
        // 桶身高光条
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))),
            topLeft = Offset(w * 0.315f, h * 0.42f), size = Size(w * 0.045f, h * 0.38f)
        )
    }
}

// ═══════════════ 弹出的木签 ═══════════════
@Composable
private fun DrawnStick(stick: FortuneStick, phase: Int, tick: Int, lang: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // 弹出动画：抛物线上抛 + 右移 + 空中微摆
    val pop = remember { Animatable(0f) }
    // 翻转动画
    val flip = remember { Animatable(0f) }
    LaunchedEffect(tick) {
        if (phase >= 2) {
            pop.snapTo(0f)
            pop.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(tick, phase) {
        if (phase == 3) {
            flip.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        } else {
            flip.snapTo(0f)
        }
    }
    val p = pop.value
    val dx = 92.dp.value * p
    val fly = sin(PI * p).toFloat() * -160.dp.value
    val rot = sin(PI * p).toFloat() * 22f
    val flipY = flip.value * 180f

    Box(
        modifier = modifier
            .offset(x = dx.dp, y = fly.dp)
            .graphicsLayer { rotationZ = rot; rotationY = flipY; cameraDistance = 14f * density }
            .width(64.dp).height(240.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF7E4C0), Color(0xFFE9CFA0), Color(0xFFD9B878))))
            .border(1.5.dp, Color(0xFF8B5A2B).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (flipY < 90f) FrontFace(lang, stick) else BackFace(lang, stick, Modifier.graphicsLayer { rotationY = 180f })
    }
}

/** 签正面：等级大字（纯黑楷书，古代木刻风格） */
@Composable
private fun FrontFace(lang: String, stick: FortuneStick) {
    val core = when (lang) { "en" -> FortuneData.levelCoreEN[stick.level]; "zh-TW" -> FortuneData.levelCoreTW[stick.level]; else -> FortuneData.levelCoreCN[stick.level] }
    if (lang == "en") {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            core.split(" ").forEach { word ->
                Text(word, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp), fontFamily = KaiTiFont, color = InkBlack, textAlign = TextAlign.Center)
            }
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            core.forEach { ch ->
                Text(ch.toString(), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp), fontFamily = KaiTiFont, color = InkBlack)
            }
            Spacer(Modifier.height(4.dp))
            Text(t(lang, "籤", "籤", ""), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), fontFamily = KaiTiFont, color = InkBlack)
        }
    }
}

/** 签背面：签诗（繁体竖排楷书）+ 解曰 */
@Composable
private fun BackFace(lang: String, stick: FortuneStick, mirror: Modifier) {
    Box(mirror.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (lang == "en") {
                stick.poemEN.split("\n").forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp, lineHeight = 11.sp), color = InkBlack, textAlign = TextAlign.Center)
                }
            } else {
                // 竖排四句
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Top) {
                    stick.poemTW.split("\n").forEach { line ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            line.forEach { ch ->
                                Text(ch.toString(), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), fontFamily = KaiTiFont, color = InkBlack)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("— " + t(lang, FortuneData.meaningCN[stick.level], FortuneData.meaningTW[stick.level], FortuneData.meaningEN[stick.level]) + " —",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp, lineHeight = 11.sp),
                fontFamily = KaiTiFont, color = InkBlack, textAlign = TextAlign.Center)
        }
    }
}
