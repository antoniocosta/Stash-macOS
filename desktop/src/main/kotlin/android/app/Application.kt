package android.app

import android.content.Context

open class Application : Context() {
    override val applicationContext: Context
        get() = this

    open fun onCreate() {}
}
