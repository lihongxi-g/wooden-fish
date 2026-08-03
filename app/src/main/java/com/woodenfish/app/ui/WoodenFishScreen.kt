package com.woodenfish.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woodenfish.app.PlusOneParticle
import com.woodenfish.app.WoodenFishState
import com.woodenfish.app.WoodenFishViewModel
import com.woodenfish.app.ui.theme.*
import kotlinx.coroutines.launch

private fun t(lang: String, zhCN: String, zhTW: String, en: String) = when (lang) { "zh-TW" -> zhTW; "en" -> en; else -> zhCN }

private val hourPresets = listOf(1, 2, 3, 6, 12)
private val minPresets = listOf(15, 30, 45, 60, 120)
private val dayPresets = listOf(1, 2, 3, 5, 7)

private fun isPackageInstalled(context: android.content.Context, pkg: String): Boolean = try {
    context.packageManager.getPackageInfo(pkg, 0); true
} catch (_: Exception) { false }

private fun openCalendarStore(context: android.content.Context) {
    val url = "https://play.google.com/store/apps/details?id=com.google.android.calendar"
    // 1. 优先 Google Play 商店
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.android.vending"))
        return
    } catch (_: Exception) {}
    // 2. 没有 Play 商店则用系统自带商店（market:// 路由到默认应用商店）
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.calendar")))
        return
    } catch (_: Exception) {}
    // 3. 最后回退浏览器打开 Google Play 网页
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WoodenFishScreen(viewModel: WoodenFishViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lang = state.language
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var page by remember { mutableStateOf<String?>(null) } // null=home, notify/appearance/language/about/sound

    LaunchedEffect(state.showMenu) { if (state.showMenu) drawerState.open() else drawerState.close() }

    // Toast
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // BackHandler for sub-pages
    BackHandler(enabled = page != null) { page = null }

    WoodenFishTheme(themeMode = state.themeMode, themeColorIndex = state.themeColorIndex, darkTheme = isDark) {
        // Legal pages
        if (state.showLegalPage == "agreement") {
            Scaffold(topBar = { TopAppBar(title = { Text(t(lang, "用户协议", "用戶協議", "User Agreement")) }, navigationIcon = { TextButton(onClick = { viewModel.dismissLegalPage() }) { Text(t(lang, "返回","返回","Back"), color = MaterialTheme.colorScheme.primary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }) { p -> Box(Modifier.padding(p)) { UserAgreementScreen {} } }
            return@WoodenFishTheme
        }
        if (state.showLegalPage == "privacy") {
            Scaffold(topBar = { TopAppBar(title = { Text(t(lang, "隐私政策", "隱私政策", "Privacy Policy")) }, navigationIcon = { TextButton(onClick = { viewModel.dismissLegalPage() }) { Text(t(lang, "返回","返回","Back"), color = MaterialTheme.colorScheme.primary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }) { p -> Box(Modifier.padding(p)) { PrivacyPolicyScreen {} } }
            return@WoodenFishTheme
        }

        // Sub-pages
        when (page) {
            "notify" -> { SubPage(t(lang, "提醒设置", "提醒設定", "Notifications"), onBack = { page = null }) { NotifySettingsPage(state, viewModel, lang, context) }; return@WoodenFishTheme }
            "appearance" -> { SubPage(t(lang, "界面主题", "界面主題", "Theme"), onBack = { page = null }) { AppearancePage(state, viewModel, lang, isDark) }; return@WoodenFishTheme }
            "language" -> { SubPage(t(lang, "语言", "語言", "Language"), onBack = { page = null }) { LanguagePage(state, viewModel, lang) }; return@WoodenFishTheme }
            "sound" -> { SubPage(t(lang, "声音与震动", "聲音與震動", "Sound & Vibration"), onBack = { page = null }) { SoundVibrationPage(state, viewModel, lang) }; return@WoodenFishTheme }
            "about" -> { SubPage(t(lang, "关于", "關於", "About"), onBack = { page = null; viewModel.resetAboutClicks() }) { AboutPage(viewModel, lang, context) }; return@WoodenFishTheme }
        }

        // Main
        ModalNavigationDrawer(
            drawerState = drawerState, gesturesEnabled = true,
            drawerContent = { ModalDrawerSheet(Modifier.width(280.dp)) { DrawerContent(state, viewModel, lang, onPage = { page = it }) } },
            content = {
                Scaffold(
                    topBar = { TopAppBar(title = { Text("Doki", fontWeight = FontWeight.Medium) }, navigationIcon = { TextButton(onClick = { viewModel.toggleMenu() }) { Text("\u2630", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
                    containerColor = MaterialTheme.colorScheme.background,
                ) { pd -> Box(Modifier.fillMaxSize().padding(pd)) {
                    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CounterDisplay(state.todayCount, state.totalCount, lang)
                        Spacer(Modifier.height(32.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                            state.particles.forEach { PlusOneAnim(it, state.tapSpeed) }
                            Box(Modifier.size(200.dp).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.onFishTap() }, contentAlignment = Alignment.Center) {
                                FishCanvas(hammerOffset = state.hammerOffset, speed = state.tapSpeed, modifier = Modifier.size(190.dp))
                            }
                        }
                    }
                    AnimatedVisibility(visible = state.showCelebration, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
                        Text(t(lang, "\uD83C\uDF89 功德圆满 \uD83C\uDF89\n今日已敲 1000 次！", "\uD83C\uDF89 功德圓滿 \uD83C\uDF89\n今日已敲 1000 次！", "\uD83C\uDF89 1000 Taps!\nMerit complete!"), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp), color = MaterialTheme.colorScheme.tertiary, textAlign = TextAlign.Center)
                    }
                } }
            }
        )
        if (state.showAgreement) AgreementDialog(onAgree = { viewModel.agreeToTerms() }, onViewAgreement = { viewModel.showLegalPage("agreement") }, onViewPrivacy = { viewModel.showLegalPage("privacy") }, lang)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { TextButton(onClick = onBack) { Text("\u2190", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }) { p -> Box(Modifier.padding(p)) { content() } }
}

// ═══════════════ FISH ═══════════════
@Composable
private fun FishCanvas(hammerOffset: Float, speed: Float, modifier: Modifier) {
    val scale = remember { Animatable(1f) }
    val wobble = remember { Animatable(0f) }
    val hammerAnim = remember { Animatable(0f) }
    LaunchedEffect(hammerOffset) {
        if (hammerOffset > 0.5f) {
            scale.snapTo(0.9f)
            wobble.snapTo(-4f)
            hammerAnim.snapTo(1f)
            launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh * speed)) }
            launch { wobble.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium * speed)) }
            launch { hammerAnim.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium * speed)) }
        }
    }
    Canvas(modifier = modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value; rotationZ = wobble.value }) {
        val w = size.width; val h = size.height; val cx = w / 2; val cy = h / 2
        drawOval(color = Color.Black.copy(alpha = 0.15f), topLeft = Offset(cx - w * 0.4f, cy - h * 0.18f + 4.dp.toPx()), size = Size(w * 0.8f, h * 0.46f))
        drawOval(brush = Brush.verticalGradient(listOf(Color(0xFFA1887F), Color(0xFF6D4C41), Color(0xFF4E342E)), startY = cy - h * 0.25f, endY = cy + h * 0.25f), topLeft = Offset(cx - w * 0.42f, cy - h * 0.25f), size = Size(w * 0.84f, h * 0.5f))
        drawOval(color = Color(0xFFBCAAA4), topLeft = Offset(cx - w * 0.42f, cy - h * 0.23f), size = Size(w * 0.84f, h * 0.12f))
        for (i in -2..2) { val lx = cx + i * w * 0.12f; drawOval(color = Color.Black.copy(alpha = 0.06f), topLeft = Offset(lx - w * 0.15f, cy - h * 0.15f), size = Size(w * 0.3f, h * 0.3f)) }
        val slit = Path().apply { moveTo(cx - w * 0.22f, cy + h * 0.08f); cubicTo(cx - w * 0.1f, cy + h * 0.2f, cx + w * 0.1f, cy + h * 0.2f, cx + w * 0.22f, cy + h * 0.08f); cubicTo(cx + w * 0.1f, cy + h * 0.14f, cx - w * 0.1f, cy + h * 0.14f, cx - w * 0.22f, cy + h * 0.08f) }
        drawPath(slit, color = Color(0xFF3E2723)); drawPath(slit, color = Color.Black.copy(alpha = 0.3f), style = Stroke(1f))
        drawCircle(color = Color(0xFFEFEBE9), radius = w * 0.08f, center = Offset(cx, cy - h * 0.02f))
        drawCircle(color = Color(0xFFD7CCC8), radius = w * 0.045f, center = Offset(cx, cy - h * 0.02f))
        val ha = hammerAnim.value * -30f; val px = cx + w * 0.38f; val py = cy - h * 0.35f
        drawContext.canvas.save(); drawContext.canvas.translate(px, py); drawContext.canvas.rotate(ha); drawContext.canvas.translate(-px, -py)
        drawLine(color = Color(0xFF5D4037), start = Offset(px, py), end = Offset(px + w * 0.06f, py + h * 0.38f), strokeWidth = 5f)
        drawCircle(color = Color.Black.copy(alpha = 0.15f), radius = w * 0.095f, center = Offset(px + w * 0.06f + 2.dp.toPx(), py + h * 0.38f + 2.dp.toPx()))
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFBCAAA4), Color(0xFF6D4C41)), center = Offset(px + w * 0.06f, py + h * 0.38f), radius = w * 0.095f), radius = w * 0.095f, center = Offset(px + w * 0.06f, py + h * 0.38f))
        drawContext.canvas.restore()
    }
}

@Composable private fun PlusOneAnim(particle: PlusOneParticle, speed: Float) {
    val color = PlusOneColors[particle.colorIndex % PlusOneColors.size]
    val aY = remember { Animatable(0f) }; val aA = remember { Animatable(0f) }; val aS = remember { Animatable(0.4f) }
    val drift = ((particle.id % 5) - 2) * 6f
    val travel = if (particle.dy < -80f) 30f else 50f
    LaunchedEffect(particle.id) {
        launch { aY.animateTo(-travel, tween((950 / speed).toInt(), easing = FastOutSlowInEasing)) }
        launch { aA.animateTo(1f, tween((100 / speed).toInt())) }
        launch { aS.animateTo(1.15f, tween((160 / speed).toInt(), easing = FastOutSlowInEasing)); aS.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh * speed)) }
        kotlinx.coroutines.delay((650 / speed).toLong())
        launch { aA.animateTo(0f, tween((350 / speed).toInt())) }
    }
    Box(Modifier.offset(x = (particle.dx + drift).dp, y = (particle.dy + aY.value).dp).graphicsLayer { alpha = aA.value; scaleX = aS.value; scaleY = aS.value }) { Text("+1", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color) }
}

