package androidx.core.content

import android.content.Intent
import java.util.ArrayList

object IntentCompat {
    @JvmStatic
    fun <T> getParcelableExtra(intent: Intent, name: String?, clazz: Class<T>): T? {
        @Suppress("DEPRECATION")
        val v = intent.extras?.get(name ?: return null) ?: return null
        return if (clazz.isInstance(v)) clazz.cast(v) else null
    }

    @JvmStatic
    fun <T> getParcelableArrayListExtra(intent: Intent, name: String?, clazz: Class<T>): ArrayList<T>? {
        @Suppress("DEPRECATION")
        val list = intent.extras?.get(name ?: return null) as? Iterable<*> ?: return null
        val out = ArrayList<T>()
        for (item in list) {
            if (item != null && clazz.isInstance(item)) {
                out.add(clazz.cast(item))
            }
        }
        return out
    }
}
