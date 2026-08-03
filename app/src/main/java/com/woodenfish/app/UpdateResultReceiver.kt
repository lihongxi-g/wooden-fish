package com.woodenfish.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

/** 接收系统 PackageInstaller 安装结果（用户确认安装后回调） */
class UpdateResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val code = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        when (code) {
            PackageInstaller.STATUS_SUCCESS -> {
                try { Toast.makeText(context.applicationContext, "Doki 更新成功", Toast.LENGTH_LONG).show() } catch (_: Exception) {}
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> return // 等待用户确认
            else -> {
                try { Toast.makeText(context.applicationContext, "Doki 更新失败（$code），请到 GitHub 重新下载", Toast.LENGTH_LONG).show() } catch (_: Exception) {}
            }
        }
    }
}
