package com.example.quickswitch

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun SleepScreen(refreshTick: Int = 0) {
    val context = LocalContext.current
    var records by remember { mutableStateOf(SleepStorage.loadAll(context)) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var expanded by remember { mutableStateOf(true) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // 启动时 / 导入后，用闹钟补记并刷新
    LaunchedEffect(refreshTick) {
        SleepStorage.autoFillWakeByAlarm(context)
        records = SleepStorage.loadAll(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 96.dp)
        ) {
            CalendarCard(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                records = records,
                expanded = expanded,
                onMonthChange = { currentMonth = it },
                onDateSelect = { selectedDate = it },
                onToggleExpand = { expanded = !expanded }
            )

            if (!expanded) {
                Spacer(Modifier.height(12.dp))
                ChartCard(selectedDate = selectedDate, records = records)
            }
        }

        // 底部固定按钮
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    SleepStorage.recordSleepNow(context)
                    records = SleepStorage.loadAll(context)
                    Toast.makeText(context, "已记录睡觉时间", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = CircleShape
            ) {
                Text("我睡了", fontSize = 16.sp)
            }
            Button(
                onClick = {
                    SleepStorage.recordWakeNow(context)
                    records = SleepStorage.loadAll(context)
                    Toast.makeText(context, "已记录起床时间", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = CircleShape
            ) {
                Text("我起了", fontSize = 16.sp)
            }
        }
    }
}

// ---------------- 日历 ----------------

@Composable
fun CalendarCard(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    records: Map<String, SleepRecord>,
    expanded: Boolean,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onToggleExpand: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                    Text("◀", fontSize = 16.sp)
                }
                Text(
                    text = "${currentMonth.year}年${currentMonth.monthValue}月",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                    Text("▶", fontSize = 16.sp)
                }
                IconButton(onClick = onToggleExpand) {
                    Text(if (expanded) "▴" else "▾", fontSize = 18.sp)
                }
            }

            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (expanded) {
                MonthGrid(currentMonth, selectedDate, records, onDateSelect)
            } else {
                WeekRow(selectedDate, records, onDateSelect)
            }
        }
    }
}

@Composable
private fun MonthGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    records: Map<String, SleepRecord>,
    onDateSelect: (LocalDate) -> Unit
) {
    val firstDay = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val leadingBlanks = firstDay.dayOfWeek.value - 1
    val totalCells = ((leadingBlanks + daysInMonth + 6) / 7) * 7
    val rows = totalCells / 7

    Column {
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - leadingBlanks + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayNum)
                            DayCell(
                                date = date,
                                isSelected = date == selectedDate,
                                isToday = date == LocalDate.now(),
                                hasRecord = records.containsKey(date.toString()),
                                onClick = { onDateSelect(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekRow(
    selectedDate: LocalDate,
    records: Map<String, SleepRecord>,
    onDateSelect: (LocalDate) -> Unit
) {
    val monday = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
    Row(Modifier.fillMaxWidth()) {
        for (i in 0 until 7) {
            val date = monday.plusDays(i.toLong())
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                DayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == LocalDate.now(),
                    hasRecord = records.containsKey(date.toString()),
                    onClick = { onDateSelect(date) }
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasRecord: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
            if (hasRecord) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}

// ---------------- 图表 ----------------

@Composable
fun ChartCard(
    selectedDate: LocalDate,
    records: Map<String, SleepRecord>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("本周睡眠", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            val monday = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
            val weekData = (0 until 7).map { i ->
                val date = monday.plusDays(i.toLong())
                date to records[date.toString()]
            }

            SleepChart(
                weekData = weekData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text("实心=睡觉", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.size(4.dp))
                    Text("空心=起床", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SleepChart(
    weekData: List<Pair<LocalDate, SleepRecord?>>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leftPad = 44f
        val bottomPad = 30f
        val topPad = 10f
        val rightPad = 8f
        val chartW = w - leftPad - rightPad
        val chartH = h - topPad - bottomPad

        val yMin = -6f
        val yMax = 12f
        val yRange = yMax - yMin

        fun yToPx(y: Float): Float {
            return topPad + chartH * (1f - (y - yMin) / yRange)
        }

        // 画 y 轴刻度线
        val ticks = listOf(-6f, -3f, 0f, 3f, 6f, 9f, 12f)

        ticks.forEach { y ->
            drawLine(
                color = gridColor,
                start = Offset(leftPad, yToPx(y)),
                end = Offset(w - rightPad, yToPx(y)),
                strokeWidth = if (y == 0f) 1.5.dp.toPx() else 0.5.dp.toPx()
            )
        }

        // 每天的位置
        val dayW = chartW / 7f
        val centers = (0 until 7).map { i -> leftPad + dayW * (i + 0.5f) }

        // 画点
        weekData.forEachIndexed { i, (_, record) ->
            val cx = centers[i]
            if (record != null) {
                val sleepY = record.sleepMinutes?.let { minutesToY(it) }
                val wakeY = record.wakeMinutes?.let { minutesToY(it) }

                if (sleepY != null && wakeY != null) {
                    drawLine(
                        color = primaryColor,
                        start = Offset(cx, yToPx(sleepY)),
                        end = Offset(cx, yToPx(wakeY)),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                sleepY?.let {
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = Offset(cx, yToPx(it))
                    )
                }
                wakeY?.let {
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = Offset(cx, yToPx(it)),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

/** 0-1439 分钟数 → y 轴小时数。早上时间为正，晚上时间为负。 */
private fun minutesToY(minutes: Int): Float {
    val hour = minutes / 60f
    return if (hour >= 12f) hour - 24f else hour
}
