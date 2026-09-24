package idont.trust.atrust

import android.app.Application
import idont.trust.atrust.logging.Logger

class DistrustApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.wtf("Crash", "Uncaught exception on thread ${thread.name}", throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
        Logger.i("Application", "Distrust process started; SDK=${android.os.Build.VERSION.SDK_INT}")
    }

    override fun onLowMemory() {
        Logger.w("Application", "System reported low memory")
        super.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        Logger.d("Application", "Memory trim requested; level=$level")
        super.onTrimMemory(level)
    }
}
