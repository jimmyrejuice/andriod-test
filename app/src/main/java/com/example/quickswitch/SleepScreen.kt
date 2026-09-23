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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

// 睡觉 = 深蓝紫（夜空），起床 = 橙色（日出）
private val SleepColor = Color(0xFF3949AB)
private val WakeColor = Color(0xFFFB8C00)

@Composable
fun SleepScreen(refreshTick: Int = 0) {
    val context = LocalContext.current
    var records by remember { mutableStateOf(SleepStorage.loadAll(context)) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var expanded by remember { mutableStateOf(true) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

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
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SleepColor,
                    contentColor = Color.White
                )
            ) {
                                Text("🌙", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text("我睡了", fontSize = 15.sp)
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
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WakeColor,
                    contentColor = Color.White
                )
            ) {
                                Text("☀️", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text("我起了", fontSize = 15.sp)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上个月")
                }
                Text(
                    text = "${currentMonth.year}年${currentMonth.monthValue}月",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下个月")
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "收起" else "展开"
                    )
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

            Spacer(Modifier.height(6.dp))

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
    // D2：圆角方块，今天用描边
    val shape = RoundedCornerShape(10.dp)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val showOutline = isToday && !isSelected

    val borderMod = if (showOutline) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(bgColor)
            .then(borderMod)
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
    val monday = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
    val weekData = (0 until 7).map { i ->
        val date = monday.plusDays(i.toLong())
        date to records[date.toString()]
    }
    val hasAnyData = weekData.any {
        it.second?.let { r -> r.sleepMinutes != null || r.wakeMinutes != null } == true
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("本周睡眠", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            if (hasAnyData) {
                SleepChart(
                    weekData = weekData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            } else {
                // G3：空状态
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "还没有记录，点下方按钮记录吧",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(color = SleepColor, label = "睡觉", filled = true)
                LegendItem(color = WakeColor, label = "起床", filled = false)
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    filled: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (filled) color else Color.Transparent)
                .then(
                    if (filled) Modifier
                    else Modifier.border(1.5.dp, color, CircleShape)
                )
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SleepChart(
    weekData: List<Pair<LocalDate, SleepRecord?>>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leftPad = 40.dp.toPx()
        val bottomPad = 28.dp.toPx()
        val topPad = 12.dp.toPx()
        val rightPad = 8.dp.toPx()
        val chartW = w - leftPad - rightPad
        val chartH = h - topPad - bottomPad

        val yMin = -6f
        val yMax = 12f
        val yRange = yMax - yMin

        fun yToPx(y: Float): Float = topPad + chartH * (1f - (y - yMin) / yRange)

        // E1：y 轴刻度 + 文字
        val yTicks = listOf(
            -6f to "18时",
            0f to "0时",
            6f to "6时",
            12f to "12时"
        )
        yTicks.forEach { (y, label) ->
            drawLine(
                color = gridColor,
                start = Offset(leftPad, yToPx(y)),
                end = Offset(w - rightPad, yToPx(y)),
                strokeWidth = if (y == 0f) 1.5.dp.toPx() else 0.5.dp.toPx()
            )
            val layout = textMeasurer.measure(
                text = label,
                style = TextStyle(color = axisTextColor, fontSize = 10.sp)
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = leftPad - layout.size.width - 6.dp.toPx(),
                    y = yToPx(y) - layout.size.height / 2f
                )
            )
        }

        // E1：x 轴星期文字
        val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
        val dayW = chartW / 7f
        val centers = (0 until 7).map { i -> leftPad + dayW * (i + 0.5f) }

        dayLabels.forEachIndexed { i, label ->
            val layout = textMeasurer.measure(
                text = label,
                style = TextStyle(color = axisTextColor, fontSize = 10.sp)
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = centers[i] - layout.size.width / 2f,
                    y = topPad + chartH + 8.dp.toPx()
                )
            )
        }

        // E3：睡觉深蓝紫实心、起床橙色空心
        weekData.forEachIndexed { i, (_, record) ->
            val cx = centers[i]
            if (record != null) {
                val sleepY = record.sleepMinutes?.let { minutesToY(it) }
                val wakeY = record.wakeMinutes?.let { minutesToY(it) }

                if (sleepY != null && wakeY != null) {
                    drawLine(
                        color = gridColor,
                        start = Offset(cx, yToPx(sleepY)),
                        end = Offset(cx, yToPx(wakeY)),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                sleepY?.let {
                    drawCircle(
                        color = SleepColor,
                        radius = 6.dp.toPx(),
                        center = Offset(cx, yToPx(it))
                    )
                }
                wakeY?.let {
                    drawCircle(
                        color = WakeColor,
                        radius = 6.dp.toPx(),
                        center = Offset(cx, yToPx(it)),
                        style = Stroke(width = 2.5.dp.toPx())
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
