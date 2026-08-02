package com.woodenfish.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woodenfish.app.PlusOneParticle
import com.woodenfish.app.WoodenFishState
import com.woodenfish.app.WoodenFishViewModel
import com.woodenfish.app.ui.theme.*

@Composable
fun WoodenFishScreen(viewModel: WoodenFishViewModel) {
    val state by viewModel.state.collectAsState()

    WoodenFishTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Counter display
                CounterDisplay(
                    todayCount = state.todayCount,
                    totalCount = state.totalCount,
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Fish body + particles overlay
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(220.dp)
                ) {
                    // Floating +1 particles
                    state.particles.forEach { particle ->
                        PlusOneAnimation(particle = particle)
                    }

                    // The fish
                    FishButton(onTap = { viewModel.onFishTap() })
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Settings toggle button
                TextButton(onClick = { viewModel.toggleSettings() }) {
                    Text(
                        text = if (state.showSettings) "收起设置 ▲" else "提醒设置 ▼",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }

                // Settings panel
                AnimatedVisibility(
                    visible = state.showSettings,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    SettingsPanel(state = state, viewModel = viewModel)
                }
            }

            // Celebration overlay
            AnimatedVisibility(
                visible = state.showCelebration,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                CelebrationOverlay()
            }
        }
    }
}

@Composable
private fun CounterDisplay(todayCount: Int, totalCount: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "今日功德",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$todayCount",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Light,
                fontSize = 56.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (todayCount > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "总计 $totalCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun FishButton(onTap: () -> Unit) {
    // Press animation
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "fishScale"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // no ripple — clean
                onClick = {
                    pressed = true
                    onTap()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner circle — the "fish" body
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            // Small center dot — like a fish eye / mallet strike point
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        }
    }

    // Reset pressed state after animation
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(80)
            pressed = false
        }
    }
}

@Composable
private fun PlusOneAnimation(particle: PlusOneParticle) {
    val color = PlusOneColors[particle.colorIndex % PlusOneColors.size]

    val animatedOffsetY = remember { Animatable(0f) }
    val animatedAlpha = remember { Animatable(1f) }
    val animatedScale = remember { Animatable(1f) }

    LaunchedEffect(particle.id) {
        launch {
            animatedOffsetY.animateTo(
                targetValue = -200f,
                animationSpec = tween(1000, easing = FastOutSlowInEasing)
            )
        }
        launch {
            animatedScale.animateTo(
                targetValue = 1.4f,
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        }
        launch {
            kotlinx.coroutines.delay(600)
            animatedAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(400, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .offset(x = particle.offsetX.dp, y = animatedOffsetY.value.dp)
            .graphicsLayer {
                alpha = animatedAlpha.value
                scaleX = animatedScale.value
                scaleY = animatedScale.value
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+1",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun CelebrationOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🎉 功德圆满 🎉\n今日已敲 1000 次！",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            ),
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SettingsPanel(state: WoodenFishState, viewModel: WoodenFishViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // no shadow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Enable toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "开启提醒",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = state.notifyEnabled,
                    onCheckedChange = { viewModel.updateNotificationEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                )
            }

            // Only show detailed settings when enabled
            if (state.notifyEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Random / Fixed time toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "随机时间",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked = state.randomTime,
                        onCheckedChange = { viewModel.updateRandomTime(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    )
                }

                if (state.randomTime) {
                    // Time range
                    Text(
                        "提醒时段: ${state.notifyStart}:00 - ${state.notifyEnd}:00",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HourPicker(
                            label = "起始",
                            value = state.notifyStart,
                            onValueChange = { viewModel.updateTimeRange(it, state.notifyEnd) }
                        )
                        HourPicker(
                            label = "结束",
                            value = state.notifyEnd,
                            onValueChange = { viewModel.updateTimeRange(state.notifyStart, it) }
                        )
                    }

                    // Count
                    Text(
                        "每日提醒次数: ${state.notifyCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = state.notifyCount.toFloat(),
                        onValueChange = { viewModel.updateNotificationCount(it.toInt()) },
                        valueRange = 1f..8f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                } else {
                    // Fixed time picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HourPicker(
                            label = "时",
                            value = state.notifyHour,
                            onValueChange = { viewModel.updateNotificationTime(it, state.notifyMinute) },
                            range = 0..23,
                        )
                        HourPicker(
                            label = "分",
                            value = state.notifyMinute,
                            onValueChange = { viewModel.updateNotificationTime(state.notifyHour, it) },
                            range = 0..59,
                        )
                    }
                    Text(
                        "每天固定 ${String.format("%02d", state.notifyHour)}:${String.format("%02d", state.notifyMinute)} 提醒",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HourPicker(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange = 6..23,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            )
        )
        Text(
            text = "$value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(24.dp),
        )
    }
}
