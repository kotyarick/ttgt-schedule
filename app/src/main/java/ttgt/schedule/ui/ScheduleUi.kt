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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ttgt.schedule.DayOfWeek
import ttgt.schedule.Icon
import ttgt.schedule.R
import ttgt.schedule.api.Client
import ttgt.schedule.api.Update
import ttgt.schedule.datastoreKey
import ttgt.schedule.dayOfWeek
import ttgt.schedule.api.editProfile
import ttgt.schedule.getLessonData
import ttgt.schedule.isEmpty
import ttgt.schedule.api.profile
import ttgt.schedule.proto.Lesson
import ttgt.schedule.proto.LessonUserData
import ttgt.schedule.proto.Overrides
import ttgt.schedule.proto.ProfileType
import ttgt.schedule.proto.Profiles
import ttgt.schedule.proto.Schedule
import ttgt.schedule.settingsDataStore
import ttgt.schedule.ui.theme.ScheduleTheme
import ttgt.schedule.updateWidgets
import ttgt.schedule.vector
import ttgt.schedule.year
import java.util.Calendar
import kotlin.time.ExperimentalTime

const val TAB_AMOUNT = 5

val startOfYear: Calendar = Calendar.getInstance().apply {
    set(Calendar.YEAR, 2025)
    set(Calendar.DAY_OF_YEAR, 1)
}

fun weekNum() = weekNum(
    Calendar.getInstance()
)

fun weekNum(inputDate: Calendar): Boolean {
    //inputDate.add(Calendar.DAY_OF_YEAR, -1)
    var out = true

    val currentDate = Calendar.getInstance().apply { time = startOfYear.time }

    while (currentDate.before(inputDate)) {
        if (currentDate.dayOfWeek == DayOfWeek.SUNDAY.ordinal) {
            out = !out
        }

        currentDate.add(Calendar.DAY_OF_YEAR, 1)
    }

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

    val now = Calendar.getInstance()

    val datePicker = rememberDatePickerState(
        yearRange = now.year..now.year + 2,
        selectableDates = NoWeekendsSelectableDates
    )
    var showDatePicker by remember { mutableStateOf(false) }

    var versionInfo: Update? by remember { mutableStateOf(null) }
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

    fun checkUpdate() = scope.launch {
        runCatching {
            Client.updates()
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
                                Icon(R.drawable.done)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    } else {
                        Button({
                            scope.launch {
                                uriHandler.openUri(Client.downloadUpdateUrl)
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
                            Icon(R.drawable.refresh)
                        },
                        modifier = Modifier.clickable {
                            versionLoadingState = LoadingState.PreLoading
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                val profile = profiles?.profile(lastUsed) ?: return@runCatching null

                Client.overrides(profile.name)
            }.apply {
                exceptionOrNull()?.printStackTrace()

                runOnUiThread {
                    overrides = getOrNull()
                    overridesChecking = false
                    onDone(isFailure)
                }
                if (isSuccess) {
                    context.settingsDataStore.updateData {
                        it.toBuilder().editProfile(lastUsed) {
                            setOverrides(
                                overrides ?: Overrides.newBuilder().build()
                            )
                        }.build()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        var lastUsedTmp: ProfileType? = null
        var profilesTmp: Profiles? = null

        scope.launch {
            lastUsedTmp = context.settingsDataStore.data.map { it.lastUsed }.firstOrNull()
            profilesTmp = context.settingsDataStore.data.map { it.profiles }.firstOrNull()
        }.invokeOnCompletion {
            lastUsed = lastUsedTmp ?: ProfileType.STUDENT
            profiles = profilesTmp

            val profile = profiles!!.profile(lastUsed)!!

            schedule = profile.schedule
            overrides = profile.overrides

            if (overrides?.takeIf { it.overridesCount > 0 } == null) {
                checkOverrides { }
            }
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
                                        tween(1000)
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
                                    CircularProgressIndicator(
                                        Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
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

                        DropdownMenuItem(
                            {
                                Text(stringResource(R.string.feedback))
                            },
                            {
                                showMenu = false
                                uriHandler.openUri("https://t.me/ttgt1bot")
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

                        // Отступ сверху
                        item { Box {} }

                        val timestampType = TimestampType.fromWeekday(page)

                        items(list.size + if (timestampType == TimestampType.ClassHour) 1 else 0) { i ->
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