package com.woodenfish.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.woodenfish.app.ui.WoodenFishScreen
import java.security.MessageDigest

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: WoodenFishViewModel

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val prefs = PreferencesManager(this)
        if (granted || !prefs.isNotificationEnabled()) {
            NotificationHelper(this).scheduleNotifications(prefs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 签名自校验：防二次打包（重签名的 APK 直接拒绝运行）
        if (!isOfficialSignature()) {
            android.app.AlertDialog.Builder(this)
                .setTitle("非官方版本")
                .setMessage("检测到 APK 签名与官方不一致，可能已被二次打包。请从官方渠道（GitHub Releases）安装。")
                .setPositiveButton("退出") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        viewModel = WoodenFishViewModel(application)
        NotificationHelper.createChannel(this)

        // Request notification permission on first launch (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Mark first launch
        val prefs = PreferencesManager(this)
        if (prefs.isFirstLaunch()) {
            prefs.markLaunched()
        }

        setContent {
            // 启动 3 秒后自动检查更新（仅提示跳转 GitHub，不下载）
            androidx.compose.runtime.LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                Updater.checkForUpdate(this@MainActivity) { info ->
                    if (info != null) {
                        runOnUiThread {
                            android.app.AlertDialog.Builder(this@MainActivity)
                                .setTitle("发现新版本 v${info.version}")
                                .setMessage("当前版本 v${currentVersion()}，请前往 GitHub 下载更新。")
                                .setPositiveButton("去 GitHub") { _, _ -> Updater.openReleases(this@MainActivity) }
                                .setNegativeButton("稍后", null)
                                .show()
                        }
                    }
                }
            }
            WoodenFishScreen(viewModel = viewModel)
        }
    }

    private fun currentVersion(): String =
        try { packageManager.getPackageInfo(packageName, 0).versionName } catch (_: Exception) { "?" }

    /** 官方签名 SHA-256（固定 keystore 证书指纹） */
    private fun isOfficialSignature(): Boolean {
        return try {
            val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
            }
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(sigs[0].toByteArray())
            val hex = digest.joinToString("") { "%02x".format(it) }
            hex == "9b4b143e3901ea778f7b7852d66727e2eefe852c458820fabf237da7d7ced8d7"
        } catch (_: Exception) {
            false
        }
    }
}
