package com.woodenfish.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.launch

// ─── L10n helper ───
private fun t(lang: String, zhCN: String, zhTW: String, en: String) = when (lang) {
    "zh-TW" -> zhTW
    "en" -> en
    else -> zhCN
}

// ─── Predefined intervals ───
private val hourPresets = listOf(1, 2, 3, 6, 12)
private val minPresets = listOf(15, 30, 45, 60, 120)
private val dayPresets = listOf(1, 2, 3, 5, 7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WoodenFishScreen(viewModel: WoodenFishViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lang = state.language
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val menuExpanded = state.showMenu
    var settingsPage by remember { mutableStateOf<String?>(null) }
    var showAbout by remember { mutableStateOf(false) }

    WoodenFishTheme(themeMode = state.themeMode, darkTheme = isDark) {
        // ── Legal pages (full‑screen) ──
        when (state.showLegalPage) {
            "agreement" -> {
                Scaffold(topBar = { TopAppBar(title = { Text(t(lang, "用户协议", "用戶協議", "User Agreement")) }, navigationIcon = { TextButton(onClick = { viewModel.dismissLegalPage() }) { Text(t(lang, "返回","返回","Back"), color = MaterialTheme.colorScheme.primary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }) { p -> Box(Modifier.padding(p)) { UserAgreementScreen {} } }
                return@WoodenFishTheme
            }
            "privacy" -> {
                Scaffold(topBar = { TopAppBar(title = { Text(t(lang, "隐私政策", "隱私政策", "Privacy Policy")) }, navigationIcon = { TextButton(onClick = { viewModel.dismissLegalPage() }) { Text(t(lang, "返回","返回","Back"), color = MaterialTheme.colorScheme.primary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }) { p -> Box(Modifier.padding(p)) { PrivacyPolicyScreen {} } }
                return@WoodenFishTheme
            }
        }

        // ── Settings sub‑pages ──
        when (settingsPage) {
            "notify" -> {
                Scaffold(topBar = { TopAppBar(title = { Text(t(lang, "提醒设置", "提醒設定", "Notifications")) }, navigationIcon = { TextButton(onClick = { settingsPage = null }) { Text(t(lang, "返回","返回","Back"), color = MaterialTheme.colorScheme.primary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }) { p -> Box(Modifier.padding(p)) { NotifySettingsPage(state, viewModel, lang) } }
                return@WoodenFishTheme
            }
            "appearance" -> {
                Scaffold(topBar = { TopAppBar(title = { Text(t(lang, "界面与语言", "界面與語言", "Appearance")) }, navigationIcon = { TextButton(onClick = { settingsPage = null }) { Text(t(lang, "返回","返回","Back"), color = MaterialTheme.colorScheme.primary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }) { p -> Box(Modifier.padding(p)) { AppearanceSettingsPage(state, viewModel, lang) } }
                return@WoodenFishTheme
            }
        }

        if (showAbout) {
            Scaffold(topBar = { TopAppBar(title = { Text(t(lang, "关于", "關於", "About")) }, navigationIcon = { TextButton(onClick = { showAbout = false; viewModel.resetAboutClicks() }) { Text(t(lang, "返回","返回","Back"), color = MaterialTheme.colorScheme.primary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }) { p -> Box(Modifier.padding(p)) { AboutPage(viewModel, lang, context) } }
            return@WoodenFishTheme
        }

        // ── Drawer + Main Content ──
        ModalNavigationDrawer(
            drawerState = rememberDrawerState(if (menuExpanded) DrawerValue.Open else DrawerValue.Closed),
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                    DrawerContent(state, viewModel, lang, onSettings = { settingsPage = it }, onAbout = { showAbout = true })
                }
            },
            content = {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Doki", fontWeight = FontWeight.Medium) },
                            navigationIcon = {
                                TextButton(onClick = { viewModel.toggleMenu() }) {
                                    Text("☰", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background,
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            CounterDisplay(state.todayCount, state.totalCount, lang)
                            Spacer(Modifier.height(32.dp))
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                                state.particles.forEach { particle -> PlusOneAnim(particle) }
                                FishCanvas(
                                    hammerOffset = state.hammerOffset,
                                    onTap = { viewModel.onFishTap() },
                                    modifier = Modifier.size(200.dp)
                                )
                            }
                        }
                        // Celebration
                        AnimatedVisibility(visible = state.showCelebration, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
                            Text(t(lang, "🎉 功德圆满 🎉\n今日已敲 1000 次！", "🎉 功德圓滿 🎉\n今日已敲 1000 次！", "🎉 1000 Taps!\nMerit complete!"), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp), color = MaterialTheme.colorScheme.tertiary, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        )

        // ── Agreement Dialog ──
        if (state.showAgreement) {
            AgreementDialog(onAgree = { viewModel.agreeToTerms() }, onViewAgreement = { viewModel.showLegalPage("agreement") }, onViewPrivacy = { viewModel.showLegalPage("privacy") }, lang)
        }
    }
}

// ═══════════════════ CANVAS FISH ═══════════════════
@Composable
private fun FishCanvas(hammerOffset: Float, onTap: () -> Unit, modifier: Modifier) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(hammerOffset) {
        if (hammerOffset > 0.5f) { scale.snapTo(0.93f); scale.animateTo(1f, spring(DampingRatioMediumBouncy, StiffnessHigh)) }
    }

    Canvas(
        modifier = modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onTap() }
    ) {
        val w = size.width; val h = size.height; val cx = w / 2; val cy = h / 2

        // ── Fish body (ellipse) ──
        drawOval(color = Color(0xFF8D6E63), topLeft = Offset(cx - w * 0.44f, cy - h * 0.25f), size = Size(w * 0.88f, h * 0.5f))
        // ── Top highlight ──
        drawOval(color = Color(0xFFA1887F), topLeft = Offset(cx - w * 0.38f, cy - h * 0.22f), size = Size(w * 0.76f, h * 0.36f))
        // ── Slit (mouth) ──
        val slitPath = Path().apply {
            moveTo(cx - w * 0.2f, cy + h * 0.1f)
            cubicTo(cx - w * 0.1f, cy + h * 0.18f, cx + w * 0.1f, cy + h * 0.18f, cx + w * 0.2f, cy + h * 0.1f)
        }
        drawPath(slitPath, color = Color(0xFF4E342E), style = Stroke(width = 3f))
        // ── Strike dot ──
        drawCircle(color = Color(0xFFD7CCC8), radius = w * 0.04f, center = Offset(cx, cy - h * 0.02f))

        // ── Mallet ──
        val hammerAngle = hammerOffset * -25f  // swing from top-right
        val pivotX = cx + w * 0.35f; val pivotY = cy - h * 0.4f
        drawContext.canvas.save()
        drawContext.canvas.rotate(hammerAngle, pivotX, pivotY)

        // Mallet handle
        drawLine(color = Color(0xFF6D4C41), start = Offset(pivotX, pivotY), end = Offset(pivotX + w * 0.08f, pivotY + h * 0.35f), strokeWidth = 5f)
        // Mallet head
        drawCircle(color = Color(0xFF8D6E63), radius = w * 0.09f, center = Offset(pivotX + w * 0.08f, pivotY + h * 0.35f))
        drawContext.canvas.restore()
    }
}

// ═══════════════════ +1 PARTICLE ═══════════════════
@Composable
private fun PlusOneAnim(particle: PlusOneParticle) {
    val color = PlusOneColors[particle.colorIndex % PlusOneColors.size]
    val animatedY = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }

    // Position: 0=左上, 1=正上, 2=右上
    val offsetX = when (particle.positionIndex) { 0 -> -60f; 2 -> 60f; else -> 0f }

    LaunchedEffect(particle.id) {
        launch { animatedY.animateTo(-180f, tween(900, easing = FastOutSlowInEasing)) }
        launch { kotlinx.coroutines.delay(500); animAlpha.animateTo(0f, tween(400)) }
    }

    Box(Modifier.offset(x = offsetX.dp, y = animatedY.value.dp).graphicsLayer { alpha = animAlpha.value }) {
        Text("+1", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ═══════════════════ COUNTER ═══════════════════
@Composable
private fun CounterDisplay(todayCount: Int, totalCount: Long, lang: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(t(lang, "今日功德", "今日功德", "Today's Merit"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("$todayCount", style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Light, fontSize = 56.sp), color = MaterialTheme.colorScheme.onSurface)
        if (todayCount > 0) {
            Spacer(Modifier.height(2.dp))
            Text("${t(lang, "总计", "總計", "Total")} $totalCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

// ═══════════════════ DRAWER MENU ═══════════════════
@Composable
private fun DrawerContent(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String, onSettings: (String) -> Unit, onAbout: () -> Unit) {
    Column(Modifier.fillMaxHeight().padding(vertical = 24.dp)) {
        Text("Doki", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        DrawerItem(t(lang, "⚙ 提醒设置", "⚙ 提醒設定", "⚙ Notifications")) { viewModel.closeMenu(); onSettings("notify") }
        DrawerItem(t(lang, "🎨 界面与语言", "🎨 界面與語言", "🎨 Appearance")) { viewModel.closeMenu(); onSettings("appearance") }
        DrawerItem(t(lang, "📖 用户协议", "📖 用戶協議", "📖 User Agreement")) { viewModel.closeMenu(); viewModel.showLegalPage("agreement") }
        DrawerItem(t(lang, "🔒 隐私政策", "🔒 隱私政策", "🔒 Privacy")) { viewModel.closeMenu(); viewModel.showLegalPage("privacy") }
        DrawerItem(t(lang, "ℹ 关于", "ℹ 關於", "ℹ About")) { viewModel.closeMenu(); onAbout() }
    }
}

@Composable
private fun DrawerItem(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp))
    }
}

// ═══════════════════ NOTIFICATION SETTINGS ═══════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifySettingsPage(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Enable toggle
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(t(lang, "开启提醒", "開啟提醒", "Enable"), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = state.notifyEnabled, onCheckedChange = { viewModel.updateNotificationEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
        }
        // Permission hint
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
            Text(t(lang, "💡 提示：请在系统设置中确保已授予 Doki 通知权限，否则提醒可能无法送达。", "💡 提示：請在系統設定中確保已授予 Doki 通知權限。", "💡 Tip: Please ensure notification permission is granted in system settings."), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }

        if (state.notifyEnabled) {
            Divider()

            // Random toggle
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(t(lang, "随机时间", "隨機時間", "Random"), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = state.randomTime, onCheckedChange = { viewModel.updateRandomTime(it) }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
            }

            if (state.randomTime) {
                // Random time range
                Text("${t(lang, "提醒时段", "提醒時段", "Time range")}: ${state.notifyStart}:00 - ${state.notifyEnd}:00", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HourPicker2(t(lang, "起始","起始","Start"), state.notifyStart, { viewModel.updateTimeRange(it, state.notifyEnd) })
                    HourPicker2(t(lang, "结束","結束","End"), state.notifyEnd, { viewModel.updateTimeRange(state.notifyStart, it) })
                }
            } else {
                // Custom interval
                CustomInterval(state, viewModel, lang)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomInterval(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String) {
    val units = listOf(t(lang, "分钟", "分鐘", "min"), t(lang, "小时", "小時", "hr"), t(lang, "天", "天", "day"))
    var inputText by remember(state.notifyIntervalValue, state.notifyIntervalUnit) { mutableStateOf(state.notifyIntervalValue.toString()) }
    var selectedUnit by remember(state.notifyIntervalUnit) { mutableStateOf(state.notifyIntervalUnit) }
    var showPresets by remember { mutableStateOf(false) }

    Text("${t(lang, "自定义间隔", "自訂間隔", "Custom interval")}（${t(lang, "从设定后开始计时", "從設定後開始計時", "counts from now")}）", style = MaterialTheme.typography.bodyMedium)

    // Input row
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = inputText, onValueChange = { inputText = it.filter { c -> c.isDigit() }.take(4) }, modifier = Modifier.weight(1f), singleLine = true, label = { Text(t(lang, "数值", "數值", "Value")) }, shape = RoundedCornerShape(8.dp))
        // Unit selector
        units.forEach { u ->
            FilterChip(selected = selectedUnit == u, onClick = { selectedUnit = u }, label = { Text(u, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
        }
    }

    // Presets
    TextButton(onClick = { showPresets = !showPresets }) { Text(t(lang, "预设值 ▼", "預設值 ▼", "Presets ▼"), fontSize = 13.sp) }
    if (showPresets) {
        val list = when {
            selectedUnit.contains(t(lang, "小时", "小時", "hr")) -> hourPresets
            selectedUnit.contains(t(lang, "分钟", "分鐘", "min")) -> minPresets
            else -> dayPresets
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            list.forEach { v ->
                AssistChip(onClick = { inputText = v.toString() }, label = { Text("$v", fontSize = 12.sp) }, shape = RoundedCornerShape(8.dp))
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Button(onClick = {
        val v = inputText.toIntOrNull() ?: 1
        viewModel.updateInterval(v, selectedUnit)
    }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Text(t(lang, "保存间隔", "儲存間隔", "Save Interval"))
    }
}

@Composable
private fun HourPicker2(label: String, value: Int, onChange: (Int) -> Unit, range: IntRange = 6..23) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp))
        Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = range.first.toFloat()..range.last.toFloat(), steps = range.last - range.first - 1, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary), modifier = Modifier.weight(1f))
        Text("$value", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(24.dp))
    }
}

// ═══════════════════ APPEARANCE SETTINGS ═══════════════════
@Composable
private fun AppearanceSettingsPage(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(t(lang, "语言", "語言", "Language"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LangChip("简体中文", "zh-CN", state.language) { viewModel.setLanguage(it) }
            LangChip("繁體中文", "zh-TW", state.language) { viewModel.setLanguage(it) }
            LangChip("English", "en", state.language) { viewModel.setLanguage(it) }
        }

        Divider()

        Text(t(lang, "界面主题", "界面主題", "Theme"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChip(t(lang, "跟随系统", "跟隨系統", "System"), ThemeMode.SYSTEM, state.themeMode) { viewModel.setThemeMode(it) }
            ThemeChip(t(lang, "白昼模式", "白晝模式", "Light"), ThemeMode.LIGHT, state.themeMode) { viewModel.setThemeMode(it) }
            ThemeChip(t(lang, "夜间模式", "夜間模式", "Dark"), ThemeMode.DARK, state.themeMode) { viewModel.setThemeMode(it) }
        }
    }
}

@Composable
private fun LangChip(label: String, code: String, current: String, onSelect: (String) -> Unit) {
    FilterChip(selected = current == code, onClick = { onSelect(code) }, label = { Text(label, fontSize = 13.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
}

@Composable
private fun ThemeChip(label: String, mode: ThemeMode, current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    FilterChip(selected = current == mode, onClick = { onSelect(mode) }, label = { Text(label, fontSize = 13.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
}

// ═══════════════════ ABOUT PAGE ═══════════════════
@Composable
private fun AboutPage(viewModel: WoodenFishViewModel, lang: String, context: android.content.Context) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(40.dp))
        Text("Doki", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(t(lang, "电子木鱼 · 功德 +1", "電子木魚 · 功德 +1", "Digital Wooden Fish"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        // Version — tap 5 times
        val clickCount = viewModel.state.collectAsState().value.aboutClickCount
        TextButton(onClick = {
            viewModel.onVersionClick()
            if (clickCount + 1 >= 5) {
                viewModel.resetAboutClicks()
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:zhif0776@hotmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Doki 应用反馈")
                }
                context.startActivity(intent)
            }
        }) {
            Text("${t(lang, "版本", "版本", "Version")} 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text(t(lang, "连续点击版本号 5 次向开发者反馈", "連續點擊版本號 5 次向開發者反饋", "Tap version 5 times to send feedback"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(24.dp))
        Text(t(lang, "一款简洁的电子木鱼应用。\n敲击木鱼，积累功德，平和心境。", "一款簡潔的電子木魚應用。\n敲擊木魚，積累功德，平和心境。", "A simple digital wooden fish app.\nTap to accumulate merit, find peace of mind."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// ═══════════════════ AGREEMENT DIALOG ═══════════════════
@Composable
private fun AgreementDialog(onAgree: () -> Unit, onViewAgreement: () -> Unit, onViewPrivacy: () -> Unit, lang: String) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(t(lang, "欢迎使用 Doki", "歡迎使用 Doki", "Welcome to Doki"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(t(lang, "在使用前，请阅读并同意以下协议：", "在使用前，請閱讀並同意以下協議：", "Please read and agree before using:"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onViewAgreement, modifier = Modifier.fillMaxWidth()) { Text("📄 ${t(lang, "查看《用户协议》", "查看《用戶協議》", "View User Agreement")}", color = MaterialTheme.colorScheme.primary) }
                TextButton(onClick = onViewPrivacy, modifier = Modifier.fillMaxWidth()) { Text("🔒 ${t(lang, "查看《隐私政策》", "查看《隱私政策》", "View Privacy Policy")}", color = MaterialTheme.colorScheme.primary) }
                Divider()
                Text(t(lang, "点击同意即表示您已阅读并同意以上协议。", "點擊同意即表示您已閱讀並同意以上協議。", "Tapping Agree means you accept the terms."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onAgree, Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text(t(lang, "同意并继续", "同意並繼續", "Agree & Continue")) }
            }
        }
    }
}
