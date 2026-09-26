package android.util

import java.io.PrintWriter
import java.io.StringWriter

/**
 * Desktop shim for android.util.Log. Writes to stderr in logcat-like format.
 * Signatures match the Android API so upstream code compiles unmodified.
 */
object Log {
    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6
    const val ASSERT = 7

    /** Minimum level printed; override with -Dstash.log.level=2..7 */
    private val minLevel: Int = System.getProperty("stash.log.level")?.toIntOrNull() ?: DEBUG

    @JvmStatic fun v(tag: String?, msg: String): Int = println(VERBOSE, tag, msg)
    @JvmStatic fun v(tag: String?, msg: String?, tr: Throwable?): Int = println(VERBOSE, tag, msg + '\n' + getStackTraceString(tr))
    @JvmStatic fun d(tag: String?, msg: String): Int = println(DEBUG, tag, msg)
    @JvmStatic fun d(tag: String?, msg: String?, tr: Throwable?): Int = println(DEBUG, tag, msg + '\n' + getStackTraceString(tr))
    @JvmStatic fun i(tag: String?, msg: String): Int = println(INFO, tag, msg)
    @JvmStatic fun i(tag: String?, msg: String?, tr: Throwable?): Int = println(INFO, tag, msg + '\n' + getStackTraceString(tr))
    @JvmStatic fun w(tag: String?, msg: String): Int = println(WARN, tag, msg)
    @JvmStatic fun w(tag: String?, msg: String?, tr: Throwable?): Int = println(WARN, tag, msg + '\n' + getStackTraceString(tr))
    @JvmStatic fun w(tag: String?, tr: Throwable?): Int = println(WARN, tag, getStackTraceString(tr))
    @JvmStatic fun e(tag: String?, msg: String): Int = println(ERROR, tag, msg)
    @JvmStatic fun e(tag: String?, msg: String?, tr: Throwable?): Int = println(ERROR, tag, msg + '\n' + getStackTraceString(tr))
    @JvmStatic fun wtf(tag: String?, msg: String?): Int = println(ASSERT, tag, msg ?: "")
    @JvmStatic fun wtf(tag: String?, tr: Throwable?): Int = println(ASSERT, tag, getStackTraceString(tr))
    @JvmStatic fun wtf(tag: String?, msg: String?, tr: Throwable?): Int = println(ASSERT, tag, msg + '\n' + getStackTraceString(tr))

    @JvmStatic fun isLoggable(tag: String?, level: Int): Boolean = level >= minLevel

    @JvmStatic fun getStackTraceString(tr: Throwable?): String {
        if (tr == null) return ""
        val sw = StringWriter()
        tr.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    @JvmStatic fun println(priority: Int, tag: String?, msg: String): Int {
        if (priority < minLevel) return 0
        val p = when (priority) {
            VERBOSE -> 'V'; DEBUG -> 'D'; INFO -> 'I'; WARN -> 'W'; ERROR -> 'E'; else -> 'A'
        }
        val line = "$p/$tag: $msg"
        System.err.println(line)
        return line.length
    }
}
