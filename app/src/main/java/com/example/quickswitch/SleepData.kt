package com.example.quickswitch

import android.app.AlarmManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONObject
import java.time.LocalDate
import java.util.Calendar

/**
 * 一条睡眠记录。
 * dateKey：入睡日期（yyyy-MM-dd），这条记录的 key。
 * sleepMinutes：入睡时刻，0:00 起的分钟数（0-1439）。
 * wakeMinutes：起床时刻，0:00 起的分钟数（0-1439）。
 */
data class SleepRecord(
    val dateKey: String,
    val sleepMinutes: Int?,
    val wakeMinutes: Int?
)

object SleepStorage {
    private const val PREF = "sleep_data"
    private const val KEY_RECORDS = "records"

    fun loadAll(context: Context): Map<String, SleepRecord> {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val json = sp.getString(KEY_RECORDS, "{}") ?: "{}"
        val obj = try { JSONObject(json) } catch (e: Exception) { JSONObject() }
        val map = mutableMapOf<String, SleepRecord>()
        obj.keys().forEach { key ->
            val item = obj.getJSONObject(key)
            map[key] = SleepRecord(
                dateKey = key,
                sleepMinutes = if (item.has("sleep") && !item.isNull("sleep")) item.getInt("sleep") else null,
                wakeMinutes = if (item.has("wake") && !item.isNull("wake")) item.getInt("wake") else null
            )
        }
        return map
    }

    private fun saveAll(context: Context, records: Map<String, SleepRecord>) {
        val obj = JSONObject()
        records.forEach { (k, r) ->
            val item = JSONObject()
            r.sleepMinutes?.let { item.put("sleep", it) }
            r.wakeMinutes?.let { item.put("wake", it) }
            obj.put(k, item)
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_RECORDS, obj.toString()).apply()
    }

    /** 记录睡觉：key = 今天，写入入睡时间。 */
    fun recordSleepNow(context: Context) {
        val records = loadAll(context).toMutableMap()
        val now = Calendar.getInstance()
        val dateKey = LocalDate.now().toString()
        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val old = records[dateKey]
        records[dateKey] = SleepRecord(
            dateKey = dateKey,
            sleepMinutes = minutes,
            wakeMinutes = old?.wakeMinutes
        )
        saveAll(context, records)
    }

    /** 记录起床：找最近一条有入睡没起床的记录补上，找不到就在今天新建一条。 */
    fun recordWakeNow(context: Context) {
        val records = loadAll(context).toMutableMap()
        val now = Calendar.getInstance()
        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = LocalDate.now()

        val candidates = listOf(today.toString(), today.minusDays(1).toString())
        var targetKey: String? = null
        for (k in candidates) {
            val r = records[k]
            if (r != null && r.sleepMinutes != null && r.wakeMinutes == null) {
                targetKey = k
                break
            }
        }
        if (targetKey != null) {
            val old = records[targetKey]!!
            records[targetKey] = old.copy(wakeMinutes = minutes)
        } else {
            val todayKey = today.toString()
            val old = records[todayKey]
            records[todayKey] = SleepRecord(
                dateKey = todayKey,
                sleepMinutes = old?.sleepMinutes,
                wakeMinutes = minutes
            )
        }
        saveAll(context, records)
    }

    /**
     * App 启动时调用：如果有入睡没起床的记录，且闹钟时间已过（早上），用闹钟时间补记起床。
     * 用户手动点过 a2 的记录已经有 wakeMinutes，不会被覆盖。
     */
    fun autoFillWakeByAlarm(context: Context) {
        val alarmMinutes = getNextAlarmMinutes(context) ?: return
        val records = loadAll(context).toMutableMap()
        val today = LocalDate.now()
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val candidates = listOf(today.minusDays(1).toString(), today.toString())
        var changed = false
        for (k in candidates) {
            val r = records[k] ?: continue
            if (r.sleepMinutes != null && r.wakeMinutes == null) {
                // 只处理早上时段的闹钟（< 12:00），且已过
                if (alarmMinutes < 12 * 60 && alarmMinutes < nowMinutes) {
                    records[k] = r.copy(wakeMinutes = alarmMinutes)
                    changed = true
                }
            }
        }
        if (changed) saveAll(context, records)
    }

    /** 读取系统下一个闹钟的时刻（分钟数）。没有闹钟返回 null。 */
    fun getNextAlarmMinutes(context: Context): Int? {
        return try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val info = am.nextAlarmClock ?: return null
            val cal = Calendar.getInstance().apply { timeInMillis = info.triggerTime }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) {
            null
        }
    }

    /** 导出所有记录到 Downloads/sleep/sleep_data.csv。成功返回 true。 */
    fun exportCsv(context: Context): Boolean {
        val records = loadAll(context).toSortedMap()
        if (records.isEmpty()) return false

        val sb = StringBuilder()
        sb.append("日期,入睡时间,起床时间\n")
        for ((k, r) in records) {
            val s = r.sleepMinutes?.let { formatMinutes(it) } ?: ""
            val w = r.wakeMinutes?.let { formatMinutes(it) } ?: ""
            sb.append("$k,$s,$w\n")
        }

        return try {
            val fileName = "sleep_data.csv"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/sleep"
                    )
                }
            }
            val resolver = context.contentResolver
            // 先删旧的同名文件，避免冲突
            try {
                resolver.delete(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                    arrayOf(fileName)
                )
            } catch (_: Exception) {}
            val uri = resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
            ) ?: return false
            resolver.openOutputStream(uri)?.use { os ->
                os.write(sb.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从 CSV 导入。按日期合并：同日期覆盖，新日期新增。
     * 返回成功导入的记录条数；失败返回 -1。
     * CSV 格式（第一行表头可省略）：
     *   日期,入睡时间,起床时间
     *   2025-09-22,23:15,07:30
     */
    fun importCsv(context: Context, uri: Uri): Int {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            } ?: return -1

            val records = loadAll(context).toMutableMap()
            var count = 0
            var isFirstLine = true

            text.lineSequence().forEach { raw ->
                val line = raw.trim().removePrefix("\uFEFF")
                if (line.isEmpty()) return@forEach

                // 跳过表头
                if (isFirstLine) {
                    isFirstLine = false
                    if (line.startsWith("日期") || line.startsWith("date", ignoreCase = true)) {
                        return@forEach
                    }
                }

                val parts = line.split(",")
                if (parts.size < 2) return@forEach

                val dateKey = parts[0].trim()
                if (dateKey.isEmpty()) return@forEach

                val sleep = parseTime(parts.getOrNull(1)?.trim() ?: "")
                val wake = parseTime(parts.getOrNull(2)?.trim() ?: "")

                records[dateKey] = SleepRecord(dateKey, sleep, wake)
                count++
            }

            if (count == 0) return -1
            saveAll(context, records)
            count
        } catch (e: Exception) {
            -1
        }
    }

    /** "23:15" → 1395。空白或格式错误返回 null。 */
    private fun parseTime(s: String): Int? {
        if (s.isBlank()) return null
        val parts = s.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun formatMinutes(m: Int): String = "%02d:%02d".format(m / 60, m % 60)
}
