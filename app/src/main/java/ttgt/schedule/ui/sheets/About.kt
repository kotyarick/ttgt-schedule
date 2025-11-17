package ttgt.schedule.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ttgt.schedule.Icon
import ttgt.schedule.R
import ttgt.schedule.api.Client
import ttgt.schedule.api.Update
import ttgt.schedule.shouldShow
import ttgt.schedule.ui.LoadingState
import ttgt.schedule.ui.runOnUiThread

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun About(state: SheetState) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

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
                            state.show()
                        }
                    }
                } else {
                    versionLoadingState = LoadingState.Error
                }
            }

        }
    }

    LaunchedEffect(Unit) {
        checkUpdate()
    }

    if (!state.shouldShow) return

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch { state.hide() }
        },
        sheetState = state
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