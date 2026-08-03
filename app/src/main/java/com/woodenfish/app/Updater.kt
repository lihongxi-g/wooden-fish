package com.woodenfish.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

data class UpdateInfo(val version: String, val apkUrl: String)

object Updater {
    private const val REPO = "lihongxi-g/wooden-fish"

    /** 后台检查 GitHub Releases 最新版，onResult(null) = 无更新或检查失败 */
    fun checkForUpdate(context: Context, onResult: (UpdateInfo?) -> Unit) {
        Thread {
            var info: UpdateInfo? = null
            try {
                val conn = java.net.URL("https://api.github.com/repos/$REPO/releases/latest")
                    .openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                if (conn.responseCode == 200) {
                    val obj = JSONObject(conn.inputStream.bufferedReader().readText())
                    val tag = obj.optString("tag_name", "").removePrefix("v")
                    val arr = obj.optJSONArray("assets")
                    var apkUrl: String? = null
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val a = arr.getJSONObject(i)
                            if (a.optString("name").endsWith(".apk")) {
                                apkUrl = a.optString("browser_download_url")
                                break
                            }
                        }
                    }
                    val current = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    if (tag.isNotBlank() && apkUrl != null && isNewer(tag, current)) {
                        info = UpdateInfo(tag, apkUrl)
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {}
            onResult(info)
        }.start()
    }

    /** 后台下载 APK 到 cache，onProgress(0-100)，onResult(file) 失败为 null */
    fun downloadApk(context: Context, url: String, onProgress: (Int) -> Unit, onResult: (File?) -> Unit) {
        Thread {
            var file: File? = null
            try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 60000
                conn.setRequestProperty("User-Agent", "Doki-Updater/2.0")
                val total = conn.contentLength
                val target = File(context.cacheDir, "update.apk")
                target.delete() // 先删旧文件，避免残留损坏文件
                conn.inputStream.use { ins ->
                    java.io.FileOutputStream(target).use { fos ->
                        val buf = ByteArray(8192)
                        var read: Int
                        var done = 0
                        while (ins.read(buf).also { read = it } != -1) {
                            fos.write(buf, 0, read)
                            done += read
                            if (total > 0) onProgress((done * 100 / total).coerceIn(0, 100))
                        }
                    }
                }
                // 校验下载的是有效 APK（损坏文件会导致安装崩溃）
                val pkgInfo = try {
                    context.packageManager.getPackageArchiveInfo(target.absolutePath, 0)
                } catch (_: Exception) { null }
                if (pkgInfo != null && target.length() > 1_000_000) file = target
                else target.delete()
            } catch (_: Exception) {}
            onResult(file)
        }.start()
    }

    /** 安装 APK：优先系统 PackageInstaller 会话（不依赖 ROM 的 intent 路由），失败回退 ACTION_VIEW */
    fun install(context: Context, file: File) {
        val appCtx = context.applicationContext
        try {
            val installer = appCtx.packageManager.packageInstaller
            val params = android.content.pm.PackageInstaller.SessionParams(android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(appCtx.packageName)
                setSize(file.length())
            }
            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)
            try {
                file.inputStream().use { input ->
                    session.openWrite("doki_update", 0, file.length()).use { out -> input.copyTo(out) }
                }
                val pi = PendingIntent.getBroadcast(
                    appCtx, 100, Intent(appCtx, UpdateResultReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                session.commit(pi.intentSender)
                session.close()
            } catch (e: Exception) {
                try { session.abandon() } catch (_: Exception) {}
                throw e
            }
            return
        } catch (e: Exception) {
            // 回退：ACTION_VIEW 调起系统安装器
            try {
                val uri = FileProvider.getUriForFile(appCtx, "${appCtx.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appCtx.startActivity(intent)
            } catch (e2: Exception) {
                try {
                    android.widget.Toast.makeText(appCtx, "无法打开安装器，请到 GitHub 手动下载：github.com/lihongxi-g/wooden-fish/releases", android.widget.Toast.LENGTH_LONG).show()
                } catch (_: Exception) {}
            }
        }
    }

    /** 版本号比较："1.9" vs "1.10" → 后者新 */
    private fun isNewer(tag: String, current: String): Boolean {
        fun parse(v: String): List<Int> = v.split(".").mapNotNull { it.toIntOrNull() }
        val a = parse(tag); val b = parse(current)
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
