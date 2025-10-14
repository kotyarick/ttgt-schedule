package ttgt.schedule.ui

import androidx.annotation.UiThread
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.protobuf.Empty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ttgt.schedule.R
import ttgt.schedule.proto.Group
import ttgt.schedule.proto.GroupId
import ttgt.schedule.settingsDataStore
import ttgt.schedule.stub
import ttgt.schedule.ui.theme.ScheduleTheme


private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

fun runOnUiThread(block: suspend () -> Unit) = uiScope.launch { block() }

@Composable
fun Welcome(goToSchedule: () -> Unit) = ScheduleTheme {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val groups = remember { mutableStateListOf<Group>() }
    var isError by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf("") }
    var changeToRefresh by remember { mutableStateOf(false) }
    var continueLoading by remember { mutableStateOf(false) }

    LaunchedEffect(changeToRefresh) {
        scope.launch(Dispatchers.IO) {
            try {
                groups.addAll(stub.getGroups(Empty.newBuilder().build()).groupsList)
            } catch (error: Throwable) {
                error.printStackTrace()
                isError = true
            }
        }
    }

    if (selectedGroup.isNotBlank()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(25.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            ExtendedFloatingActionButton({
                if (continueLoading) return@ExtendedFloatingActionButton

                continueLoading = true
                var erroring = false
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
                                    .setGroupName(groups.first { it.id == selectedGroup }.name)
                                    .let {
                                        if (overrides != null)
                                            it.setOverrides(overrides)
                                        else it
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
            }) {
                if (continueLoading) {
                    Text(stringResource(R.string.loading))
                    Spacer(Modifier.width(10.dp))
                    CircularProgressIndicator(Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.continu))
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, null)
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(25.dp),
        contentAlignment = Alignment.Center
    ) {
        var course by remember { mutableIntStateOf(0) }

        Column {
            Text(
                stringResource(R.string.group_selection),
                style = MaterialTheme.typography.displaySmall
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.course_selection),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.width(15.dp))
                Row(
                    Modifier.weight(1F),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
            }

            Card(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {
                when {
                    !groups.isEmpty() -> {
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
                                            ),
                                        shape = RoundedCornerShape(5.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedGroup == group.id)
                                                MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                    ) {
                                        Box(
                                            Modifier.padding(13.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                group.name,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Normal
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
                                Icons.Default.SignalWifiStatusbarConnectedNoInternet4,
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
        }
    }
}