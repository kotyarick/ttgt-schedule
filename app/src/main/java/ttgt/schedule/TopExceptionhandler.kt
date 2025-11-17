package ttgt.schedule

import android.app.Activity
import kotlinx.coroutines.runBlocking
import ttgt.schedule.api.Client


class TopExceptionHandler(val app: Activity) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        e.printStackTrace()

        runBlocking {
            Client.sendCrashLogs("${e.message} ${e.stackTrace.joinToString("\n")}")
        }

        app.finish()
    }
}
