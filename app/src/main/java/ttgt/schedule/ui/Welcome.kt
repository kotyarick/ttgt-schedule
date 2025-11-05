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
import ttgt.schedule.empty
import ttgt.schedule.proto.Group
import ttgt.schedule.proto.GroupId
import ttgt.schedule.proto.Teacher
import ttgt.schedule.settingsDataStore
import ttgt.schedule.stub
import ttgt.schedule.ui.theme.ScheduleTheme
import ttgt.schedule.vector


private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

fun runOnUiThread(block: suspend () -> Unit) = uiScope.launch { block() }

enum class UserType {
    Student, Teacher
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Welcome(goToSchedule: () -> Unit) = ScheduleTheme {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val groups = remember { mutableStateListOf<Group>() }
    val teachers = remember { mutableStateListOf<String>() }
    var isError by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf("") }
    var changeToRefresh by remember { mutableStateOf(false) }
    var continueLoading by remember { mutableStateOf(false) }
    var loginAs by remember { mutableStateOf(UserType.Student) }
    var teacherQuery by remember { mutableStateOf("") }
    var selectedTeacher by remember { mutableStateOf("") }

    LaunchedEffect(changeToRefresh) {
        scope.launch(Dispatchers.IO) {
            try {
                val g = stub.getGroups(empty).groupsList
                groups.clear()
                groups.addAll(g)
            } catch (error: Throwable) {
                error.printStackTrace()
                isError = true
            }
        }

        scope.launch(Dispatchers.IO) {
            try {
                teachers.addAll(stub.getTeachers(empty).teacherList)
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
                            UserType.Student -> R.string.group_selection
                            UserType.Teacher -> R.string.teacher_selection
                        }
                    )
                )
            })
        },
        floatingActionButton = {
            if (
                (selectedGroup.isNotBlank() && loginAs == UserType.Student)
                || (selectedTeacher.isNotBlank() && loginAs == UserType.Teacher)
            ) {
                ExtendedFloatingActionButton({
                    if (continueLoading) return@ExtendedFloatingActionButton

                    continueLoading = true
                    var erroring = false
                    when (loginAs) {
                        UserType.Student -> {
                            val groupId = GroupId.newBuilder().setId(selectedGroup).build()
                            scope.launch(Dispatchers.IO) {
                                runCatching {
                                    stub.getSchedule(groupId)
                                }.apply {
                                    erroring = isFailure

                                    exceptionOrNull()?.printStackTrace()

                                    getOrNull()?.let { result ->
                                        val overrides = runCatching {
                                            stub.getOverrides(groupId)
                                        }.let {
                                            it.exceptionOrNull()?.printStackTrace()

                                            it.getOrNull()
                                        }

                                        context.settingsDataStore.updateData {
                                            it.toBuilder()
                                                .setSchedule(result)
                                                .setGroupName(groups.first { group -> group.id == selectedGroup }.name)
                                                .let { builder ->
                                                    if (overrides != null)
                                                        builder.setOverrides(overrides)
                                                    else builder
                                                }
                                                .build()
                                        }
                                    }
                                }
                            }.invokeOnCompletion {
                                continueLoading = false

                                runOnUiThread {
                                    if (!erroring) goToSchedule()
                                }
                            }
                        }

                        UserType.Teacher -> {
                            val teacher = Teacher.newBuilder().setName(selectedTeacher).build()
                            scope.launch(Dispatchers.IO) {
                                runCatching {
                                    stub.getTeacherSchedule(teacher)
                                }.apply {
                                    erroring = isFailure

                                    exceptionOrNull()?.printStackTrace()

                                    getOrNull()?.let { result ->
                                        context.settingsDataStore.updateData {
                                            it.toBuilder()
                                                .setSchedule(result)
                                                .setTeacherName(selectedTeacher)
                                                .build()
                                        }
                                    }
                                }
                            }.invokeOnCompletion {
                                continueLoading = false

                                runOnUiThread {
                                    if (!erroring) goToSchedule()
                                }
                            }
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
                                UserType.Student -> R.string.login_as_teacher
                                UserType.Teacher -> R.string.login_as_student
                            }
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardDefaults.shape)
                    .clickable {
                        loginAs =
                            if (loginAs == UserType.Teacher) UserType.Student else UserType.Teacher
                    }
            )
            Card(
                Modifier.fillMaxWidth()
            ) {
                when (loginAs) {
                    UserType.Student -> Row(
                        Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
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

                    UserType.Teacher -> {
                        TextField(
                            teacherQuery, { teacherQuery = it },
                            Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.search)) },
                            maxLines = 1,
                            leadingIcon = { Icon(R.drawable.search) }
                        )
                    }
                }
            }

            Card(
                Modifier
                    .weight(1F)
                    .fillMaxWidth()
            ) {
                when {
                    loginAs == UserType.Teacher && teachers.isNotEmpty() -> {
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

                    loginAs == UserType.Student && groups.isNotEmpty() -> {
                        Box(
                            Modifier
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                groups.sortedBy { it.name }.forEach { group ->
                                    if (course != 0 && !group.name.contains("-$course-")) return@forEach

                                    Card(
                                        { selectedGroup = group.id },
                                        Modifier
                                            .border(
                                                width = 1.dp,
                                                color = if (selectedGroup == group.id) MaterialTheme.colorScheme.inversePrimary else Color.Black,
                                                shape = RoundedCornerShape(5.dp)
                                            )
                                            .weight(1F),
                                        shape = RoundedCornerShape(5.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedGroup == group.id)
                                                MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                    ) {
                                        Box(
                                            Modifier
                                                .padding(
                                                    horizontal = if (selectedGroup == group.id) 10.dp else 13.dp,
                                                    vertical = 13.dp
                                                )
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                group.name,
                                                textAlign = TextAlign.Center,
                                                fontWeight = if (selectedGroup == group.id) FontWeight.Bold
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
                                style = MaterialTheme.typography.bodyLarge
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