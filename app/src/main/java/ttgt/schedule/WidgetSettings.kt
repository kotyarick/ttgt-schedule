package ttgt.schedule

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.util.TypedValueCompat.dpToPx
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import ttgt.schedule.ui.TimestampType
import ttgt.schedule.ui.widgets.ScheduleWidget

@Composable fun Range(
    @StringRes label: Int,
    value: String,
    slider: @Composable () -> Unit
) = Column {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelLarge
        )
        Text(value)
    }

    slider()
}

class WidgetSettings : ComponentActivity() {
    var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @SuppressLint("InflateParams")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val settings = runBlocking {
            getSetting { widgetsMap }
                ?.takeIf { it.containsKey(appWidgetId) }
                ?.getValue(appWidgetId)
        }

        setContent {
            Scaffold(Modifier.background(Color.Transparent), containerColor = Color.Transparent) { paddingValues ->
                Column(
                    Modifier.padding(paddingValues).background(Color.Transparent).fillMaxWidth(),
                    Arrangement.spacedBy(10.dp),

                ) {
                    var alpha by remember { mutableFloatStateOf(settings?.background ?: 1F) }
                    var padding by remember { mutableIntStateOf(settings?.innerPadding ?: 8) }
                    val bg = MaterialTheme.colorScheme.background

                    Row(
                        Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AndroidView<View>(
                            { context ->
                                LayoutInflater.from(context)
                                    .inflate(R.layout.schedule_widget, null, false)
                                    .apply {
                                        val lessonList: LinearLayout = findViewById(R.id.lesson_list)
                                        findViewById<TextView>(R.id.today).text = "Понедельник, первая неделя"

                                        TimestampType.Normal.timestamps.forEach { timestamp ->
                                            lessonList.addView(
                                                LayoutInflater.from(context)
                                                    .inflate(
                                                        R.layout.lesson,
                                                        lessonList,
                                                        false
                                                    )
                                                    .apply {
                                                        findViewById<TextView>(R.id.name).text = "Название пары"
                                                        findViewById<TextView>(R.id.time).text = timestamp.toString()
                                                    }
                                            )
                                        }
                                    }
                            },
                            update = { view ->
                                view.setBackgroundColor(
                                    bg.copy(alpha=alpha).toArgb()
                                )

                                val padding = dpToPx(
                                    padding.toFloat(),
                                    view.context.resources.displayMetrics
                                ).toInt()

                                view.setPadding(
                                    padding, padding,
                                    padding, padding
                                )
                            }
                        )
                    }

                    Spacer(Modifier.weight(1F))

                    Column(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(16.dp)
                            )
                            .background(bg),
                        Arrangement.spacedBy(16.dp),
                        Alignment.CenterHorizontally
                    ) {
                        Column(
                            Modifier.padding(16.dp)
                        ) {
                            Range(
                                R.string.alpha,
                                "${(alpha * 100).toInt()}%"
                            ) {
                                Slider(
                                    alpha,
                                    { alpha = it },
                                    steps = 100,
                                    colors = SliderDefaults.colors(
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent
                                    )
                                )
                            }

                            Range(
                                R.string.padding,
                                "$padding dp"
                            ) {
                                Slider(
                                    padding.toFloat(),
                                    { padding = it.toInt() },
                                    steps = 24,
                                    valueRange = 0F..24F,
                                    colors = SliderDefaults.colors(
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent
                                    )
                                )
                            }

                            Button({
                                runBlocking {
                                    println("$appWidgetId from saver")
                                    settingsDataStore.updateData {
                                        it.toBuilder()
                                            .putWidgets(
                                                appWidgetId,
                                                ttgt.schedule.proto.WidgetSettings
                                                    .newBuilder()
                                                    .setBackground(alpha)
                                                    .setInnerPadding(padding)
                                                    .build()
                                            )
                                            .build()
                                    }
                                }
                                confirm()
                            }, Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    private fun confirm() {
        println("$appWidgetId from other saver")

        ScheduleWidget().onUpdate(
            this,
            AppWidgetManager.getInstance(this),
            intArrayOf(appWidgetId)
        )

        val resultValue = Intent().putExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            appWidgetId
        )
        setResult(
            RESULT_OK,
            resultValue
        )
        finish()
    }
}