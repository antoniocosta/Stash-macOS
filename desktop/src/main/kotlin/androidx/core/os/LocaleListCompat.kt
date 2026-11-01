package androidx.core.os

import android.os.LocaleList
import java.util.Locale

/** Desktop shim for androidx.core.os.LocaleListCompat, wrapping [LocaleList]. */
class LocaleListCompat private constructor(private val list: LocaleList) {
    operator fun get(index: Int): Locale? = list[index]
    fun size(): Int = list.size()
    val isEmpty: Boolean get() = list.isEmpty
    fun toLanguageTags(): String = list.toLanguageTags()
    fun unwrap(): Any = list

    override fun equals(other: Any?): Boolean = other is LocaleListCompat && other.list == list
    override fun hashCode(): Int = list.hashCode()
    override fun toString(): String = list.toString()

    companion object {
        @JvmStatic fun wrap(localeList: LocaleList): LocaleListCompat = LocaleListCompat(localeList)
        @JvmStatic fun create(vararg localeList: Locale): LocaleListCompat = LocaleListCompat(LocaleList(*localeList))
        @JvmStatic fun forLanguageTags(list: String?): LocaleListCompat = LocaleListCompat(LocaleList.forLanguageTags(list))
        @JvmStatic fun getEmptyLocaleList(): LocaleListCompat = LocaleListCompat(LocaleList.getEmptyLocaleList())
        @JvmStatic fun getDefault(): LocaleListCompat = LocaleListCompat(LocaleList.getDefault())
        @JvmStatic fun getAdjustedDefault(): LocaleListCompat = getDefault()
    }
}
