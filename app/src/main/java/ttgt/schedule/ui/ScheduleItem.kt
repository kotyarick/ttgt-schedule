package ttgt.schedule.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ttgt.schedule.R
import ttgt.schedule.getLessonData
import ttgt.schedule.isEmpty
import ttgt.schedule.proto.CustomLesson
import ttgt.schedule.proto.Lesson
import ttgt.schedule.vector
import java.lang.System.currentTimeMillis
import java.util.Date

fun Int.pad() = if (this < 10) "0$this" else this.toString()

data class Time(
    val hours: Int,
    val minutes: Int
) : Comparable<Time> {
    operator fun compareTo(time: Long): Int =
        (hours * 60 + minutes) - Date(time).let { date ->
            date.hours * 60 + date.minutes
        }

    override fun toString() = hours.pad() + ":" + minutes.pad()

    fun proto() = ttgt.schedule.proto.Time.newBuilder().setHours(hours).setMinutes(minutes).build()

    override fun compareTo(other: Time): Int =
        (hours * 60 + minutes) - (other.hours * 60 + other.minutes)

    companion object {
        fun from(proto: ttgt.schedule.proto.Time) = Time(proto.hours, proto.minutes)
    }
}

data class LessonTime(
    val start: Time,
    val end: Time
) {
    fun isNow() = currentTimeMillis().let { time ->
        start <= time && end >= time
    }

    override fun toString() = "$start - $end"
    //fun toLinedString() = "$start\n$end"
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

@Composable fun CustomLessonItem(
    lesson: CustomLesson,
    index: Int,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val time = LessonTime(
        Time.from(lesson.startTime),
        Time.from(lesson.endTime)
    )

    var isCurrent by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        if (isToday) {
            while (true) {
                isCurrent = time.isNow()

                delay(1000)
            }
        }
    }

    var twoLined: Boolean? by remember { mutableStateOf(null) }

    Card(
        onClick,
        Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape),
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

                Column(Modifier.width(200.dp)) {
                    Text(lesson.name, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.weight(1F))

                Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End) {
                    Text(
                        time.toString(),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.End,
                        onTextLayout = { result ->
                            if (twoLined == null) {
                                twoLined = result.lineCount >= 2
                            }
                        }
                    )

                    Column(
                        Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Spacer(Modifier.fillMaxHeight())

                        androidx.compose.material3.Icon(
                            R.drawable.edit.vector,
                            null,
                            Modifier
                                .size(20.dp)
                                .alpha(0.7F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleItem(
    lesson: Lesson,
    index: Int,
    isToday: Boolean,
    timestampType: TimestampType,
    isTeacher: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val timestampIndex =
        if (timestampType == TimestampType.ClassHour && index > 2) index + 1 else index

    var isCurrent by remember {
        mutableStateOf(false)
    }

    var subgroup by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(Unit) {
        if (lesson.hasSubgroupedLesson()) {
            subgroup = context.getLessonData(lesson)?.subgroup ?: 0
        }

        if (isToday) {
            while (true) {
                isCurrent = timestampType.timestamps.getOrNull(
                    if (timestampType == TimestampType.ClassHour && index > 2) index + 1 else index
                )?.isNow() == true

                delay(1000)
            }
        }
    }

    @Composable
    fun lessonName(name: String) = Text(name, fontWeight = FontWeight.Bold)

    @Composable
    fun lessonSubtext(
        teacher: String,
        room: String
    ) = Column {
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

    var twoLined: Boolean? by remember { mutableStateOf(null) }

    Card(
        Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .alpha(if (lesson.isEmpty()) 0.5F else 1F).let {
                if (lesson.isEmpty()) it
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

                when(lesson.lessonCase) {
                    Lesson.LessonCase.COMMONLESSON -> {
                        lessonText(
                            lesson.commonLesson.name,
                            if (isTeacher) lesson.group else lesson.commonLesson.teacher,
                            lesson.commonLesson.room
                        )
                    }

                    Lesson.LessonCase.SUBGROUPEDLESSON -> {
                        Column(Modifier.width(200.dp)) {
                            lessonName(lesson.subgroupedLesson.name)

                            lesson.subgroupedLesson.subgroupsList.forEachIndexed { index, data ->
                                Row {
                                    Text(
                                        "${data.subgroupIndex.takeIf { it > 0 } ?: (index + 1)} п/г",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = if (subgroup == index+1) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )

                                    Spacer(Modifier.width(10.dp))

                                    lessonSubtext(data.teacher, data.room)
                                }
                            }
                        }
                    }

                    Lesson.LessonCase.NOLESSON,
                    Lesson.LessonCase.LESSON_NOT_SET -> {
                        Text(
                            stringResource(R.string.no_lesson),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(Modifier.weight(1F))

                Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End) {
                    timestampType.timestamps.getOrNull(timestampIndex)?.let { time ->
                        Text(
                            time.toString(),
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.End,
                            onTextLayout = { result ->
                                if (twoLined == null) {
                                    twoLined = result.lineCount >= 2
                                }
                            }
                        )
                    }

                    if (
                        lesson.lessonCase !in listOf(
                            Lesson.LessonCase.NOLESSON,
                            Lesson.LessonCase.LESSON_NOT_SET
                        )
                    ) {
                        Column(
                            Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Spacer(Modifier.fillMaxHeight())

                            androidx.compose.material3.Icon(
                                R.drawable.edit.vector,
                                null,
                                Modifier
                                    .size(20.dp)
                                    .alpha(0.7F)
                            )
                        }

                    }
                }
            }
        }
    }
}