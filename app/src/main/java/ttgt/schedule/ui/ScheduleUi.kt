package ttgt.schedule.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import ttgt.schedule.DayOfWeek
import ttgt.schedule.Icon
import ttgt.schedule.R
import ttgt.schedule.api.Client
import ttgt.schedule.datastoreKey
import ttgt.schedule.dayOfWeek
import ttgt.schedule.api.editProfile
import ttgt.schedule.getLessonData
import ttgt.schedule.isEmpty
import ttgt.schedule.api.profile
import ttgt.schedule.display
import ttgt.schedule.getSetting
import ttgt.schedule.name
import ttgt.schedule.proto.CustomLesson
import ttgt.schedule.proto.Lesson
import ttgt.schedule.proto.LessonUserData
import ttgt.schedule.proto.OverrideHistoryElement
import ttgt.schedule.proto.Overrides
import ttgt.schedule.proto.Profile
import ttgt.schedule.proto.ProfileType
import ttgt.schedule.proto.Profiles
import ttgt.schedule.proto.Schedule
import ttgt.schedule.settingsDataStore
import ttgt.schedule.sortString
import ttgt.schedule.ui.sheets.About
import ttgt.schedule.ui.sheets.LessonAdd
import ttgt.schedule.ui.sheets.OverrideHistoryDisplay
import ttgt.schedule.ui.theme.ScheduleTheme
import ttgt.schedule.updateWidgets
import ttgt.schedule.vector
import ttgt.schedule.year
import java.util.Calendar
import kotlin.time.ExperimentalTime

const val TAB_AMOUNT = 5
const val FEEDBACK_URL = "https://t.me/ttgt1bot"

val startOfYear: Calendar = Calendar.getInstance().apply {
    set(Calendar.YEAR, 2025)
    set(Calendar.DAY_OF_YEAR, 1)
}

fun weekNum() = weekNum(
    Calendar.getInstance()
)

fun weekNum(inputDate: Calendar): Boolean {
    var out = true

    val currentDate = Calendar.getInstance().apply { time = startOfYear.time }

    while (currentDate.before(inputDate)) {
        if (currentDate.dayOfWeek == DayOfWeek.SUNDAY.ordinal) {
            out = !out
        }

        currentDate.add(Calendar.DAY_OF_YEAR, 1)
    }

    if (inputDate.dayOfWeek == DayOfWeek.SUNDAY.ordinal)
        out = !out

    return out
}

fun weekday() =
        Calendar
            .getInstance()
            .dayOfWeek

enum class LoadingState {
    PreLoading, Loading, Error, Done
}

fun Long.timestamp(): Calendar = Calendar.getInstance().apply {
    timeInMillis = this@timestamp
}

