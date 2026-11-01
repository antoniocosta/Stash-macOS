package android.app

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Window

/**
 * Desktop shim for android.app.Activity. The macOS app has one main window, represented by
 * [DesktopActivity] / `MainActivity`; its [window] holds the bar colours upstream sets.
 *
 * Like Android, an activity's base context is attached before [onCreate] (`attachBaseContext`,
 * done by `Main.kt` via [attach]) and may carry an override configuration
 * ([applyOverrideConfiguration]), which its [resources] — and the Compose window it hosts — see.
 */
open class Activity : ContextWrapper(null) {
    open val window: Window = Window()

    private var currentIntent: Intent = Intent()
    private var override: Configuration? = null

    open val intent: Intent
        get() = currentIntent

    open fun setIntent(newIntent: Intent) {
        currentIntent = newIntent
    }

    /** Desktop: `ActivityThread.performLaunchActivity`'s `attach` step. */
    fun attach(base: Context) = attachBaseContext(base)

    internal override val overrideConfiguration: Configuration?
        get() = override?.let { o -> Configuration().apply { super.overrideConfiguration?.let(::updateFrom); updateFrom(o) } }
            ?: super.overrideConfiguration

    open fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
        check(override == null) { "Override configuration has already been set" }
        override = Configuration(overrideConfiguration)
    }

    open fun onCreate(savedInstanceState: Bundle?) {}

    open fun onNewIntent(intent: Intent) {
        currentIntent = intent
    }

    open fun onResume() {}

    open fun onPause() {}

    open fun onDestroy() {}

    open fun finish() {}

    /** Set by the host while it replaces this instance ([recreate]), as Android does around a config change. */
    internal var changingConfigurations: Boolean = false

    open val isChangingConfigurations: Boolean get() = changingConfigurations

    /** Desktop: the host (`Main.kt`) builds a fresh instance of this activity in place of this one. */
    open fun recreate() {
        DesktopActivityHost.recreate(this)
    }

    open fun overridePendingTransition(enterAnim: Int, exitAnim: Int) {}

    companion object {
        const val RESULT_OK = -1
        const val RESULT_CANCELED = 0
    }
}

/** The single desktop "activity" backing LocalContext/LocalView in the Compose window. */
object DesktopActivity : Activity()

/** Lets `Main.kt` replace the window's activity when one asks to be recreated. */
object DesktopActivityHost {
    @Volatile var onRecreate: ((Activity) -> Unit)? = null

    /** The activity the main window currently shows. */
    @Volatile var current: Activity? = null

    fun recreate(activity: Activity) {
        onRecreate?.invoke(activity)
    }
}
