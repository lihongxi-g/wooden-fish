package com.woodenfish.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UserAgreementScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "用户协议",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "更新日期：2024年1月1日\n生效日期：2024年1月1日",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Section(title = "1. 服务说明") {
            Text(body = "Doki（以下简称"本应用"）是一款电子木鱼应用，旨在为用户提供减压、冥想辅助和娱乐功能。本应用所有功能均为免费提供。")
        }

        Section(title = "2. 用户行为规范") {
            Text(body = "您在使用本应用时，应遵守中华人民共和国相关法律法规。不得利用本应用从事任何违法违规活动。本应用仅用于娱乐和减压目的，不涉及任何宗教活动或迷信行为。")
        }

        Section(title = "3. 免责声明") {
            Text(body = "本应用提供的计数、提醒等功能仅供娱乐参考，不构成任何形式的建议或承诺。开发者不对因使用本应用而产生的任何直接或间接损失承担责任。")
        }

        Section(title = "4. 知识产权") {
            Text(body = "本应用的所有代码、设计和内容均受知识产权保护。未经授权，不得对本应用进行反向工程、修改或再分发。")
        }

        Section(title = "5. 协议修改") {
            Text(body = "开发者保留随时修改本协议的权利。修改后的协议将在应用内公布，继续使用即视为同意修改后的协议。如不同意修改，请停止使用本应用。")
        }

        Section(title = "6. 联系方式") {
            Text(body = "如有任何问题或建议，请通过应用内反馈渠道或 GitHub Issues 联系我们。")
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "隐私政策",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "更新日期：2024年1月1日\n生效日期：2024年1月1日",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Section(title = "1. 信息收集") {
            Text(body = "本应用仅在本地存储以下数据：\n• 每日敲击计数和总计数\n• 通知偏好设置（时间、频率等）\n• 首次使用标记和协议同意状态\n\n所有数据均存储在您的设备本地，我们不会收集、上传或分享任何个人信息。本应用不需要网络连接即可正常使用。")
        }

        Section(title = "2. 权限使用") {
            Text(body = "本应用申请以下权限及其用途：\n• 振动权限：用于点击反馈和庆祝效果\n• 通知权限：用于定时提醒您使用本应用\n• 开机自启权限：用于在设备重启后恢复提醒闹钟\n\n您可以随时在系统设置中关闭这些权限。")
        }

        Section(title = "3. 数据安全") {
            Text(body = "所有数据仅存储在您的设备上。我们不会收集、传输或处理您的任何数据。卸载应用将永久删除所有本地数据。")
        }

        Section(title = "4. 第三方服务") {
            Text(body = "本应用不使用任何第三方分析工具、广告 SDK 或数据收集服务。本应用完全离线运行。")
        }

        Section(title = "5. 儿童隐私") {
            Text(body = "本应用面向所有年龄段的用户，内容健康无害。我们不收集任何用户的个人信息，包括儿童。")
        }

        Section(title = "6. 隐私政策更新") {
            Text(body = "我们可能会不定期更新本隐私政策。更新后将在应用内通知您。继续使用即视为接受更新后的政策。")
        }

        Section(title = "7. 联系我们") {
            Text(body = "如对本隐私政策有任何疑问，请通过 GitHub Issues 联系我们。")
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    content()
}

@Composable
private fun Text(body: String) {
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
    )
}