@Preview
@Composable
fun MenuTest() {
    Box(Modifier.size(256.dp)) {
        Box {
            DropdownMenu(true, { }) {
                DropdownMenuItem({ Text("Icon") }, {}, leadingIcon = { Icon(R.drawable.info) })
                DropdownMenuItem({ Text("Load") }, {}, leadingIcon = {
                    Box(Modifier.padding(2.dp)) {
                        CircularProgressIndicator(
                            Modifier.size(21.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                })
            }
        }
    }
}

fun isAtLeastOneWeekAfter(timestamp: Long): Boolean {
    val calendar = Calendar.getInstance()
    val currentTime = calendar.timeInMillis / 1000 // Convert to seconds

    val oneWeekInSeconds = 7 * 24 * 60 * 60 // 7 days in seconds
    return currentTime - timestamp >= oneWeekInSeconds
}

fun currentTimestamp() = Calendar.getInstance().timeInMillis / 1000

@OptIn(ExperimentalMaterial3Api::class)
object NoWeekendsSelectableDates : SelectableDates {
    @OptIn(ExperimentalTime::class)
    override fun isSelectableDate(utcTimeMillis: Long) =
        DayOfWeek.entries.toTypedArray()[utcTimeMillis
            .timestamp()
            .dayOfWeek] !in listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.SATURDAY
        )

    override fun isSelectableYear(year: Int) = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleUi(goToWelcome: () -> Unit) = ScheduleTheme {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var weekday = remember { weekday() }
    val pagerState = rememberPagerState(
        weekday.let { weekday ->
            if (weekday >= TAB_AMOUNT) 0
            else weekday
        }
    ) { TAB_AMOUNT }
    var isCurrentWeekEven by remember { mutableStateOf(weekNum()) }  // Чётная?
    var isSelectedWeekEven by remember {
        mutableStateOf(
            if (weekday >= TAB_AMOUNT) !isCurrentWeekEven else isCurrentWeekEven
        )
    }  // Чётная?
    var schedule: Schedule? by remember { mutableStateOf(null) }
    var overrides: Overrides? by remember { mutableStateOf(null) }
    var applyOverrides by remember { mutableStateOf(true) }
    var overridesChecking by remember { mutableStateOf(false) }
    val about = rememberModalBottomSheetState()
    val lessonAdd = rememberModalBottomSheetState()
    var customLessonEdit by remember { mutableIntStateOf(-1) }

    val now = Calendar.getInstance()

    val datePicker = rememberDatePickerState(
        yearRange = now.year..now.year + 2,
        selectableDates = NoWeekendsSelectableDates
    )
    var showDatePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    var lastUsed by remember { mutableStateOf(ProfileType.TEACHER) }

    var updateItems by remember { mutableStateOf(false) }

//    LaunchedEffect(Unit) {
//        while (true) {
//            delay(1000)
//            updateItems = !updateItems
//        }
//    }

    var currentLesson: Lesson? by remember { mutableStateOf(null) }
    val lessonInfo = rememberModalBottomSheetState()

    val openLink = stringResource(R.string.open_overrides)
    val overridesRetrieveError = stringResource(R.string.overrides_retrieve_error)
    val noOverrides = stringResource(R.string.no_overrides)
    val overridesDetected = stringResource(R.string.overrides_detected)

    val isToday = (isCurrentWeekEven == isSelectedWeekEven)
            && (weekday == pagerState.currentPage)

    val arrowRotationAnimated =  animateFloatAsState(
        if (
            isToday && overrides
                ?.overridesList
                ?.isNotEmpty() == true
            ) 180F else 0F
    )

    val profileSwitcherRotationAnimated = remember {
        Animatable(0F)
    }

    var profiles: Profiles? by remember { mutableStateOf(null) }
    var profile: Profile? by remember { mutableStateOf(null) }

    val overrideHistory = remember { mutableStateListOf<OverrideHistoryElement>() }
    val overrideHistorySheet = rememberModalBottomSheetState(true)
    var askForFeedback by remember { mutableStateOf(false) }
    val customLessons = remember { mutableStateListOf<CustomLesson>() }

    val filteredCustomLessons = remember { mutableStateListOf<CustomLesson>() }

    fun snackbar(text: String) = scope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()

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

            SnackbarResult.Dismissed -> {}
        }
    }

    if (lessonInfo.isVisible || lessonInfo.isAnimationRunning) {
        currentLesson?.let { lesson ->
            val name = lesson.commonLesson.name.ifBlank { lesson.subgroupedLesson.name }
            var notes by remember { mutableStateOf("") }
            var subgroup by remember { mutableIntStateOf(0) }

            suspend fun updateLesson(setData: LessonUserData.Builder.() -> Unit) {
                context.settingsDataStore.updateData {
                    it.toBuilder()
                        .apply {
                            if (lessonDataMap.containsKey(lesson.datastoreKey)) {
                                putLessonData(
                                    lesson.datastoreKey,
                                    lessonDataMap
                                        .getValue(lesson.datastoreKey)
                                        .toBuilder()
                                        .apply { setData() }
                                        .build()
                                )
                            } else {
                                putLessonData(
                                    lesson.datastoreKey,
                                    LessonUserData
                                        .newBuilder()
                                        .apply { setData() }
                                        .build()
                                )
                            }
                        }
                        .build()
                }
            }

            ModalBottomSheet({
                scope.launch {
                    updateLesson {
                        setNotes(notes)
                    }

                    lessonInfo.hide()
                }.invokeOnCompletion {
                    currentLesson = null
                }
            }) {
                LaunchedEffect(Unit) {
                    context.getLessonData(lesson)?.let { data ->
                        notes = data.notes
                        subgroup = data.subgroup
                    }
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
                    if (lastUsed == ProfileType.TEACHER) {
                        Text(
                            lesson.group,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6F)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    lesson
                        .subgroupedLesson
                        .subgroupsCount
                        .takeIf { it > 0 }
                        ?.let { count ->
                            Row(
                                Modifier
                                    .fillMaxWidth(),
                                Arrangement.spacedBy(8.dp),
                                Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.subgroup))

                                repeat(count) { i ->
                                    val index = i + 1

                                    Button({
                                        scope.launch {
                                            updateLesson {
                                                setSubgroup(index)
                                            }

                                            context.updateWidgets()
                                        }.invokeOnCompletion {
                                            subgroup = index
                                            updateItems = !updateItems
                                        }
                                    }, colors = if (index == subgroup) ButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = Color.Transparent,
                                        disabledContentColor = Color.Transparent
                                    ) else ButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        disabledContainerColor = Color.Transparent,
                                        disabledContentColor = Color.Transparent
                                    )) {
                                        Text(index.toString())
                                    }
                                }
                            }
                        }

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
    About(about)
    OverrideHistoryDisplay(
        overrideHistorySheet,
        overrideHistory.filter { it.overrides.overridesCount > 0 && it.itemName == profile?.name },
        lastUsed == ProfileType.TEACHER
    ) {
        overrideHistory.clear()
    }
    LessonAdd(
        lessonAdd,
        pagerState.currentPage,
        isSelectedWeekEven,
        if (customLessonEdit != -1) {
            customLessons[customLessonEdit].let {
                LessonTime(
                    Time.from(it.startTime),
                    Time.from(it.endTime)
                )
            }
        } else null,
        if (customLessonEdit > -1) customLessons[customLessonEdit].name else "",
        customLessonEdit
    ) { customLesson ->
        when {
            customLesson == null -> customLessons.removeAt(customLessonEdit)
            customLessonEdit == -1 -> customLessons.add(customLesson)
            else -> {
                customLessons[customLessonEdit] = customLesson
                customLessonEdit = -1
            }
        }

        updateItems = !updateItems
    }

    if (askForFeedback) {
        AlertDialog({
            askForFeedback = false
        }, {
            TextButton({
                askForFeedback = false
                scope.launch { context.settingsDataStore.updateData { it.toBuilder().setFeedbackSent(true).build() } }
                uriHandler.openUri(FEEDBACK_URL)
            }) {
                Text(stringResource(R.string.feedback_confirm))
            }
        }, text = {
            Text(stringResource(R.string.feedback_please))
        }, dismissButton = {
            TextButton({
                askForFeedback = false
            }) {
                Text(stringResource(R.string.feedback_deny))
            }
        })
    }

    fun checkOverrides(onDone: (Boolean) -> Unit) {
        if (overridesChecking) return
        overridesChecking = true

        scope.launch {
            runCatching {
                val profile = profiles?.profile(lastUsed) ?: return@runCatching null

                Client.overrides(profile.name)
            }.apply {
                exceptionOrNull()?.printStackTrace()

                runOnUiThread {
                    overridesChecking = false
                    onDone(isFailure)
                }

                if (isSuccess) {
                    overrides = getOrNull()?.toBuilder()
                        ?.let {
                            val overs = it.overridesList.map { override ->
                                override.toBuilder()
                                    .setShouldBe(
                                        schedule
                                            ?.weeksList[it.weekNum]
                                            ?.daysList[it.weekDay]
                                            ?.lessonList[override.index]
                                    )
                                    .build()
                            }
                            it.clearOverrides().addAllOverrides(overs)
                        }
                        ?.build()

                    val string = overrides?.date?.sortString()
                    println()
                    overrideHistory.map {
                        """
                            ${it.itemName} ${it.overrides.date.display()}
                            ${it.overrides.overridesList.joinToString { 
                                "${it.index +1}. ${it.shouldBe.name}"
                            }}
                        """.trimIndent()
                    }.forEach(::println)

                    val overrideToAppend = if (
                        string != null && overrideHistory
                            .firstOrNull {
                                it
                                    .overrides
                                    .date
                                    .sortString() == string
                                        && it.itemName == profile?.name
                            } == null
                    ) {
                        OverrideHistoryElement.newBuilder()
                            .setOverrides(overrides)
                            .setItemName(profile!!.name)
                            .build()
                    } else {
                        null
                    }
                    println(overrideToAppend)
                    if (overrideToAppend != null) {
                        overrideHistory.add(overrideToAppend)
                    }

                    context.settingsDataStore.updateData {
                        it.toBuilder().editProfile(lastUsed) {
                            setOverrides(
                                overrides ?: Overrides.newBuilder().build()
                            )
                        }.let {
                            if (overrideToAppend != null) {
                                it.addOverrideHistory(
                                    overrideToAppend
                                )
                            } else it
                        }.build()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        lastUsed = context.getSetting { this.lastUsed } ?: ProfileType.STUDENT
        profiles = context.getSetting { this.profiles }
        overrideHistory.addAll(
            context.getSetting { overrideHistoryList } ?: emptyList()
        )

        profile = profiles?.profile(lastUsed) ?: return@LaunchedEffect goToWelcome()

        schedule = profile!!.schedule
        overrides = profile!!.overrides

        context.getSetting { customLessonsList }?.let {
            customLessons.addAll(it)
        }

        checkOverrides { }

        val sentFeedback = context.getSetting { feedbackSent } ?: false
        val firstLaunch = context.getSetting { firstTimeLaunch } ?: 0

        if (firstLaunch == 0L) {
            context.settingsDataStore.updateData { it.toBuilder().setFirstTimeLaunch(currentTimestamp()).build() }
        } else if (!sentFeedback && isAtLeastOneWeekAfter(firstLaunch)) {
            askForFeedback = true
        }
    }

    fun backPress() {
        if (isToday && (overrides?.overridesCount ?: 0) > 0) {
            overrides?.let { overrides ->
                isSelectedWeekEven = overrides.weekNum == 1

                scope.launch {
                    pagerState.animateScrollToPage(
                        overrides.weekDay
                    )
                }
            }
        } else {
            isSelectedWeekEven =
                if (weekday >= TAB_AMOUNT) !isCurrentWeekEven else isCurrentWeekEven

            scope.launch {
                pagerState.animateScrollToPage(
                    weekday.let { weekday ->
                        if (weekday >= pagerState.pageCount) 0
                        else weekday
                    }
                )
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch {
            checkOverrides { }
        }
        isCurrentWeekEven = weekNum()
        weekday = weekday()
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
                                pagerState.animateScrollToPage(date.dayOfWeek)
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

                profiles?.let { profiles ->
                    if (profiles.hasStudent() && profiles.hasTeacher()) {
                        IconButton({
                            if (!profileSwitcherRotationAnimated.isRunning) {
                                scope.launch {
                                    profileSwitcherRotationAnimated.animateTo(
                                        180F,
                                        tween(500)
                                    )
                                    profileSwitcherRotationAnimated.snapTo(0F)
                                }
                            }

                            lastUsed = when (lastUsed) {
                                ProfileType.TEACHER -> ProfileType.STUDENT
                                ProfileType.STUDENT -> ProfileType.TEACHER
                                else -> ProfileType.STUDENT
                            }

                            val profile = profiles.profile(lastUsed)!!
                            schedule = profile.schedule
                            overrides = profile.overrides

                            scope.launch {
                                context.settingsDataStore.updateData {
                                    it.toBuilder().setLastUsed(lastUsed).build()
                                }
                            }

                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()

                                snackbarHostState
                                    .showSnackbar(
                                        message = when (lastUsed) {
                                            ProfileType.TEACHER -> "Преподаватель: "
                                            ProfileType.STUDENT -> "Группа: "
                                            else -> ""
                                        } + profile.name,
                                        duration = SnackbarDuration.Short,
                                        withDismissAction = true
                                    )
                            }
                        }) {
                            Icon(
                                R.drawable.switch_profile.vector,
                                null,
                                Modifier
                                    .rotate(profileSwitcherRotationAnimated.value)
                            )
                        }
                    }
                }

                var showMenu by remember { mutableStateOf(false) }
                IconButton({ showMenu = true }) {
                    Icon(R.drawable.more)

                    DropdownMenu(showMenu, { showMenu = false }) {
                        DropdownMenuItem({
                            if (overridesChecking) {
                                Text(stringResource(R.string.checking_overrides))
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
                        },
                            leadingIcon = {
                                if (overridesChecking) {
                                    Box(Modifier.padding(2.dp)) {
                                        CircularProgressIndicator(
                                            Modifier.size(21.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Icon(R.drawable.overrides)
                                }
                            },
                            enabled = !overridesChecking
                        )

                        DropdownMenuItem(
                            {
                                Text(stringResource(R.string.change_group))
                            },
                            {
                                showMenu = false
                                goToWelcome()
                            },
                            leadingIcon = {
                                Icon(R.drawable.profile)
                            }
                        )

                        if (overrideHistory.isNotEmpty()) {
                            DropdownMenuItem(
                                { Text(stringResource(R.string.overrides_history)) },
                                {
                                    showMenu = false
                                    scope.launch { overrideHistorySheet.show() }
                                },
                                leadingIcon = { Icon(R.drawable.history) }
                            )
                        }

                        DropdownMenuItem({
                            Text(stringResource(R.string.add_lesson))
                        }, {
                            showMenu = false
                            scope.launch { lessonAdd.show() }
                        },
                            leadingIcon = {
                                Icon(R.drawable.add)
                            })

                        DropdownMenuItem(
                            {
                                Text(stringResource(R.string.feedback))
                            },
                            {
                                showMenu = false
                                scope.launch { context.settingsDataStore.updateData { it.toBuilder().setFeedbackSent(true).build() } }
                                uriHandler.openUri(FEEDBACK_URL)
                            },
                            leadingIcon = { Icon(R.drawable.feedback) }
                        )

                        DropdownMenuItem({ Text(stringResource(R.string.about)) }, {
                            showMenu = false
                            scope.launch {
                                about.show()
                            }
                        }, leadingIcon = { Icon(R.drawable.info) })
                    }
                }
            }
        },
        bottomBar = {
            Row(
                Modifier
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
                    .background(Color.Transparent),
                Arrangement.SpaceAround,
                Alignment.CenterVertically
            ) {
                val enableBack = !isToday || (overrides?.overridesCount ?: 0) > 0

                IconButton(
                    ::backPress,
                    enabled = enableBack
                ) {
                    if (enableBack) {
                        Icon(
                            R.drawable.arrow.vector,
                            null,
                            Modifier.rotate(arrowRotationAnimated.value)
                        )
                    }
                }

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

                IconButton({
                    showDatePicker = true
                }) {
                    Icon(R.drawable.calendar)
                }
            }
        }
    ) { innerPadding ->
        BackHandler(onBack = ::backPress)

        HorizontalPager(
            pagerState,
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            val isToday = (isCurrentWeekEven == isSelectedWeekEven)
                    && (weekday == page)

            if (schedule == null) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@HorizontalPager
            }

            key(isSelectedWeekEven, updateItems) {
                schedule?.let { schedule ->
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val overs = overrides  // Allow smart cast

                        val sched: Schedule = if (applyOverrides && overs != null) {
                            schedule.let {
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
                            }
                        } else {
                            schedule
                        }

                        val list = sched
                            .weeksList[if (isSelectedWeekEven) 1 else 0]
                            .daysList[page]
                            .lessonList
                            .filterIndexed { index, item ->
                                index < 5 || !item.isEmpty()
                            }

                        // Отступ сверху
                        item { Box {} }

                        val timestampType = TimestampType.fromWeekday(page)
                        val normalLessonSize = list.size + if (timestampType == TimestampType.ClassHour) 1 else 0

                        items(normalLessonSize) { i ->
                            val index =
                                i - if (timestampType == TimestampType.ClassHour && i > 3) 1 else 0

                            when {
                                timestampType == TimestampType.ClassHour && i == 3 -> ClassHour(
                                    isToday && timestampType.timestamps.getOrNull(3)
                                        ?.isNow() == true
                                )

                                index < 5 || !list[index].isEmpty() -> ScheduleItem(
                                    list[index],
                                    index,
                                    isToday,
                                    timestampType,
                                    lastUsed == ProfileType.TEACHER
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

                        println("""
                            --- /// ---
                        $updateItems
                        """.trimIndent())

                        filteredCustomLessons.addAll(
                            customLessons.filter {
                                it.weeknum == isCurrentWeekEven && it.weekday == pagerState.currentPage
                            }.sortedBy { Time.from(it.startTime) }
                        )

                        if (filteredCustomLessons.isNotEmpty()) {
                            item {
                                HorizontalDivider()
                            }
                        }

                        items(filteredCustomLessons.size) {
                            val lesson = filteredCustomLessons[it]

                            CustomLessonItem(
                                lesson,
                                normalLessonSize + it,
                                isToday,
                            ) {
                                customLessonEdit = it
                                scope.launch { lessonAdd.show() }
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
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}