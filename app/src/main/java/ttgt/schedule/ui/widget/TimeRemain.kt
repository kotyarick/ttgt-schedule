package ttgt.schedule.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.TypedValueCompat.dpToPx
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import ttgt.schedule.DayOfWeek
import ttgt.schedule.R
import ttgt.schedule.proto.Lesson
import ttgt.schedule.proto.Schedule
import ttgt.schedule.proto.WidgetSettings
import ttgt.schedule.settingsDataStore
import ttgt.schedule.ui.TimestampType
import ttgt.schedule.ui.weekNum
import ttgt.schedule.ui.weekday


@Composable
fun stringResource(@StringRes id: Int) = LocalContext.current.getString(id)

fun RemoteViews.addLessonView(
    lesson: Lesson,
    index: Int,
    timestampType: TimestampType,
    isTeacher: Boolean
) {
    val view = RemoteViews(`package`, R.layout.lesson)

    view.setTextViewText(
        R.id.name,
        lesson.commonLesson.name.ifBlank {
            lesson.subgroupedLesson.name.ifBlank {
                "Нет пары"
            }
        }
    )

    view.setTextViewText(
        R.id.time,
        timestampType.timestamps.getOrNull(index)?.toString() ?: ""
    )

    if (isTeacher) {
        view.setTextViewText(
            R.id.group,
            lesson.group
        )
    }

    addView(
        R.id.lesson_list,
                view
        )
}

class TimeRemainWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        var isSecondWeek = weekNum()
        var weekday = weekday()

        if (weekday > 4) {
            isSecondWeek = !isSecondWeek
            weekday = 0
        }

        val views = RemoteViews(context.packageName, R.layout.widget)
        var schedule: Schedule?

        val widgetSettings = runBlocking {
            context.settingsDataStore.data.map {
                it.widgetsMap
            }.firstOrNull()
        }

        val isTeacher = runBlocking {
            context.settingsDataStore.data.map {
                it.teacherName
            }.firstOrNull()?.isNotBlank() == true
        }

        widgetSettings?.keys?.forEach {
            println("$it ${widgetSettings.getValue(it)}")
        }

        runBlocking {
            schedule = context.settingsDataStore.data.map {
                it.schedule
            }.firstOrNull()
        }

        views.setTextViewText(
            R.id.today,
            context.getString(DayOfWeek.entries[weekday].nameRes) +
                    ", " +
                    context.getString(
                        if (!isSecondWeek) R.string.first_week
                        else R.string.second_week
                    ).lowercase()
        )

        views.removeAllViews(R.id.lesson_list)

        schedule?.let { schedule ->
            schedule
                .weeksList[if (isSecondWeek) 1 else 0]
                .daysList[weekday]
                .lessonList
                .forEachIndexed { index, lesson ->
                    if (index > 4) {
                        return@forEachIndexed
                    }
                    views.addLessonView(
                        lesson,
                        index,
                        TimestampType.fromWeekday(weekday),
                        isTeacher
                    )
                }
        }

        val defaultSettings = WidgetSettings.newBuilder()
            .setBackground(1F)
            .setInnerPadding(8)

        appWidgetIds.forEach { appWidgetId ->
            val viewsInner = views
            val settings_a = widgetSettings?.takeIf {
                println("$appWidgetId ${it.keys}")

                it.containsKey(appWidgetId)
            }?.getValue(appWidgetId)
            println(settings_a)
            val settings = settings_a ?: defaultSettings

            val a = TypedValue()

            context.theme.resolveAttribute(android.R.attr.colorBackground, a, true)
            val background = ((settings.getBackground() * 0xFF).toInt() shl 24) or (a.data and 0x00FFFFFF)
            println("${a.data.toHexString()} ${background.toHexString()}")
            views.setInt(
                R.id.root,
                "setBackgroundColor",
                background
            )

            val padding = dpToPx(
                settings.innerPadding.toFloat(),
                context.resources.displayMetrics
            ).toInt()

            views.setViewPadding(
                R.id.root,
                padding, padding, padding, padding
            )

            appWidgetManager.updateAppWidget(appWidgetId, viewsInner)
        }
    }
}

class TimeRemainWidgetGlance : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            var isSecondWeek by remember { mutableStateOf(false) }
            var currentWeekday by remember { mutableIntStateOf(0) }
            var schedule: Schedule? by remember { mutableStateOf(null) }

            runBlocking {
                schedule = context.settingsDataStore.data.map { it.schedule }.firstOrNull()
            }

            LaunchedEffect(Unit) {
                while (true) {
                    var isSec = weekNum()
                    var weekday = weekday()

                    if (weekday > 4) {
                        isSec = !isSec
                        weekday = 0
                    }

                    if (isSec != isSecondWeek)
                        isSecondWeek = isSec

                    if (weekday != currentWeekday)
                        currentWeekday = weekday

                    delay(1000)
                }
            }
//            Box(
//                GlanceModifier
//                    .fillMaxSize()
//                    .background(GlanceTheme.colors.background)
//                    .clickable {
//                        context.startActivity(
//                            context
//                                .packageManager
//                                .getLaunchIntentForPackage(context.packageName)
//                        )
//                    }
//            ) {
//                Column(GlanceModifier.padding(8.dp).fillMaxSize()) {
//                    Row(GlanceModifier.fillMaxWidth()) {
//                        Text(
//                            stringResource(DayOfWeek.entries[currentWeekday].nameRes) +
//                                    ", " +
//                                    stringResource(
//                                        if (!isSecondWeek) R.string.first_week
//                                        else R.string.second_week
//                                    ).lowercase(),
//                            style = TextStyle(fontWeight = FontWeight.Bold)
//                        )
//                    }
//                }
//            }

            schedule?.let { schedule ->
                        LazyColumn {
                            val list = schedule
                                .weeksList[if (isSecondWeek) 1 else 0]
                                .daysList[currentWeekday]
                                .lessonList

                            val timestampType = TimestampType.fromWeekday(currentWeekday)

                            items(
                                list.size -
                                        if (timestampType == TimestampType.ClassHour) 1 else 0
                            ) { i ->
                                val index = if (timestampType == TimestampType.ClassHour && i > 3)
                                    i - 1 else i
                                val lesson = list[index]

                                Row {
                                    Text(
                                        when {
                                            timestampType == TimestampType.ClassHour && i == 3 -> stringResource(R.string.class_hour)

                                            index < 5 || !list[index].hasNoLesson() -> {
                                                when (lesson.lessonCase) {
                                                    Lesson.LessonCase.LESSON_NOT_SET,
                                                    Lesson.LessonCase.NOLESSON ->
                                                        stringResource(R.string.no_lesson)

                                                    Lesson.LessonCase.COMMONLESSON ->
                                                        lesson.commonLesson.name

                                                    Lesson.LessonCase.SUBGROUPEDLESSON ->
                                                        lesson.subgroupedLesson.name
                                                }
                                            }

                                            else -> ""
                                        },
                                        GlanceModifier, //.fillMaxWidth(),
                                        TextStyle(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )

                                    if (5 > index) {
                                        Text(
                                            timestampType.timestamps[index].toString(),
                                            GlanceModifier.fillMaxWidth().defaultWeight(),
                                            TextStyle(
                                                textAlign = TextAlign.End
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
        }
    }
}