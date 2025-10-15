package ttgt.schedule.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ttgt.schedule.DayOfWeek
import ttgt.schedule.R
import ttgt.schedule.proto.Application
import ttgt.schedule.proto.GroupId
import ttgt.schedule.proto.LatestVersion
import ttgt.schedule.proto.Lesson
import ttgt.schedule.proto.LessonUserData
import ttgt.schedule.proto.Overrides
import ttgt.schedule.proto.Schedule
import ttgt.schedule.settingsDataStore
import ttgt.schedule.stub
import ttgt.schedule.ui.theme.ScheduleTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import kotlin.time.ExperimentalTime


const val TAB_AMOUNT = 5
val startOfYear: LocalDate = LocalDate.of(2025, 1, 1)

fun weekNum() = weekNum(LocalDate.now())

fun weekNum(inputDate: LocalDate): Boolean {
    val date = inputDate.minusDays(1)
    var out = true

    var currentDate = startOfYear
    while (currentDate.isBefore(date) || currentDate.isEqual(date)) {
        if (currentDate.dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            out = !out
        }
        currentDate = currentDate.plusDays(1)
    }

    return out
}

fun weekday() = (
        Calendar
            .getInstance()
            .get(Calendar.DAY_OF_WEEK) - 1
        ).let { if (it == 0) 6 else it - 1 }

enum class LoadingState {
    PreLoading, Loading, Error, Done
}

fun Long.timestamp(): LocalDate = Instant
    .ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()!!

@OptIn(ExperimentalMaterial3Api::class)
object NoWeekendsSelectableDates : SelectableDates {
    @OptIn(ExperimentalTime::class)
    override fun isSelectableDate(utcTimeMillis: Long) =
        utcTimeMillis
            .timestamp()
            .dayOfWeek !in listOf(
            java.time.DayOfWeek.SUNDAY,
            java.time.DayOfWeek.SATURDAY
        )

    override fun isSelectableYear(year: Int) = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleUi(goToWelcome: () -> Unit) = ScheduleTheme {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val pagerState = rememberPagerState(
        weekday().let { weekday ->
            if (weekday >= TAB_AMOUNT) 0
            else weekday
        }
    ) { TAB_AMOUNT }
    var isCurrentWeekEven by remember { mutableStateOf(weekNum()) }  // Чётная?
    var isSelectedWeekEven by remember {
        mutableStateOf(
            if (weekday() >= TAB_AMOUNT) !isCurrentWeekEven else isCurrentWeekEven
        )
    }  // Чётная?
    var schedule: Schedule? by remember { mutableStateOf(null) }
    var overrides: Overrides? by remember { mutableStateOf(null) }
    var applyOverrides by remember { mutableStateOf(true) }
    var overridesChecking by remember { mutableStateOf(false) }
    var updateItems by remember { mutableStateOf(false) }
    val about = rememberModalBottomSheetState()

    val now = LocalDate.now()

    val datePicker = rememberDatePickerState(
        yearRange = now.year..now.year + 2,
        selectableDates = NoWeekendsSelectableDates
    )
    var showDatePicker by remember { mutableStateOf(false) }

    var versionInfo: LatestVersion? by remember { mutableStateOf(null) }
    var versionLoadingState by remember { mutableStateOf(LoadingState.PreLoading) }

    val packageInfo = remember {
        context
            .packageManager
            .getPackageInfo(context.packageName, 0)
    }

    val versionName = remember {
        packageInfo.versionName ?: "0"
    }

    val versionCode = remember {
        packageInfo.versionCode
    }

    val snackbarHostState = remember { SnackbarHostState() }

    var isTeacher by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var a = false
        scope.launch {
            a = context.settingsDataStore.data.map {
                it.hasTeacherName()
            }.firstOrNull() ?: false
        }.invokeOnCompletion {
            isTeacher = a
        }
    }

    var currentLesson: Lesson? by remember { mutableStateOf(null) }
    val lessonInfo = rememberModalBottomSheetState()

    val openLink = stringResource(R.string.open_overrides)
    val overridesRetrieveError = stringResource(R.string.overrides_retrieve_error)
    val noOverrides = stringResource(R.string.no_overrides)
    val overridesDetected = stringResource(R.string.overrides_detected)

