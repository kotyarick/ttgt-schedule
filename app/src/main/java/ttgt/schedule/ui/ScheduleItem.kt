package ttgt.schedule.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ttgt.schedule.Icon
import ttgt.schedule.R
import ttgt.schedule.proto.Lesson
import ttgt.schedule.vector
import java.time.Instant
import java.util.Date

fun Int.pad() = if (this < 10) "0$this" else this.toString()

data class Time(
    val hours: Int,
    val minutes: Int
) {
    operator fun compareTo(time: Long): Int =
        (hours * 60 + minutes) - Date(time).let { date ->
            date.hours * 60 + date.minutes
        }

    override fun toString() = hours.pad() + ":" + minutes.pad()
}

data class LessonTime(
    val start: Time,
    val end: Time
) {
    fun isNow() = Instant.now().toEpochMilli().let { time ->
        start < time && end > time
    }

    override fun toString() = "$start - $end"
    fun toLinedString() = "$start\n$end"
}

@Composable
fun ClassHour(isCurrent: Boolean) {
    Card(
        Modifier
            .fillMaxWidth(),
        colors = if (isCurrent) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Row(
            Modifier
                .padding(15.dp)
        ) {
            Text(
                "1.",
                color = Color.Transparent
            )
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.class_hour), fontWeight = FontWeight.Bold)

            Spacer(Modifier.weight(1F))

            TimestampType.ClassHour.timestamps.getOrNull(3)?.let { time ->
                Text(
                    time.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun ScheduleItem(
    lesson: Lesson,
    subgroup: Int?,
    index: Int,
    isCurrent: Boolean,
    timestampType: TimestampType,
    isTeacher: Boolean,
    onClick: () -> Unit
) {
    val timestampIndex =
        if (timestampType == TimestampType.ClassHour && index > 2) index + 1 else index

    @Composable
    fun lessonName(name: String) = Text(name, fontWeight = FontWeight.Bold)

    @Composable
    fun lessonSubtext(teacher: String, room: String) = Column {
        Text(
            "$teacher\nКабинет $room",
            color = MaterialTheme.colorScheme.secondary
        )
    }


    @Composable
    fun lessonText(name: String, teacher: String, room: String) = Column(Modifier.width(200.dp)) {
        lessonName(name)
        lessonSubtext(teacher, room)
    }


    Card(
        Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .alpha(if (lesson.hasNoLesson()) 0.5F else 1F).let {
                if (lesson.hasNoLesson()) it
                else it.clickable(onClick = onClick)
            },
        colors = if (isCurrent) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Box {
            Row(
                Modifier
                    .padding(15.dp)
            ) {
                Text(
                    "${index + 1}.",
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(10.dp))

                when {
                    lesson.hasCommonLesson() -> {
                        lessonText(
                            lesson.commonLesson.name,
                            if (isTeacher) lesson.group else lesson.commonLesson.teacher,
                            lesson.commonLesson.room
                        )
                    }

                    lesson.hasSubgroupedLesson() -> {
                        Column(Modifier.width(200.dp)) {
                            lessonName(lesson.subgroupedLesson.name)
                            if (subgroup == null) {
                                lesson.subgroupedLesson.subgroupsList.forEachIndexed { index, data ->
                                    Row {
                                        Text(
                                            "${data.subgroupIndex.takeIf { it > 0 } ?: (index + 1)} п/г",
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        lessonSubtext(data.teacher, data.room)
                                    }
                                }
                            } else {
                                Row {
                                    Text(
                                        "${subgroup + 1} п/г",
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    lessonSubtext(
                                        lesson.subgroupedLesson.subgroupsList[subgroup].teacher,
                                        lesson.subgroupedLesson.subgroupsList[subgroup].room
                                    )
                                }
                            }
                        }
                    }

                    lesson.hasNoLesson() -> {
                        Text(
                            stringResource(R.string.no_lesson),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(Modifier.weight(1F))

                Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End) {
                    timestampType.timestamps.getOrNull(timestampIndex)?.let { time ->
                        var twoLined: Boolean? by remember { mutableStateOf(null) }
                        Text(
                            if (twoLined == true) {
                                time.toLinedString()
                            } else {
                                time.toString()
                            },
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.End,
                            onTextLayout = { result ->
                                if (twoLined == null) {
                                    twoLined = result.lineCount >= 2
                                }
                            }
                        )
                    }

                    if (!lesson.hasNoLesson()) {
                        Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                            Card(
                                Modifier.border(
                                    width = 1.dp,
                                    shape = CardDefaults.shape,
                                    color = MaterialTheme
                                        .colorScheme
                                        .onBackground
                                        .copy(alpha = 0.2F)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = CardDefaults
                                        .cardColors()
                                        .containerColor
                                        .copy(alpha = 0.3F)
                                )
                            ) {
                                Row(
                                    Modifier.padding(6.dp),
                                    horizontalArrangement = Arrangement
                                        .spacedBy(
                                            8.dp,
                                            Alignment.CenterHorizontally
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    //Text(stringResource(R.string.notes_short))
                                    Icon(R.drawable.edit)
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}