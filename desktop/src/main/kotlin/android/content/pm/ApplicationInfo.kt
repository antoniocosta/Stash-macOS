package android.content.pm

import android.content.Context
import java.io.File

/**
 * Desktop shim for android.content.pm.ApplicationInfo. `nativeLibraryDir` is
 * `~/Library/Application Support/Stash/native_libs` (where the desktop build places the
 * helper binaries upstream looks up, e.g. libffmpeg.so / libqjs.so); FLAG_DEBUGGABLE
 * follows `-Dstash.debug=true`, like the BuildConfig shim.
 */
class ApplicationInfo {
    @JvmField var packageName: String = Context.PACKAGE_NAME
    @JvmField var flags: Int = 0
    @JvmField var nativeLibraryDir: String = ""
    @JvmField var dataDir: String = ""

    companion object {
        const val FLAG_DEBUGGABLE = 1 shl 1

        internal fun forApp(context: Context): ApplicationInfo = ApplicationInfo().apply {
            packageName = context.packageName
            val root = context.noBackupFilesDir.parentFile
            dataDir = root.absolutePath
            nativeLibraryDir = File(root, "native_libs").absolutePath
            if (System.getProperty("stash.debug") == "true") flags = flags or FLAG_DEBUGGABLE
        }
    }
}
