package dev.filipfan.appfunctionspilot.agent

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import java.util.concurrent.CountDownLatch

/**
 * An [Instrumentation] that adopts shell permissions and starts the [MainActivity].
 *
 * This allows the app to acquire privileged permissions like EXECUTE_APP_FUNCTIONS
 * when started via `adb shell am instrument`.
 */
class ShellIdentityInstrumentation : Instrumentation() {

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        // Start the instrumentation thread
        start()
    }

    override fun onStart() {
        super.onStart()

        // Adopt shell permissions to acquire privileged permissions
        uiAutomation.adoptShellPermissionIdentity(
            "android.permission.EXECUTE_APP_FUNCTIONS",
        )

        // Start the MainActivity
        val intent = Intent.makeMainActivity(
            ComponentName(
                targetContext,
                MainActivity::class.java,
            ),
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        targetContext.startActivity(intent)

        // Keep the instrumentation process alive to maintain the adopted permissions
        try {
            CountDownLatch(1).await()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
