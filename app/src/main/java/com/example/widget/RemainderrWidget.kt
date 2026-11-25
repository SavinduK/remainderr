package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class RemainderrWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val incompleteTasks = db.taskDao().getAllOnce()
            .filter { !it.completed }
            .sortedBy { it.date }

        val todayStr = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
            .uppercase(Locale.ENGLISH)

        provideContent {
            WidgetContent(tasks = incompleteTasks, dateLabel = todayStr)
        }
    }

    @Composable
    private fun WidgetContent(tasks: List<TaskEntity>, dateLabel: String) {
        val nowInstant = Instant.now()
        val zone = ZoneId.systemDefault()

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            // Centered Header with Bottom Border
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = dateLabel,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
            }
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White),
            ) {}

            if (tasks.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No pending tasks",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF8E8E93)),
                            fontSize = 13.sp,
                        ),
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize(),
                ) {
                    items(tasks, itemId = { it.id.hashCode().toLong() }) { item ->
                        val parsedInstant = runCatching { Instant.parse(item.date) }.getOrNull()
                        val isOverdue = parsedInstant?.isBefore(nowInstant) == true
                        val monthDay = parsedInstant
                            ?.atZone(zone)
                            ?.toLocalDate()
                            ?.format(DateTimeFormatter.ofPattern("MM-dd", Locale.US))
                            ?: item.date.take(10).drop(5)
                        val displayTitle = "$monthDay - ${item.title}"
                        val dotColor = if (isOverdue) Color(0xFFFE8080) else Color(0xFF6B82F7)

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(12.dp)
                                .clickable(actionStartActivity<MainActivity>()),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(12.dp)
                                    .cornerRadius(6.dp)
                                    .background(dotColor),
                            ) {}
                            Spacer(modifier = GlanceModifier.width(10.dp))
                            Text(
                                text = displayTitle,
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

class RemainderrWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RemainderrWidget()
}
