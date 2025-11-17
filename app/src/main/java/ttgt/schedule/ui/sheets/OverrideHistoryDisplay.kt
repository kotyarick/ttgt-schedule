package ttgt.schedule.ui.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.launch
import ttgt.schedule.Icon
import ttgt.schedule.R
import ttgt.schedule.display
import ttgt.schedule.name
import ttgt.schedule.proto.OverrideHistoryElement
import ttgt.schedule.settingsDataStore
import ttgt.schedule.shouldShow
import ttgt.schedule.sortString
import ttgt.schedule.vector

private val inlineContent = mapOf(
    "arrow" to InlineTextContent(
        Placeholder(
            width = 16.sp,
            height = 16.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
        )
    ) {
        Icon(R.drawable.arrow.vector, "", Modifier.rotate(180F))
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverrideHistoryDisplay(
    state: SheetState,
    history: List<OverrideHistoryElement>,
    isTeacher: Boolean,
    onHistoryDelete: () -> Unit
) {
    if (!state.shouldShow) return

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var deletionConfirmation by remember { mutableStateOf(false) }

    if (deletionConfirmation) {
        AlertDialog(
            { deletionConfirmation = false },
            {
                TextButton({
                    scope.launch {
                        context.settingsDataStore.updateData { it.toBuilder().clearOverrideHistory().build() }
                        deletionConfirmation = false
                        state.hide()
                        onHistoryDelete()
                    }
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton({ deletionConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                Text(stringResource(R.string.delete_history_alert))
            }
        )
    }

    ModalBottomSheet(
        {
            scope.launch { state.hide() }
        },
        sheetState = state,
        dragHandle = {
            TopAppBar(
                {
                    Text(stringResource(R.string.overrides_history))
                },
                navigationIcon = {
                    IconButton({ scope.launch { state.hide() } }) {
                        Icon(R.drawable.arrow)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) {
        Column {
            LazyColumn {
                items(history.size) { index ->
                    val element = history[index]

                    Spacer(Modifier.height(24.dp))

                    Text(
                        element.overrides.date.display(),
                        Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.labelLarge
                    )

                    val overrides = element.overrides.overridesList.toMutableList()
                    overrides.sortBy { it.index }

                    overrides.forEach { override ->
                        ListItem(
                            headlineContent = {
                                Text(buildAnnotatedString {
                                    withStyle(SpanStyle(
                                        textDecoration = TextDecoration.LineThrough,
                                        fontWeight = FontWeight.Bold
                                    )) {
                                        append(override.shouldBe.name)
                                    }

                                    override
                                        .shouldBe
                                        .let {
                                            if (isTeacher) it.group else null
                                        }
                                        ?.let {
                                            withStyle(SpanStyle(
                                                textDecoration = TextDecoration.LineThrough
                                            )) {
                                                append(" ", it)
                                            }
                                        }

                                    appendInlineContent("arrow", "[icon]")

                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(override.willBe.name)
                                    }
                                    override
                                        .willBe
                                        .let {
                                            if (isTeacher) it.group else null
                                        }
                                        ?.let { append(" ", it) }
                                }, inlineContent = inlineContent)
                            },
                            leadingContent = {
                                Text((override.index + 1).toString())
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
            Row(Modifier.padding(16.dp).fillMaxWidth()) {
                Button(
                    { deletionConfirmation = true }, Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.clearHistory))
                }
            }
        }
    }
}