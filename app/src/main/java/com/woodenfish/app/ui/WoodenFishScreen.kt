package com.woodenfish.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woodenfish.app.PlusOneParticle
import com.woodenfish.app.WoodenFishState
import com.woodenfish.app.WoodenFishViewModel
import com.woodenfish.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WoodenFishScreen(viewModel: WoodenFishViewModel) {
    val state by viewModel.state.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    WoodenFishTheme {
        // --- Legal pages (full-screen) ---
        when (state.showLegalPage) {
            "agreement" -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("用户协议") },
                            navigationIcon = {
                                TextButton(onClick = { viewModel.dismissLegalPage() }) {
                                    Text("返回", color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                            )
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        UserAgreementScreen(onBack = { viewModel.dismissLegalPage() })
                    }
                }
                return@WoodenFishTheme
            }
            "privacy" -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("隐私政策") },
                            navigationIcon = {
                                TextButton(onClick = { viewModel.dismissLegalPage() }) {
                                    Text("返回", color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                            )
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        PrivacyPolicyScreen(onBack = { viewModel.dismissLegalPage() })
                    }
                }
                return@WoodenFishTheme
            }
        }

        // --- Main screen ---
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Doki", fontWeight = FontWeight.Medium) },
                    actions = {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "菜单",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("用户协议") },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.showLegalPage("agreement")
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("隐私政策") },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.showLegalPage("privacy")
                                    },
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
                        state.particles.forEach { particle ->
                            PlusOneAnimation(particle = particle)
                        }
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

        // --- First-launch agreement dialog ---
        if (state.showAgreement) {
            AgreementDialog(
                onAgree = { viewModel.agreeToTerms() },
                onViewAgreement = { viewModel.showLegalPage("agreement") },
                onViewPrivacy = { viewModel.showLegalPage("privacy") },
            )
        }
    }
}

@Composable
private fun AgreementDialog(
    onAgree: () -> Unit,
    onViewAgreement: () -> Unit,
    onViewPrivacy: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* cannot dismiss — must agree */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "欢迎使用 Doki",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = "Doki 是一款电子木鱼应用，帮助您在忙碌中寻找片刻宁静。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = "在使用前，请阅读并同意以下协议：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Links to agreements
                TextButton(
                    onClick = onViewAgreement,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("📄 查看《用户协议》", color = MaterialTheme.colorScheme.primary)
                }

                TextButton(
                    onClick = onViewPrivacy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("🔒 查看《隐私政策》", color = MaterialTheme.colorScheme.primary)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                Text(
                    text = "点击"同意"即表示您已阅读并同意以上协议。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = onAgree,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("同意并继续", modifier = Modifier.padding(vertical = 4.dp))
                }
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
                indication = null,
                onClick = {
                    pressed = true
                    onTap()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        }
    }

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
            animatedOffsetY.animateTo(-200f, tween(1000, easing = FastOutSlowInEasing))
        }
        launch {
            animatedScale.animateTo(1.4f, tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            kotlinx.coroutines.delay(600)
            animatedAlpha.animateTo(0f, tween(400, easing = LinearEasing))
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            if (state.notifyEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

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
