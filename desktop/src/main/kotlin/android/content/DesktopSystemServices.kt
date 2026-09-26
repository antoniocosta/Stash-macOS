package android.content

import android.app.ActivityManager
import android.app.NotificationManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Desktop registry behind [Context.getSystemService]. Services are created lazily
 * once per process. Shims owned elsewhere (e.g. `android.media.AudioManager`) plug
 * in with [register], e.g. `DesktopSystemServices.register(Context.AUDIO_SERVICE) { AudioManager(...) }`.
 * Unknown names return null, exactly like Android for an unsupported service.
 */
object DesktopSystemServices {
    private val factories = ConcurrentHashMap<String, () -> Any>()
    private val instances = ConcurrentHashMap<String, Any>()

    init {
        register(Context.NOTIFICATION_SERVICE) { NotificationManager.INSTANCE }
        register(Context.CONNECTIVITY_SERVICE) { ConnectivityManager.INSTANCE }
        register(Context.BATTERY_SERVICE) { BatteryManager.INSTANCE }
        register(Context.ACTIVITY_SERVICE) { ActivityManager.INSTANCE }
        register(Context.CLIPBOARD_SERVICE) { ClipboardManager.INSTANCE }
        register(Context.AUDIO_SERVICE) { android.media.AudioManager.INSTANCE }
    }

    fun register(name: String, factory: () -> Any) {
        factories[name] = factory
        instances.remove(name)
    }

    fun get(name: String): Any? {
        instances[name]?.let { return it }
        val factory = factories[name] ?: return null
        return instances.computeIfAbsent(name) { factory() }
    }

    fun <T> get(serviceClass: Class<T>): T? =
        factories.keys.asSequence().mapNotNull { get(it) }.firstOrNull { serviceClass.isInstance(it) }
            ?.let(serviceClass::cast)
}
