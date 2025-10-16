package ttgt.schedule.ui.widget

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import ttgt.schedule.DayOfWeek
import ttgt.schedule.R
import ttgt.schedule.proto.Lesson
import ttgt.schedule.proto.Schedule
import ttgt.schedule.settingsDataStore
import ttgt.schedule.ui.TimestampType
import ttgt.schedule.ui.weekNum
import ttgt.schedule.ui.weekday

@Composable
fun stringResource(@StringRes id: Int) = LocalContext.current.getString(id)

class TimeRemainWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            var isFirstWeek by remember { mutableStateOf(weekNum()) }
            var currentWeekday by remember { mutableIntStateOf(weekday()) }
            val context = LocalContext.current
            var schedule: Schedule? by remember { mutableStateOf(null) }

            LaunchedEffect(Unit) {
                schedule = context.settingsDataStore.data.map { it.schedule }.firstOrNull()

                while (true) {
                    delay(1000)

                    isFirstWeek = weekNum()
                    currentWeekday = weekday()
                }
            }
            Box(
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.background)
                    .clickable {
                        context.startActivity(
                            context
                                .packageManager
                                .getLaunchIntentForPackage(context.packageName)
                        )
                    }
            ) {
                Column(GlanceModifier.padding(8.dp).fillMaxSize()) {
                    Row(GlanceModifier.fillMaxWidth()) {
                        Text(
                            stringResource(DayOfWeek.entries[currentWeekday].nameRes) +
                                    ", " +
                                    stringResource(
                                        if (isFirstWeek) R.string.first_week
                                        else R.string.second_week
                                    ).lowercase(),
                            style = TextStyle(fontWeight = FontWeight.Bold)
                        )
                    }

                    schedule?.let { schedule ->
                        LazyColumn {
                            val list = schedule
                                .weeksList[if (isFirstWeek) 0 else 1]
                                .daysList[currentWeekday]
                                .lessonList

                            val timestampType = when (currentWeekday) {
                                1 -> TimestampType.ClassHour
                                5 -> TimestampType.Saturday
                                else -> TimestampType.Normal
                            }

                            items(
                                list.size -
                                        if (timestampType == TimestampType.ClassHour) 1 else 0
                            ) { i ->
                                val index = if (timestampType == TimestampType.ClassHour && i > 3)
                                    i - 1 else i
                                val lesson = list[index]

                                Row {
                                    when {
                                        timestampType == TimestampType.ClassHour && i == 3 -> Text(
                                            stringResource(R.string.class_hour)
                                        )

                                        index < 5 || !list[index].hasNoLesson() -> {
                                            when (lesson.lessonCase) {
                                                Lesson.LessonCase.LESSON_NOT_SET,
                                                Lesson.LessonCase.NOLESSON ->
                                                    Text(stringResource(R.string.no_lesson))

                                                Lesson.LessonCase.COMMONLESSON ->
                                                    Text(lesson.commonLesson.name)

                                                Lesson.LessonCase.SUBGROUPEDLESSON ->
                                                    Text(lesson.subgroupedLesson.name)
                                            }
                                        }
                                    }

                                    Spacer(GlanceModifier.defaultWeight())

                                    Text(timestampType.timestamps[index].toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class TimeRemain : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimeRemainWidget()
}