@Composable private fun CounterDisplay(todayCount: Int, totalCount: Long, lang: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(t(lang, "今日功德", "今日功德", "Today's Merit"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("$todayCount", style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Light, fontSize = 56.sp), color = MaterialTheme.colorScheme.onSurface)
        if (todayCount > 0) { Spacer(Modifier.height(2.dp)); Text("${t(lang, "总计", "總計", "Total")} $totalCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
    }
}

// ═══════════════ DRAWER (no emoji) ═══════════════
@Composable private fun DrawerContent(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String, onPage: (String) -> Unit) {
    Column(Modifier.fillMaxHeight().padding(vertical = 24.dp)) {
        Text("Doki", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Divider(modifier = Modifier.padding(horizontal = 16.dp)); Spacer(Modifier.height(8.dp))
        Item(t(lang, "提醒设置", "提醒設定", "Notifications")) { viewModel.closeMenu(); onPage("notify") }
        Item(t(lang, "界面主题", "界面主題", "Theme")) { viewModel.closeMenu(); onPage("appearance") }
        Item(t(lang, "语言", "語言", "Language")) { viewModel.closeMenu(); onPage("language") }
        Item(t(lang, "声音与震动", "聲音與震動", "Sound & Vibration")) { viewModel.closeMenu(); onPage("sound") }
        Item(t(lang, "用户协议", "用戶協議", "User Agreement")) { viewModel.closeMenu(); viewModel.showLegalPage("agreement") }
        Item(t(lang, "隐私政策", "隱私政策", "Privacy")) { viewModel.closeMenu(); viewModel.showLegalPage("privacy") }
        Item(t(lang, "关于", "關於", "About")) { viewModel.closeMenu(); onPage("about") }
    }
}
@Composable private fun Item(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) }
}

// ═══════════════ NOTIFICATION SETTINGS ═══════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun NotifySettingsPage(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String, context: android.content.Context) {
    val gcalInstalled = remember { isPackageInstalled(context, "com.google.android.calendar") }
    var showGcalDialog by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }
    var availableCalendars by remember { mutableStateOf<List<com.woodenfish.app.CalendarSync.CalendarInfo>>(emptyList()) }

    fun pickCalendar(c: com.woodenfish.app.CalendarSync.CalendarInfo) {
        showCalendarPicker = false
        if (viewModel.enableFixedTime(c.id, c.name)) {
            viewModel.toast(t(lang, "已写入${c.name}，由日历提醒", "已寫入${c.name}，由日曆提醒", "Saved to ${c.name}"))
        } else {
            viewModel.toast(t(lang, "写入日历失败", "寫入日曆失敗", "Failed to save to calendar"))
        }
    }

    // 检测日历并进入启用流程：无日历→提示；多个→弹窗选择；仅 Google 未装→先提示安装
    val startFixedTime: () -> Unit = {
        val cals = com.woodenfish.app.CalendarSync.listCalendars(context)
        availableCalendars = cals
        when {
            cals.isEmpty() -> viewModel.toast(t(lang, "没有可用的日历，请先在系统日历中添加账号", "沒有可用的日曆，請先在系統日曆中新增帳號", "No calendar available, add an account first"))
            !cals.any { it.isGoogle } && !gcalInstalled -> showGcalDialog = true
            cals.size == 1 -> pickCalendar(cals[0])
            else -> showCalendarPicker = true
        }
    }
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.READ_CALENDAR] == true && grants[Manifest.permission.WRITE_CALENDAR] == true) {
            startFixedTime()
        } else {
            viewModel.toast(t(lang, "需要日历权限才能使用固定时间提醒", "需要日曆權限才能使用固定時間提醒", "Calendar permission required"))
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(t(lang, "开启提醒", "開啟提醒", "Enable"), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = state.notifyEnabled, onCheckedChange = { viewModel.updateNotificationEnabled(it); viewModel.toast(if (it) t(lang, "提醒已开启", "提醒已開啟", "Notifications enabled") else t(lang, "提醒已关闭", "提醒已關閉", "Notifications disabled")) }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
            Text(t(lang, "提示：请在系统设置中确保已授予 Doki 通知权限，否则提醒可能无法送达。", "提示：請在系統設定中確保已授予 Doki 通知權限。", "Tip: Please ensure notification permission is granted in system settings."), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
        if (state.notifyEnabled) { Divider()
            Text(t(lang, "提醒方式", "提醒方式", "Reminder mode"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip(t(lang, "随机时间", "隨機時間", "Random"), state.randomTime && !state.fixedTimeEnabled) { viewModel.selectRandomTime() }
                ModeChip(t(lang, "自定义间隔", "自訂間隔", "Interval"), !state.randomTime && !state.fixedTimeEnabled) { viewModel.selectInterval() }
                ModeChip(t(lang, "固定时间·日历", "固定時間·日曆", "Fixed (Calendar)"), state.fixedTimeEnabled) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startFixedTime()
                    } else {
                        calendarLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                    }
                }
            }
            when {
                state.fixedTimeEnabled -> {
                    Text(t(lang, "自定义时间（每天固定这个时间提醒）", "自訂時間（每天固定這個時間提醒）", "Custom time (remind daily at this fixed time)"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TimePickerButton(state.fixedTimeMin, lang) { viewModel.updateFixedTimeMin(it) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(t(lang, "提醒日历：", "提醒日曆：", "Calendar: ") + (state.selectedCalendarName ?: t(lang, "系统日历", "系統日曆", "System")), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        TextButton(onClick = { startFixedTime() }) { Text(t(lang, "切换", "切換", "Change"), fontSize = 13.sp) }
                    }
                    Text(t(lang, "提醒已写入系统日历（Google 日历需登录 Google 账号并开启同步），由日历 App 到点提醒，Doki 无需后台运行。可在日历中修改或删除。", "提醒已寫入系統日曆（Google 日曆需登入 Google 帳號並開啟同步），由日曆 App 到點提醒，Doki 無需後台運行。可在日曆中修改或刪除。", "Reminder saved to system calendar (Google Calendar needs a Google account with sync on). The calendar app alerts you; Doki runs nothing in background. Editable in calendar."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!gcalInstalled) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(t(lang, "未检测到 Google 日历：仅本地提醒，无云端同步", "未偵測到 Google 日曆：僅本地提醒，無雲端同步", "Google Calendar not found: local reminder only, no cloud sync"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1f))
                            TextButton(onClick = { openCalendarStore(context) }) { Text(t(lang, "去安装", "去安裝", "Install"), fontSize = 13.sp) }
                        }
                    }
                }
                state.randomTime -> {
                    Text(t(lang, "提醒时段（每天随机 3 个时间提醒）", "提醒時段（每天隨機 3 個時間提醒）", "Time range (3 random reminders daily)"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TimeRow(t(lang, "起始时间", "起始時間", "Start"), state.notifyStartMin, lang) { m ->
                        if (m >= state.notifyEndMin) viewModel.toast(t(lang, "起始时间需早于结束时间", "起始時間需早於結束時間", "Start must be earlier than end"))
                        else viewModel.updateNotifyStartMin(m)
                    }
                    TimeRow(t(lang, "结束时间", "結束時間", "End"), state.notifyEndMin, lang) { m ->
                        if (m <= state.notifyStartMin) viewModel.toast(t(lang, "结束时间需晚于起始时间", "結束時間需晚於起始時間", "End must be later than start"))
                        else viewModel.updateNotifyEndMin(m)
                    }
                }
                else -> { CustomInterval(state, viewModel, lang) }
            }
        }
    }
    if (showGcalDialog) {
        AlertDialog(
            onDismissRequest = { showGcalDialog = false },
            title = { Text(t(lang, "未检测到 Google 日历", "未偵測到 Google 日曆", "Google Calendar not found")) },
            text = { Text(t(lang, "Doki 仍会写入系统日历，手机自带日历可以正常提醒；但要同步到云端需要 Google 日历。要现在去安装吗？", "Doki 仍會寫入系統日曆，手機內建日曆可以正常提醒；但要同步到雲端需要 Google 日曆。要現在去安裝嗎？", "Doki will still save to the system calendar and your built-in calendar app will remind you. Cloud sync requires Google Calendar. Install it now?")) },
            confirmButton = { TextButton(onClick = { showGcalDialog = false; openCalendarStore(context) }) { Text(t(lang, "安装 Google 日历", "安裝 Google 日曆", "Install Google Calendar")) } },
            dismissButton = { TextButton(onClick = { showGcalDialog = false; if (availableCalendars.isNotEmpty()) pickCalendar(availableCalendars[0]) else viewModel.toast(t(lang, "没有可用的日历", "沒有可用的日曆", "No calendar available")) }) { Text(t(lang, "继续使用系统日历", "繼續使用系統日曆", "Use system calendar")) } }
        )
    }
    if (showCalendarPicker) {
        AlertDialog(
            onDismissRequest = { showCalendarPicker = false },
            title = { Text(t(lang, "选择提醒日历", "選擇提醒日曆", "Choose reminder calendar")) },
            text = {
                Column {
                    availableCalendars.forEach { c ->
                        TextButton(onClick = { pickCalendar(c) }, modifier = Modifier.fillMaxWidth()) {
                            Text((if (c.isGoogle) "Google 日历 · " else "") + c.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (availableCalendars.none { it.isGoogle }) {
                        Spacer(Modifier.height(8.dp))
                        Text(t(lang, "未检测到 Google 日历账号：仅本地提醒，安装 Google 日历并登录后可云端同步", "未偵測到 Google 日曆帳號：僅本地提醒，安裝 Google 日曆並登入後可雲端同步", "No Google Calendar account found: local reminder only. Install Google Calendar and sign in for cloud sync"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCalendarPicker = false }) { Text(t(lang, "取消", "取消", "Cancel")) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun CustomInterval(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String) {
    val units = listOf(t(lang, "分钟", "分鐘", "min"), t(lang, "小时", "小時", "hr"), t(lang, "天", "天", "day"))
    var txt by remember(state.notifyIntervalValue, state.notifyIntervalUnit) { mutableStateOf(state.notifyIntervalValue.toString()) }
    var unit by remember(state.notifyIntervalUnit) { mutableStateOf(state.notifyIntervalUnit) }
    var show by remember { mutableStateOf(false) }
    Text("${t(lang, "自定义间隔", "自訂間隔", "Custom interval")}（${t(lang, "从设定后开始计时", "從設定後開始計時", "counts from now")}）", style = MaterialTheme.typography.bodyMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = txt, onValueChange = { txt = it.filter { c -> c.isDigit() }.take(4) }, modifier = Modifier.weight(1f), singleLine = true, label = { Text(t(lang, "数值", "數值", "Value")) }, shape = RoundedCornerShape(8.dp))
        units.forEach { u -> FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)) }
    }
    TextButton(onClick = { show = !show }) { Text(t(lang, "预设值", "預設值", "Presets"), fontSize = 13.sp) }
    if (show) { val list = when { unit.contains(t(lang, "小时","小時","hr")) -> hourPresets; unit.contains(t(lang, "分钟","分鐘","min")) -> minPresets; else -> dayPresets }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { list.forEach { v -> AssistChip(onClick = { txt = v.toString() }, label = { Text("$v", fontSize = 12.sp) }, shape = RoundedCornerShape(8.dp)) } } }
    Spacer(Modifier.height(8.dp))
    Button(onClick = { val v = txt.toIntOrNull() ?: 1; viewModel.updateInterval(v, unit); viewModel.toast(t(lang, "间隔已保存", "間隔已儲存", "Interval saved")) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text(t(lang, "保存", "儲存", "Save")) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TimeRow(label: String, minute: Int, lang: String, onChange: (Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val hour = minute / 60; val min = minute % 60
    Row(Modifier.fillMaxWidth().clickable { showPicker = true }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(String.format("%02d:%02d", hour, min), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
    if (showPicker) {
        val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = min, is24Hour = true)
        Dialog(onDismissRequest = { showPicker = false }) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = pickerState)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showPicker = false }) { Text(t(lang, "取消", "取消", "Cancel")) }
                        TextButton(onClick = { onChange(pickerState.hour * 60 + pickerState.minute); showPicker = false }) { Text(t(lang, "确定", "確定", "OK"), color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TimePickerButton(minute: Int, lang: String, onChange: (Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val hour = minute / 60; val min = minute % 60
    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Text(String.format("%02d:%02d", hour, min), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
    if (showPicker) {
        val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = min, is24Hour = true)
        Dialog(onDismissRequest = { showPicker = false }) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = pickerState)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showPicker = false }) { Text(t(lang, "取消", "取消", "Cancel")) }
                        TextButton(onClick = { onChange(pickerState.hour * 60 + pickerState.minute); showPicker = false }) { Text(t(lang, "确定", "確定", "OK"), color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        }
    }
}

// ═══════════════ APPEARANCE (theme color only) ═══════════════
@Composable private fun AppearancePage(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String, isDark: Boolean) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(t(lang, "主题颜色", "主題顏色", "Theme Color"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        allThemes.forEachIndexed { i, tc ->
            val name = when (lang) { "zh-TW" -> tc.nameTW; "en" -> tc.nameEN; else -> tc.name }
            val bg = if (isDark) tc.darkScheme.primary else tc.lightScheme.primary
            Row(Modifier.fillMaxWidth().clickable { viewModel.setThemeColor(i) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(bg))
                Spacer(Modifier.width(12.dp))
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                if (state.themeColorIndex == i) Text("\u2713", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Divider()
        Text(t(lang, "界面模式", "界面模式", "Theme Mode"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChip(t(lang, "跟随系统", "跟隨系統", "System"), ThemeMode.SYSTEM, state.themeMode) { viewModel.setThemeMode(it) }
            ThemeChip(t(lang, "白昼模式", "白晝模式", "Light"), ThemeMode.LIGHT, state.themeMode) { viewModel.setThemeMode(it) }
            ThemeChip(t(lang, "夜间模式", "夜間模式", "Dark"), ThemeMode.DARK, state.themeMode) { viewModel.setThemeMode(it) }
        }
    }
}

// ═══════════════ LANGUAGE ═══════════════
@Composable private fun LanguagePage(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple("简体中文", "zh-CN", "Simplified Chinese"), Triple("繁體中文", "zh-TW", "Traditional Chinese"), Triple("English", "en", "English")).forEach { (label, code, _) ->
            Row(Modifier.fillMaxWidth().clickable { viewModel.setLanguage(code) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                if (state.language == code) Text("\u2713", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═══════════════ SOUND & VIBRATION ═══════════════
@Composable private fun SoundVibrationPage(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(t(lang, "声音反馈", "聲音回饋", "Sound Feedback"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (state.soundSupported) {
            Text("${t(lang, "音量", "音量", "Volume")}: ${(state.soundVolume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(value = state.soundVolume, onValueChange = { viewModel.setSoundVolume(it) }, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
        } else {
            Slider(value = 0f, onValueChange = {}, enabled = false, colors = SliderDefaults.colors(disabledThumbColor = MaterialTheme.colorScheme.outline, disabledActiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
            Text(t(lang, "您的设备不支持此功能", "您的設備不支援此功能", "Your device does not support this feature"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }

        Divider()
        Text(t(lang, "震动反馈", "震動回饋", "Vibration"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (state.vibrationSupported) {
            Text("${t(lang, "强度", "強度", "Intensity")}: ${(state.vibrationIntensity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(value = state.vibrationIntensity, onValueChange = { viewModel.setVibrationIntensity(it) }, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
        } else {
            Slider(value = 0f, onValueChange = {}, enabled = false, colors = SliderDefaults.colors(disabledThumbColor = MaterialTheme.colorScheme.outline, disabledActiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
            Text(t(lang, "您的设备不支持此功能", "您的設備不支援此功能", "Your device does not support this feature"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }

        Divider()
        InteractionSpeedSection(state, viewModel, lang)
    }
}

// ═══════════════ INTERACTION SPEED ═══════════════
@Composable private fun InteractionSpeedSection(state: WoodenFishState, viewModel: WoodenFishViewModel, lang: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(t(lang, "互动速度", "互動速度", "Interaction speed"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.5f, 0.75f, 1.0f, 1.25f).forEach { s ->
                ModeChip(s.toString(), state.tapSpeed == s) { viewModel.setTapSpeed(s) }
            }
        }
        Text(t(lang, "敲击动画与反馈的速度：0.5 慢速、1.0 默认、1.25 快速", "敲擊動畫與反饋的速度：0.5 慢速、1.0 預設、1.25 快速", "Speed of tap animations: 0.5 slow, 1.0 default, 1.25 fast"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ThemeChip(label: String, mode: ThemeMode, current: ThemeMode, onSelect: (ThemeMode) -> Unit) { FilterChip(selected = current == mode, onClick = { onSelect(mode) }, label = { Text(label, fontSize = 13.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)) }

// ═══════════════ ABOUT ═══════════════
@Composable private fun AboutPage(viewModel: WoodenFishViewModel, lang: String, context: android.content.Context) {
    val cc = viewModel.state.collectAsState().value.aboutClickCount
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(40.dp))
        Text("Doki", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(t(lang, "电子木鱼", "電子木魚", "Digital Wooden Fish"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { viewModel.onVersionClick(); if (cc + 1 >= 5) { viewModel.resetAboutClicks(); context.startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:zhif0776@hotmail.com"); putExtra(Intent.EXTRA_SUBJECT, "Doki \u53CD\u9988") }) } }) {
            Text("${t(lang, "版本", "版本", "Version")} 2.0.2", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text(t(lang, "连续点击版本号 5 次向开发者反馈", "連續點擊版本號 5 次向開發者反饋", "Tap version 5 times to send feedback"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                com.woodenfish.app.Updater.checkForUpdate(context) { info ->
                    (context as? android.app.Activity)?.runOnUiThread {
                        if (info == null) {
                            Toast.makeText(context, t(lang, "已是最新版本", "已是最新版本", "Up to date"), Toast.LENGTH_SHORT).show()
                        } else {
                            android.app.AlertDialog.Builder(context)
                                .setTitle("发现新版本 v${info.version}")
                                .setMessage(t(lang, "是否下载更新？", "是否下載更新？", "Download update?"))
                                .setPositiveButton(t(lang, "更新", "更新", "Update")) { _, _ -> viewModel.downloadAndInstall(context, info.apkUrl) }
                                .setNegativeButton(t(lang, "稍后", "稍後", "Later"), null)
                                .show()
                        }
                    }
                }
            }, shape = RoundedCornerShape(8.dp)) {
                Text(t(lang, "检查更新", "檢查更新", "Check for updates"))
            }
            OutlinedButton(onClick = {
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lihongxi-g/wooden-fish/releases"))) } catch (_: Exception) {}
            }, shape = RoundedCornerShape(8.dp)) {
                Text(t(lang, "GitHub 下载", "GitHub 下載", "GitHub Releases"))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(t(lang, "一款简洁的电子木鱼应用。\n敲击木鱼，积累功德，平和心境。", "一款簡潔的電子木魚應用。\n敲擊木魚，積累功德，平和心境。", "A simple digital wooden fish app.\nTap to accumulate merit, find peace of mind."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// ═══════════════ AGREEMENT ═══════════════
@Composable private fun AgreementDialog(onAgree: () -> Unit, onViewAgreement: () -> Unit, onViewPrivacy: () -> Unit, lang: String) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(t(lang, "欢迎使用 Doki", "歡迎使用 Doki", "Welcome to Doki"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(t(lang, "在使用前，请阅读并同意以下协议：", "在使用前，請閱讀並同意以下協議：", "Please read and agree before using:"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onViewAgreement, Modifier.fillMaxWidth()) { Text(t(lang, "查看《用户协议》", "查看《用戶協議》", "View User Agreement"), color = MaterialTheme.colorScheme.primary) }
                TextButton(onClick = onViewPrivacy, Modifier.fillMaxWidth()) { Text(t(lang, "查看《隐私政策》", "查看《隱私政策》", "View Privacy Policy"), color = MaterialTheme.colorScheme.primary) }
                Divider()
                Text(t(lang, "点击同意即表示您已阅读并同意以上协议。", "點擊同意即表示您已閱讀並同意以上協議。", "Tapping Agree means you accept the terms."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onAgree, Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text(t(lang, "同意并继续", "同意並繼續", "Agree & Continue")) }
            }
        }
    }
}
