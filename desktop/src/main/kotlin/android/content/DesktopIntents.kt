package android.content

import android.net.Uri
import android.os.BatteryManager
import android.util.Log
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * Desktop dispatcher for Intents. Real macOS equivalents where they exist:
 *  - ACTION_VIEW on a URL/file      -> `open <url>` (default browser / app)
 *  - ACTION_SEND (via chooser)      -> EXTRA_TEXT copied to the clipboard; file:// EXTRA_STREAM revealed in Finder
 *  - ACTION_MAIN for our package    -> relaunch this JVM (after the current process exits)
 *  - broadcasts                     -> delivered in-process to receivers registered via registerReceiver
 *  - sticky ACTION_BATTERY_CHANGED  -> synthesized from `pmset -g batt`
 * Anything else is logged and dropped (Android would throw ActivityNotFoundException).
 */
internal object DesktopIntents {
    private const val TAG = "DesktopIntents"
    private val receivers = CopyOnWriteArrayList<Pair<BroadcastReceiver, IntentFilter>>()
    private val broadcastThread = Executors.newSingleThreadExecutor { r -> Thread(r, "broadcasts").apply { isDaemon = true } }

    fun startActivity(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_CHOOSER -> intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.let { startActivity(context, it) }
            Intent.ACTION_VIEW -> intent.data?.let { open(it.toString()) } ?: unhandled(intent)
            Intent.ACTION_SEND -> share(intent)
            Intent.ACTION_MAIN ->
                if ((intent.`package` ?: intent.component?.packageName) == context.packageName) relaunch() else unhandled(intent)
            else -> unhandled(intent)
        }
    }

    private fun unhandled(intent: Intent) = Log.w(TAG, "No desktop handler for $intent")

    private fun open(target: String) {
        runCatching { ProcessBuilder("open", target).inheritIO().start() }
            .onFailure { Log.e(TAG, "open $target failed", it) }
    }

    private fun share(intent: Intent) {
        intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.let { text ->
            ClipboardManager.INSTANCE.setPrimaryClip(ClipData.newPlainText(null, text))
            Log.i(TAG, "Share: copied to clipboard: $text")
        }
        val stream = intent.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM) as? Uri ?: return
        val path = stream.path
        if (stream.scheme == "file" && path != null && File(path).exists()) {
            runCatching { ProcessBuilder("open", "-R", path).start() }
                .onFailure { Log.e(TAG, "reveal $path failed", it) }
        } else {
            Log.w(TAG, "Share: cannot hand $stream to another app on desktop")
        }
    }

    /** Starts a fresh copy of this JVM once the current process has exited (Android: activity relaunch). */
    private fun relaunch() {
        val info = ProcessHandle.current().info()
        val cmd = info.command().orElse(null)
        val args = info.arguments().orElse(null)
        if (cmd == null || args == null) {
            Log.w(TAG, "Relaunch unavailable: JVM command line not visible")
            return
        }
        val pid = ProcessHandle.current().pid()
        val script = "while kill -0 $pid 2>/dev/null; do sleep 0.2; done; exec \"\$@\""
        runCatching { ProcessBuilder(listOf("/bin/sh", "-c", script, "sh", cmd) + args).inheritIO().start() }
            .onFailure { Log.e(TAG, "relaunch failed", it) }
    }

    fun sendBroadcast(context: Context, intent: Intent) {
        receivers.filter { it.second.hasAction(intent.action) }.forEach { (r, _) ->
            broadcastThread.execute { runCatching { r.onReceive(context, intent) }.onFailure { Log.e(TAG, "receiver failed", it) } }
        }
    }

    fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter): Intent? {
        if (receiver != null) receivers += receiver to filter
        return if (filter.hasAction(Intent.ACTION_BATTERY_CHANGED)) batteryIntent() else null
    }

    fun unregisterReceiver(receiver: BroadcastReceiver) {
        receivers.removeIf { it.first === receiver }
    }

    private fun batteryIntent(): Intent? {
        val out = runCatching {
            val p = ProcessBuilder("pmset", "-g", "batt").redirectErrorStream(true).start()
            p.inputStream.bufferedReader().readText().also { p.waitFor() }
        }.getOrNull() ?: return null
        val onAc = out.contains("'AC Power'")
        val level = Regex("""(\d+)%""").find(out)?.groupValues?.get(1)?.toIntOrNull()
        val status = when {
            level == null -> BatteryManager.BATTERY_STATUS_UNKNOWN
            Regex("""%;\s*charging""").containsMatchIn(out) -> BatteryManager.BATTERY_STATUS_CHARGING
            Regex("""%;\s*(charged|finishing charge)""").containsMatchIn(out) -> BatteryManager.BATTERY_STATUS_FULL
            onAc -> BatteryManager.BATTERY_STATUS_NOT_CHARGING
            else -> BatteryManager.BATTERY_STATUS_DISCHARGING
        }
        return Intent(Intent.ACTION_BATTERY_CHANGED)
            .putExtra(BatteryManager.EXTRA_PLUGGED, if (onAc) BatteryManager.BATTERY_PLUGGED_AC else 0)
            .putExtra(BatteryManager.EXTRA_PRESENT, level != null)
            .putExtra(BatteryManager.EXTRA_STATUS, status)
            .putExtra(BatteryManager.EXTRA_LEVEL, level ?: -1)
            .putExtra(BatteryManager.EXTRA_SCALE, 100)
    }
}
