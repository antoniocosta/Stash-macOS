package android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window

/**
 * Desktop shim for android.app.Activity. The macOS app has one main window, represented by
 * [DesktopActivity] / `MainActivity`; its [window] holds the bar colours upstream sets.
 */
open class Activity : Context() {
    open val window: Window = Window()

    private var currentIntent: Intent = Intent()

    open val intent: Intent
        get() = currentIntent

    open fun setIntent(newIntent: Intent) {
        currentIntent = newIntent
    }

    open fun onCreate(savedInstanceState: Bundle?) {}

    open fun onNewIntent(intent: Intent) {
        currentIntent = intent
    }

    open fun onDestroy() {}

    open fun finish() {}
}

/** The single desktop "activity" backing LocalContext/LocalView in the Compose window. */
object DesktopActivity : Activity()
