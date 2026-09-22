package com.example.quickswitch

import android.app.AlarmManager
import android.content.ContentValues
import android.content.Context
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

    fun formatMinutes(m: Int): String = "%02d:%02d".format(m / 60, m % 60)
}
