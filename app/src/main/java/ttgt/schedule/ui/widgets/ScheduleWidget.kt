package ttgt.schedule.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.core.util.TypedValueCompat.dpToPx
import androidx.glance.Visibility
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import ttgt.schedule.DayOfWeek
import ttgt.schedule.R
import ttgt.schedule.getLessonData
import ttgt.schedule.isEmpty
import ttgt.schedule.name
import ttgt.schedule.api.profile
import ttgt.schedule.getSetting
import ttgt.schedule.proto.Lesson
import ttgt.schedule.proto.ProfileType
import ttgt.schedule.proto.Schedule
import ttgt.schedule.proto.WidgetSettings
import ttgt.schedule.settingsDataStore
import ttgt.schedule.ui.TimestampType
import ttgt.schedule.ui.weekNum
import ttgt.schedule.ui.weekday


private fun RemoteViews.addLessonView(
    lesson: Lesson,
    index: Int,
    timestampType: TimestampType,
    isTeacher: Boolean,
    context: Context
) {
    val view = RemoteViews(`package`, R.layout.lesson)

    view.setTextViewText(
        R.id.name,
        lesson.name
    )

    view.setTextViewText(
        R.id.time,
        timestampType.timestamps.getOrNull(index)?.toString() ?: ""
    )

    if (!lesson.isEmpty()) {
        val text = if (isTeacher) {
            lesson.group
        } else {
            lesson
                .commonLesson
                .room
                .ifBlank {
                    val sugroup = runBlocking {
                        context.getLessonData(lesson)
                    }?.subgroup?.takeIf { it > 0 }

                    if (sugroup != null) {
                        lesson
                            .subgroupedLesson
                            .subgroupsList[sugroup - 1]
                            .room
                    } else {
                        lesson
                            .subgroupedLesson
                            .subgroupsList
                            .joinToString(", ") {
                                it.room
                            }
                    }
                }
        }

        view.setTextViewText(R.id.group, text)
    }

    addView(
            R.id.lesson_list,
            view
        )
}

class ScheduleWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty())
            return
        var isSecondWeek = weekNum()
        var weekday = weekday()

        if (weekday > 4) {
            isSecondWeek = !isSecondWeek
            weekday = 0
        }

        val views = RemoteViews(context.packageName, R.layout.schedule_widget)
        var schedule: Schedule?

        val widgetSettings = runBlocking { context.getSetting { widgetsMap } }

        val lastUsed = runBlocking { context.getSetting { lastUsed } }

        val profiles = runBlocking { context.getSetting { profiles } }

        schedule = profiles?.profile(lastUsed)?.schedule

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
            println(schedule.weeksCount)

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
                        lastUsed == ProfileType.TEACHER,
                        context
                    )
                }
        }

        val defaultSettings = WidgetSettings.newBuilder()
            .setBackground(1F)
            .setInnerPadding(8)

        appWidgetIds.forEach { appWidgetId ->
            val viewsInner = views
            val settings2 = widgetSettings?.takeIf {
                it.containsKey(appWidgetId)
            }?.getValue(appWidgetId)
            val settings = settings2 ?: defaultSettings

            val a = TypedValue()

            context.theme.resolveAttribute(android.R.attr.colorBackground, a, true)
            val background = ((settings.getBackground() * 0xFF).toInt() shl 24) or (a.data and 0x00FFFFFF)
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

//class TimeRemainWidgetGlance : GlanceAppWidget() {
//    override suspend fun provideGlance(context: Context, id: GlanceId) {
//        provideContent {
//            var isSecondWeek by remember { mutableStateOf(false) }
//            var currentWeekday by remember { mutableIntStateOf(0) }
//            var schedule: Schedule? by remember { mutableStateOf(null) }
//
//            runBlocking {
//                schedule = context.settingsDataStore.data.map { it.schedule }.firstOrNull()
//            }
//
//            LaunchedEffect(Unit) {
//                while (true) {
//                    var isSec = weekNum()
//                    var weekday = weekday()
//
//                    if (weekday > 4) {
//                        isSec = !isSec
//                        weekday = 0
//                    }
//
//                    if (isSec != isSecondWeek)
//                        isSecondWeek = isSec
//
//                    if (weekday != currentWeekday)
//                        currentWeekday = weekday
//
//                    delay(1000)
//                }
//            }
////            Box(
////                GlanceModifier
////                    .fillMaxSize()
////                    .background(GlanceTheme.colors.background)
////                    .clickable {
////                        context.startActivity(
////                            context
////                                .packageManager
////                                .getLaunchIntentForPackage(context.packageName)
////                        )
////                    }
////            ) {
////                Column(GlanceModifier.padding(8.dp).fillMaxSize()) {
////                    Row(GlanceModifier.fillMaxWidth()) {
////                        Text(
////                            stringResource(DayOfWeek.entries[currentWeekday].nameRes) +
////                                    ", " +
////                                    stringResource(
////                                        if (!isSecondWeek) R.string.first_week
////                                        else R.string.second_week
////                                    ).lowercase(),
////                            style = TextStyle(fontWeight = FontWeight.Bold)
////                        )
////                    }
////                }
////            }
//
//            schedule?.let { schedule ->
//                        LazyColumn {
//                            val list = schedule
//                                .weeksList[if (isSecondWeek) 1 else 0]
//                                .daysList[currentWeekday]
//                                .lessonList
//
//                            val timestampType = TimestampType.fromWeekday(currentWeekday)
//
//                            items(
//                                list.size -
//                                        if (timestampType == TimestampType.ClassHour) 1 else 0
//                            ) { i ->
//                                val index = if (timestampType == TimestampType.ClassHour && i > 3)
//                                    i - 1 else i
//                                val lesson = list[index]
//
//                                Row {
//                                    Text(
//                                        when {
//                                            timestampType == TimestampType.ClassHour && i == 3 -> stringResource(R.string.class_hour)
//
//                                            index < 5 || !list[index].hasNoLesson() -> {
//                                                when (lesson.lessonCase) {
//                                                    Lesson.LessonCase.LESSON_NOT_SET,
//                                                    Lesson.LessonCase.NOLESSON ->
//                                                        stringResource(R.string.no_lesson)
//
//                                                    Lesson.LessonCase.COMMONLESSON ->
//                                                        lesson.commonLesson.name
//
//                                                    Lesson.LessonCase.SUBGROUPEDLESSON ->
//                                                        lesson.subgroupedLesson.name
//                                                }
//                                            }
//
//                                            else -> ""
//                                        },
//                                        GlanceModifier, //.fillMaxWidth(),
//                                        TextStyle(
//                                            fontWeight = FontWeight.Bold
//                                        )
//                                    )
//
//                                    if (5 > index) {
//                                        Text(
//                                            timestampType.timestamps[index].toString(),
//                                            GlanceModifier.fillMaxWidth().defaultWeight(),
//                                            TextStyle(
//                                                textAlign = TextAlign.End
//                                            )
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//        }
//    }
//}