    fun snackbar(text: String) = scope.launch {
        val result = snackbarHostState
            .showSnackbar(
                message = text,
                actionLabel = openLink,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
        when (result) {
            SnackbarResult.ActionPerformed -> {
                uriHandler.openUri("https://ttgt.org/images/pdf/zamena.pdf")
            }

            SnackbarResult.Dismissed -> {
                /* Handle snackbar dismissed */
            }
        }
    }

    fun checkUpdate() = scope.launch(Dispatchers.IO) {
        runCatching {
            stub.getLatestVersion(
                Application.newBuilder()
                    .setPlatform(Application.Platform.Android)
                    .build()
            )
        }.apply {
            exceptionOrNull()?.printStackTrace()
            runOnUiThread {
                if (isSuccess) {
                    versionInfo = getOrNull()
                    versionLoadingState = LoadingState.Done

                    if ((versionInfo?.versionCode ?: -1) != versionCode) {
                        scope.launch {
                            about.show()
                        }
                    }
                } else {
                    versionLoadingState = LoadingState.Error
                }
            }

        }
    }

    LaunchedEffect(versionLoadingState) {
        if (versionLoadingState != LoadingState.PreLoading) return@LaunchedEffect

        versionLoadingState = LoadingState.Loading

        checkUpdate()
    }

    if (about.isVisible || about.isAnimationRunning) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { about.hide() }
            },
            sheetState = about
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    stringResource(R.string.app_name),
                    Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )

                Text(
                    stringResource(
                        R.string.app_version,
                        context
                            .packageManager
                            .getPackageInfo(context.packageName, 0)
                            .versionName ?: "0"
                    ),
                    Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Text(
                    stringResource(
                        R.string.changelog,
                        versionName
                    ),
                    Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelLarge
                )

                if (versionInfo != null) {
                    Text(versionInfo?.changelog ?: "")

                    if (versionCode == versionInfo?.versionCode) {
                        ListItem(
                            {
                                Text(stringResource(R.string.current_version_latest))
                            },
                            leadingContent = {
                                Icon(Icons.Default.Done, null)
                            },
                        )
                    } else {
                        Button({
                            scope.launch {
                                uriHandler.openUri("http://185.13.47.146:50145/android.apk")
                            }
                        }, Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(
                                    R.string.download_update,
                                    versionInfo?.versionCode ?: 0
                                )
                            )
                        }
                    }
                }

                when (versionLoadingState) {
                    LoadingState.Loading -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    LoadingState.Error -> ListItem(
                        {
                            Text(stringResource(R.string.error))
                        },
                        trailingContent = {
                            Icon(Icons.Default.Refresh, null)
                        },
                        modifier = Modifier.clickable {
                            versionLoadingState = LoadingState.PreLoading
                        }
                    )

                    else -> {}
                }
            }
        }
    }

    fun checkOverrides(onDone: (Boolean) -> Unit) {
        if (overridesChecking) return
        overridesChecking = true

        scope.launch(Dispatchers.IO) {
            runCatching {
                stub.getOverrides(
                    GroupId.newBuilder()
                        .setId(context.settingsDataStore.data.map { it.groupName }.firstOrNull())
                        .build()
                )
            }.apply {
                exceptionOrNull()?.printStackTrace()

                runOnUiThread {
                    overrides = getOrNull()
                    overridesChecking = false
                    onDone(isFailure)
                }

                overrides?.let { overrides ->
                    context.settingsDataStore.updateData {
                        it.toBuilder().setOverrides(overrides).build()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        schedule = context.settingsDataStore.data.map { it.schedule }.firstOrNull()
        overrides = context.settingsDataStore.data.map { it.overrides }.firstOrNull()

        if (overrides?.takeIf { it.overridesCount > 0 } == null) {
            checkOverrides { }
        }

        while (true) {
            updateItems = !updateItems
            delay(1000)
        }
    }

    if (showDatePicker) {
        val selectedDateMillis = datePicker.selectedDateMillis
        val displayedMonthMillis = datePicker.displayedMonthMillis

        fun dismiss() {
            datePicker.selectedDateMillis = selectedDateMillis
            datePicker.displayedMonthMillis = displayedMonthMillis

            showDatePicker = false
        }

        DatePickerDialog(
            onDismissRequest = { dismiss() },
            confirmButton = {
                if (datePicker.selectedDateMillis != null) {
                    TextButton({
                        showDatePicker = false

                        datePicker.selectedDateMillis?.timestamp()?.let { date ->
                            scope.launch {
                                isSelectedWeekEven = weekNum(date)
                                pagerState.animateScrollToPage(DayOfWeek.from(date.dayOfWeek).ordinal)
                            }
                        }
                    }) {
                        Text(stringResource(R.string.jump_to_date_confirm))
                    }
                }
            },
            dismissButton = {
                TextButton({ dismiss() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            content = {
                DatePicker(
                    datePicker,
                    title = { }
                )
            }
        )
    }

    if (lessonInfo.isVisible || lessonInfo.isAnimationRunning) {
        currentLesson?.let { lesson ->
            val name = lesson.commonLesson.name.ifBlank { lesson.subgroupedLesson.name }
            var notes by remember { mutableStateOf("") }

            ModalBottomSheet({
                scope.launch {
                    context.settingsDataStore.updateData {
                        it.toBuilder()
                            .apply {
                                val key = "$name ${lesson.group}"

                                if (lessonDataMap.containsKey(key)) {
                                    lessonDataMap.put(
                                        key,
                                        lessonDataMap
                                            .getValue(key)
                                            .toBuilder()
                                            .setNotes(notes)
                                            .build()
                                    )
                                } else {
                                    putLessonData(
                                        key,
                                        LessonUserData
                                            .newBuilder()
                                            .setNotes(notes)
                                            .build()
                                    )
                                }
                            }
                            .build()
                    }

                    lessonInfo.hide()
                }.invokeOnCompletion {
                    currentLesson = null
                }
            }) {
                LaunchedEffect(Unit) {
                    notes = context.settingsDataStore.data.map {
                        it.lessonDataMap
                    }.firstOrNull()?.get(name)?.notes ?: ""
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        name,
                        Modifier.padding(bottom = 16.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    OutlinedTextField(
                        notes, { notes = it },
                        Modifier
                            .fillMaxSize(),
                        label = {
                            Text(
                                stringResource(R.string.notes)
                            )
                        }
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            Row(Modifier.statusBarsPadding()) {
                ScrollableTabRow(
                    pagerState.currentPage,
                    Modifier.weight(1F)
                ) {
                    (0..<pagerState.pageCount).forEach { index ->
                        Tab(
                            pagerState.currentPage == index,
                            {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            Modifier.height(48.dp)
                        ) {
                            Text(
                                stringResource(DayOfWeek.entries[index].nameRes),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                var showMenu by remember { mutableStateOf(false) }
                IconButton({ showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null)

                    DropdownMenu(showMenu, { showMenu = false }) {
                        DropdownMenuItem({
                            Text(stringResource(R.string.jump_to_date_title))
                        }, {
                            showMenu = false
                            showDatePicker = true
                        })

                        DropdownMenuItem({
                            if (overridesChecking) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.checking_overrides))

                                    CircularProgressIndicator(
                                        Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            } else {
                                Text(stringResource(R.string.check_overrides))
                            }
                        }, {

                            checkOverrides { isFailure ->
                                showMenu = false

                                snackbar(
                                    when {
                                        isFailure -> overridesRetrieveError
                                        overrides?.takeIf { it.overridesCount > 0 } == null -> noOverrides
                                        else -> overridesDetected
                                    }
                                )
                            }
                        }, enabled = !overridesChecking)

                        DropdownMenuItem({
                            Text(stringResource(R.string.change_group))
                        }, { goToWelcome() })

                        DropdownMenuItem({ Text(stringResource(R.string.about)) }, {
                            scope.launch {
                                about.show()
                            }
                            showMenu = false
                        })
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton({ isSelectedWeekEven = !isSelectedWeekEven }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                        Image(
                            painterResource(
                                if (isSelectedWeekEven)
                                    R.drawable.timer_2_24px
                                else R.drawable.timer_1_24px
                            ),
                            null,
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    Column {
                        Text(
                            stringResource(
                                if (isSelectedWeekEven)
                                    R.string.second_week
                                else
                                    R.string.first_week
                            ),
                            style = MaterialTheme.typography.labelLarge
                        )

                        Text(
                            stringResource(
                                if (isSelectedWeekEven == isCurrentWeekEven)
                                    R.string.current_week
                                else
                                    R.string.next_week
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        BackHandler {
            isSelectedWeekEven =
                if (weekday() >= TAB_AMOUNT) !isCurrentWeekEven else isCurrentWeekEven
            scope.launch {
                pagerState.animateScrollToPage(
                    weekday().let { weekday ->
                        if (weekday >= pagerState.pageCount) 0
                        else weekday
                    }
                )
            }
        }

        HorizontalPager(
            pagerState,
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            if (schedule == null) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@HorizontalPager
            }

            schedule?.let { schedule ->
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val overs = overrides  // Allow smart cast

                    val sched: Schedule = if (applyOverrides && overs != null) schedule.let {
                        it
                            .toBuilder()
                            .setWeeks(
                                overs.weekNum,
                                it.weeksList[overs.weekNum]
                                    .toBuilder()
                                    .setDays(
                                        overs.weekDay,
                                        it.weeksList[overs.weekNum].daysList[overs.weekDay]
                                            .toBuilder()
                                            .let { day ->
                                                overs.overridesList.forEach { over ->
                                                    //TODO: subgrouped lesson shit

                                                    /*if (
                                                        day
                                                            .lessonList
                                                            .getOrNull(over.index)
                                                            ?.hasSubgroupedLesson() == true
                                                        && over.willBe.hasSubgroupedLesson()
                                                    ) {
                                                        day.setLesson(
                                                            over.index,
                                                            Lesson.newBuilder()
                                                                .setSubgroupedLesson(
                                                                    SubgroupedLesson.newBuilder()
                                                                        .let { sublesson ->
                                                                            over.willBe.subgroupedLesson

                                                                            sublesson
                                                                        }
                                                                        .build()
                                                                )
                                                                .build()
                                                        )
                                                    } else {

                                                    }*/

                                                    day.setLesson(over.index, over.willBe)
                                                }
                                                day
                                            }
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    } else schedule

                    val list = sched
                        .weeksList[if (isSelectedWeekEven) 1 else 0]
                        .daysList[page]
                        .lessonList

                    item {
                        Spacer(Modifier.height(0.dp))
                    }

                    val timestampType = when (page) {
                        1 -> TimestampType.ClassHour
                        5 -> TimestampType.Saturday
                        else -> TimestampType.Normal
                    }
                    val isToday = (isCurrentWeekEven == isSelectedWeekEven)
                            && (weekday() == page)

                    items(list.size + if (timestampType == TimestampType.ClassHour) 1 else 0) { i ->
                        val index =
                            i - if (timestampType == TimestampType.ClassHour && i > 3) 1 else 0

                        key(updateItems) {
                            when {
                                timestampType == TimestampType.ClassHour && i == 3 -> ClassHour(
                                    isToday && timestampType.timestamps.getOrNull(3)
                                        ?.isNow() == true
                                )

                                index < 5 || !list[index].hasNoLesson() -> ScheduleItem(
                                    list[index],
                                    null,
                                    index,
                                    isToday && timestampType.timestamps.getOrNull(
                                        if (timestampType == TimestampType.ClassHour && index > 2) index + 1 else index
                                    )?.isNow() == true,
                                    timestampType,
                                    isTeacher
                                ) {
                                    scope.launch {
                                        currentLesson = list[index]
                                        if (currentLesson != null) {
                                            lessonInfo.show()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (
                        overs != null &&
                        overs.overridesCount > 0 &&
                        (overs.weekNum == 1) == isSelectedWeekEven &&
                        overs.weekDay == page
                    ) {
                        item {
                            ListItem(
                                {
                                    Text(stringResource(R.string.apply_overrides))
                                },
                                Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        applyOverrides = !applyOverrides
                                    },
                                leadingContent = {
                                    Checkbox(
                                        applyOverrides,
                                        { applyOverrides = !applyOverrides }
                                    )
                                })
                        }
                    }

                    item {
                        Spacer(Modifier.height(56.dp))
                    }
                }
            }
        }
    }
}