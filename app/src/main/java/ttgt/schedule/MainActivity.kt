package ttgt.schedule

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ttgt.schedule.api.profile
import ttgt.schedule.proto.Lesson
import ttgt.schedule.proto.LessonUserData
import ttgt.schedule.proto.UserData
import ttgt.schedule.ui.Destination
import ttgt.schedule.ui.ScheduleUi
import ttgt.schedule.ui.Welcome
import ttgt.schedule.ui.widgets.ScheduleWidget
import java.io.InputStream
import java.io.OutputStream

fun Lesson.isEmpty() = lessonCase in listOf(
    Lesson.LessonCase.LESSON_NOT_SET,
    Lesson.LessonCase.NOLESSON
)

val Lesson.name: String
    get() = commonLesson.name.ifBlank {
        subgroupedLesson.name
    }.ifBlank { "Нет пары" }

val Lesson.datastoreKey: String
    get() = "$name $group"

suspend fun Context.getLessonData(lesson: Lesson): LessonUserData? =
    getSetting {
        lessonDataMap
    }
        ?.takeIf { it.containsKey(lesson.datastoreKey) }
        ?.getValue(lesson.datastoreKey)

object SettingsSerializer : Serializer<UserData> {
    override val defaultValue: UserData =
        UserData.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserData {
        try {
            return UserData.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception) as Throwable
        }
    }

    override suspend fun writeTo(
        t: UserData,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.settingsDataStore: DataStore<UserData> by dataStore(
    fileName = "settings.pb",
    serializer = SettingsSerializer
)

suspend fun <T> Context.getSetting(block: UserData.() -> T): T? = settingsDataStore.data.map { it.block() }.firstOrNull()

@OptIn(ExperimentalMaterial3Api::class)
val SheetState.shouldShow: Boolean
    get() = isAnimationRunning || isVisible


@Composable
fun Icon(@DrawableRes drawable: Int) =
    Icon(drawable.vector, null)

@get:Composable
val Int.vector: ImageVector
    get() = ImageVector.vectorResource(this)

suspend fun Context.updateWidgets() {
    val widgets = getSetting {
        widgetsMap
    }?.keys ?: emptyList<Int>()

    ScheduleWidget().onUpdate(
        this,
        AppWidgetManager.getInstance(this),
        widgets.toIntArray()
    )
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler(TopExceptionHandler(this))

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()



            NavHost(
                navController = navController,
                startDestination = runBlocking {
                    val profiles = getSetting { profiles }
                    val lastUsed = getSetting { lastUsed }

                    if (
                        profiles?.profile(lastUsed) == null
                    ) Destination.Welcome
                    else Destination.Schedule
                }
            ) {
                composable<Destination.Welcome> {
                    Welcome {
                        scope.launch {
                            context.updateWidgets()
                        }

                        navController.navigate(Destination.Schedule) {
                            popUpTo(Destination.Welcome) {
                                inclusive = true
                            }
                        }
                    }
                }

                composable<Destination.Schedule> {
                    ScheduleUi {
                        navController.navigate(Destination.Welcome) {
                            popUpTo(Destination.Schedule) {
                                inclusive = true
                            }
                        }
                    }
                }
            }
        }
    }
}
