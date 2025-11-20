package ttgt.schedule.ui.sheets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch
import ttgt.schedule.DayOfWeek
import ttgt.schedule.Icon
import ttgt.schedule.R
import ttgt.schedule.getSetting
import ttgt.schedule.proto.CustomLesson
import ttgt.schedule.settingsDataStore
import ttgt.schedule.shouldShow
import ttgt.schedule.ui.LessonTime
import ttgt.schedule.ui.Time
import ttgt.schedule.vector

fun Modifier.forceClickable(
    onClick: () -> Unit
) = this.then(
    Modifier.pointerInput(PointerEventPass.Initial) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            down.consume()
            val up = waitForUpOrCancellation(PointerEventPass.Initial)
            if (up != null) {
                onClick()
            }
        }
    }
)

enum class TimeType {
    Start, End
}

@OptIn(ExperimentalMaterial3Api::class)
fun TimePickerState.time() = Time(hour, minute)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonAdd(
    state: SheetState,
    defaultWeekday: Int,
    defaultWeeknum: Boolean,
    defaultTime: LessonTime? = null,
    defaultName: String,
    editIndex: Int,
    onAdd: (CustomLesson?) -> Unit
) {
    if (!state.shouldShow) return

    val time = defaultTime ?: LessonTime(
        Time(16, 50),
        Time(17, 20)
    )

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var weekday by remember { mutableIntStateOf(defaultWeekday) }
    var weeknum by remember { mutableStateOf(defaultWeeknum) }
    var name by remember { mutableStateOf(defaultName) }

    val startTime = rememberTimePickerState(
        time.start.hours,
        time.start.minutes
    )
    val endTime = rememberTimePickerState(
        time.end.hours,
        time.end.minutes
    )
    var showTimePickerFor: TimeType? by remember { mutableStateOf(null) }

    var weekdayExpanded by remember { mutableStateOf(false) }
    var weekdayInputSize by remember { mutableStateOf(Size.Zero)}
    val weekdayIconRotation = animateFloatAsState(if (weekdayExpanded) 180F else 0F)

    if (showTimePickerFor != null) {
        TimePickerDialog(
            { showTimePickerFor = null },
            {
                TextButton({
                    showTimePickerFor = null
                }) { Text(stringResource(R.string.ok)) }
            },
            {
                Column {
                    Text(
                        stringResource(
                            when (showTimePickerFor) {
                                TimeType.Start -> R.string.lesson_start_time
                                TimeType.End -> R.string.lesson_end_time
                                else -> R.string.time
                            }
                        ),
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        ) {
            TimePicker(
                when (showTimePickerFor) {
                    TimeType.Start -> startTime
                    TimeType.End -> endTime
                    else -> return@TimePickerDialog
                }
            )
        }
    }


    ModalBottomSheet(
        { scope.launch { state.hide() } },
        sheetState = state
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                name, { name = it },
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        weekdayInputSize = coordinates.size.toSize()
                    },
                label = { Text(stringResource(R.string.lesson_name)) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedTextField(
                    startTime.time().toString(), {},
                    Modifier
                        .weight(1F)
                        .forceClickable {
                            showTimePickerFor = TimeType.Start
                        },
                    label = {
                        Text(stringResource(R.string.lesson_start_time))
                    },
                    readOnly = true
                )

                OutlinedTextField(
                    endTime.time().toString(), {},
                    Modifier
                        .weight(1F)
                        .forceClickable {
                            showTimePickerFor = TimeType.End
                        },
                    label = {
                        Text(stringResource(R.string.lesson_end_time))
                    },
                    readOnly = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedTextField(
                    stringResource(DayOfWeek.entries[weekday].nameRes),
                    {},
                    Modifier
                        .weight(1F)
                        .forceClickable { weekdayExpanded = true },
                    trailingIcon = {
                        Icon(
                            R.drawable.expand.vector,
                            null,
                            Modifier.rotate(weekdayIconRotation.value)
                        )
                    },
                    label = {
                        Text(stringResource(R.string.weekday))

                        DropdownMenu(
                            weekdayExpanded,
                            { weekdayExpanded = false },
                            Modifier.width(
                                with(LocalDensity.current) {
                                    weekdayInputSize.width.toDp()
                                }
                            )
                        ) {
                            repeat(5) { index ->
                                DropdownMenuItem(
                                    { Text(stringResource(DayOfWeek.entries[index].nameRes)) },
                                    {
                                        weekdayExpanded = false
                                        weekday = index
                                    }
                                )
                            }
                        }
                    },
                    readOnly = true
                )


                OutlinedTextField(
                    stringResource(
                        if (weeknum) {
                            R.string.second_week
                        } else {
                            R.string.first_week
                        }
                    ), {},
                    Modifier
                        .weight(1F)
                        .forceClickable { weeknum = !weeknum },
                    label = {
                        Text(stringResource(R.string.weeknum))
                    },
                    readOnly = true
                )
            }
            Row {
                Button(
                    {
                        scope.launch {
                            val build = {
                                CustomLesson.newBuilder()
                                    .setName(name)
                                    .setStartTime(startTime.time().proto())
                                    .setEndTime(endTime.time().proto())
                                    .setWeekday(weekday)
                                    .setWeeknum(weeknum)
                                    .build()
                            }

                            val customLesson = if (editIndex == -1) {
                                build()
                            } else {
                                context.getSetting { customLessonsList }
                                    ?.getOrNull(editIndex)
                                    ?: build()
                            }

                            context.settingsDataStore.updateData {
                                it.toBuilder()
                                    .addCustomLessons(
                                        customLesson
                                    )
                                    .build()
                            }
                            onAdd(customLesson)
                            state.hide()
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Text(stringResource(
                        when {
                            name.isBlank() -> R.string.name_is_empty
                            editIndex == -1 -> R.string.add_lesson
                            else -> R.string.edit_lesson
                        }
                    ))
                }

                if (editIndex != -1) {
                    IconButton({
                        scope.launch {
                            context.settingsDataStore.updateData {
                                it.toBuilder()
                                    .removeCustomLessons(editIndex)
                                    .build()
                            }
                            onAdd(null)
                            state.hide()
                        }
                    }) {
                        Icon(R.drawable.delete)
                    }
                }
            }
        }
    }
}