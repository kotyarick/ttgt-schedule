package ttgt.schedule

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.google.protobuf.Empty
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ttgt.schedule.proto.Lesson
import ttgt.schedule.proto.Overrides
import ttgt.schedule.proto.ServerGrpc
import ttgt.schedule.proto.UserData
import ttgt.schedule.ui.Destination
import ttgt.schedule.ui.ScheduleUi
import ttgt.schedule.ui.Welcome
import ttgt.schedule.ui.widget.TimeRemainWidget
import java.io.InputStream
import java.io.OutputStream

val empty = Empty.newBuilder().build()
val noLesson = Lesson.newBuilder().setNoLesson(empty).build()

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

lateinit var stub: ServerGrpc.ServerBlockingStub

@Composable
fun Icon(@DrawableRes drawable: Int) =
    Icon(drawable.vector, null)

@get:Composable
val Int.vector: ImageVector
    get() = ImageVector.vectorResource(this)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            stub = ServerGrpc.newBlockingStub(createGrpcChannel(context))

            NavHost(
                navController = navController,
                startDestination = runBlocking {
                    if (settingsDataStore.data.map { it.hasSchedule() }.firstOrNull() == true)
                        Destination.Schedule else Destination.Welcome
                }
            ) {
                composable<Destination.Welcome> {
                    Welcome {
                        scope.launch {
                            context.settingsDataStore.updateData {
                                it.toBuilder()
                                    .setOverrides(Overrides.newBuilder().build())
                                    .build()
                            }

                            val widgets = context.settingsDataStore.data.map {
                                it.widgetsMap
                            }.firstOrNull()?.keys ?: emptyList<Int>()

                            TimeRemainWidget().onUpdate(
                                this@MainActivity,
                                AppWidgetManager.getInstance(this@MainActivity),
                                widgets.toIntArray()
                            )
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
