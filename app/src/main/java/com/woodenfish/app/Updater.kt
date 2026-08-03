package com.woodenfish.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject

data class UpdateInfo(val version: String, val apkUrl: String)

object Updater {
    private const val REPO = "lihongxi-g/wooden-fish"
    private const val RELEASES_URL = "https://github.com/lihongxi-g/wooden-fish/releases"

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

    /** 打开 GitHub Releases 页面（浏览器） */
    fun openReleases(context: Context) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)))
        } catch (_: Exception) {}
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
