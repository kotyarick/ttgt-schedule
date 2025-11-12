package ttgt.schedule.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ttgt.schedule.Icon
import ttgt.schedule.R
import ttgt.schedule.api.Client
import ttgt.schedule.api.editProfile
import ttgt.schedule.proto.ProfileType
import ttgt.schedule.settingsDataStore
import ttgt.schedule.ui.theme.ScheduleTheme
import ttgt.schedule.vector


private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

fun runOnUiThread(block: suspend () -> Unit) = uiScope.launch { block() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Welcome(goToSchedule: () -> Unit) = ScheduleTheme {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val groups = remember { mutableStateListOf<String>() }
    val teachers = remember { mutableStateListOf<String>() }
    var isError by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf("") }
    var changeToRefresh by remember { mutableStateOf(false) }
    var continueLoading by remember { mutableStateOf(false) }
    var loginAs by remember { mutableStateOf(ProfileType.STUDENT) }
    var teacherQuery by remember { mutableStateOf("") }
    var selectedTeacher by remember { mutableStateOf("") }

    LaunchedEffect(changeToRefresh) {
        scope.launch(Dispatchers.IO) {
            try {
                val items = Client.items()

                groups.clear()
                groups.addAll(items.groups)

                teachers.clear()
                teachers.addAll(items.teachers)
            } catch (error: Throwable) {
                error.printStackTrace()
                isError = true
            }
        }
    }

    var course by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar({
                Text(
                    stringResource(
                        when (loginAs) {
                            ProfileType.STUDENT -> R.string.group_selection
                            ProfileType.TEACHER -> R.string.teacher_selection
                            else -> 0
                        }
                    )
                )
            })
        },
        floatingActionButton = {
            if (
                (selectedGroup.isNotBlank() && loginAs == ProfileType.STUDENT)
                || (selectedTeacher.isNotBlank() && loginAs == ProfileType.TEACHER)
            ) {
                ExtendedFloatingActionButton({
                    if (continueLoading) return@ExtendedFloatingActionButton

                    continueLoading = true
                    var erroring = false

                    val itemName = if (loginAs == ProfileType.TEACHER) selectedTeacher else selectedGroup

                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            Client.schedule(itemName)
                        }.apply {
                            erroring = isFailure

                            exceptionOrNull()?.printStackTrace()

                            val schedule = getOrNull()

                            if (!isFailure) {
                                context.settingsDataStore.updateData {
                                    it.toBuilder().editProfile(loginAs) {
                                        setSchedule(schedule)
                                            .setName(itemName)
                                    }
                                        .setLastUsed(loginAs).build()
                                }
                            }
                        }
                    }.invokeOnCompletion {
                        continueLoading = false

                        runOnUiThread {
                            if (!erroring) goToSchedule()
                        }
                    }
                }) {
                    if (continueLoading) {
                        Text(stringResource(R.string.loading))
                        Spacer(Modifier.width(10.dp))
                        CircularProgressIndicator(Modifier.size(20.dp))
                    } else {
                        Text(stringResource(R.string.continu))
                        Icon(R.drawable.next)
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ListItem(
                {
                    Text(
                        stringResource(
                            when (loginAs) {
                                ProfileType.STUDENT -> R.string.login_as_teacher
                                ProfileType.TEACHER -> R.string.login_as_student
                                else -> 0
                            }
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardDefaults.shape)
                    .clickable {
                        loginAs =
                            if (loginAs == ProfileType.TEACHER) ProfileType.STUDENT
                            else ProfileType.TEACHER
                    }
            )
            Card(
                Modifier.fillMaxWidth()
            ) {
                when (loginAs) {
                    ProfileType.STUDENT -> Row(
                        Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.course_selection),
                            style = MaterialTheme.typography.labelLarge
                        )
                        (1..4).forEach {
                            Button(
                                {
                                    course = if (course == it) 0 else it
                                },
                                Modifier.weight(1F),
                                colors = if (course == it) ButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = Color.Transparent,
                                    disabledContentColor = Color.Transparent
                                ) else ButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = Color.Transparent,
                                    disabledContentColor = Color.Transparent
                                )
                            ) {
                                Text(it.toString())
                            }
                        }
                    }

                    ProfileType.TEACHER -> {
                        TextField(
                            teacherQuery, { teacherQuery = it },
                            Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.search)) },
                            maxLines = 1,
                            leadingIcon = { Icon(R.drawable.search) },
                            colors = TextFieldDefaults.colors(
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }

                    else -> {}
                }
            }

            Card(
                Modifier
                    .weight(1F)
                    .fillMaxWidth()
            ) {
                when {
                    loginAs == ProfileType.TEACHER && teachers.isNotEmpty() -> {
                        LazyColumn {
                            val filteredTeachers = teachers.filter { teacher ->
                                if (!teacher.contains(".")) return@filter false
                                if (teacherQuery.isBlank()) return@filter true

                                fun String.normalize() = lowercase()
                                    .replace(" ", "")
                                    .replace(".", "")

                                teacher
                                    .normalize()
                                    .contains(
                                        teacherQuery
                                            .normalize()
                                    )
                            }.sorted()

                            items(filteredTeachers.size) { index ->
                                val teacher = filteredTeachers[index]

                                ListItem(
                                    {
                                        Text(
                                            teacher,
                                            fontWeight = if (teacher == selectedTeacher)
                                                FontWeight.Bold
                                            else FontWeight.Normal
                                        )
                                    },
                                    Modifier.clickable {
                                        selectedTeacher = teacher
                                    },
                                    colors = ListItemDefaults.colors().copy(
                                        containerColor = Color.Transparent
                                    ),
                                    trailingContent = {
                                        if (teacher == selectedTeacher) {
                                            Icon(R.drawable.done)
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }

                    loginAs == ProfileType.STUDENT && groups.isNotEmpty() -> {
                        Box(
                            Modifier
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                groups.sorted().forEach { group ->
                                    if (course != 0 && !group.contains("-$course-")) return@forEach

                                    Card(
                                        { selectedGroup = group },
                                        Modifier
                                            .border(
                                                width = 1.dp,
                                                color = if (selectedGroup == group) MaterialTheme.colorScheme.inversePrimary else Color.Black,
                                                shape = RoundedCornerShape(5.dp)
                                            )
                                            .weight(1F),
                                        shape = RoundedCornerShape(5.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedGroup == group)
                                                MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                    ) {
                                        Box(
                                            Modifier
                                                .padding(
                                                    horizontal = if (selectedGroup == group) 10.dp else 13.dp,
                                                    vertical = 13.dp
                                                )
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                group,
                                                textAlign = TextAlign.Center,
                                                fontWeight = if (selectedGroup == group) FontWeight.Bold
                                                else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    isError -> {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(15.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                R.drawable.signal_wifi_off.vector,
                                null,
                                Modifier.size(50.dp)
                            )

                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.request_error_title),
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                stringResource(R.string.request_error_body),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(10.dp))

                            Button({
                                isError = false
                                changeToRefresh = !changeToRefresh
                            }) {
                                Text(stringResource(R.string.try_again))
                            }
                        }
                    }

                    else -> {
                        Row(
                            Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}