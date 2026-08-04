package com.woodenfish.app

import org.json.JSONArray
import org.json.JSONObject

/** 转盘分区：名称 + 权重（权重越大被指中的概率越高） */
data class SpinnerSegment(val name: String, val weight: Int)

fun List<SpinnerSegment>.toJson(): String {
    val arr = JSONArray()
    forEach { s ->
        arr.put(JSONObject().put("n", s.name).put("w", s.weight))
    }
    return arr.toString()
}

fun String?.parseSegments(fallback: List<SpinnerSegment>): List<SpinnerSegment> {
    if (this == null) return fallback
    return try {
        val arr = JSONArray(this)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            SpinnerSegment(o.optString("n", "").take(12), o.optInt("w", 1).coerceIn(0, 100))
        }.ifEmpty { fallback }
    } catch (_: Exception) { fallback }
}

fun defaultSegments(): List<SpinnerSegment> = listOf(
    SpinnerSegment("选项一", 1),
    SpinnerSegment("选项二", 1),
    SpinnerSegment("选项三", 1),
)
