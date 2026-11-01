package com.stash.desktop.res

import java.io.InputStream
import java.util.Locale
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/**
 * Runtime side of the generated Android resources (see `generateAndroidResources` in
 * desktop/build.gradle.kts): maps an upstream `R.*` id to its classpath file or string value.
 *
 * Strings and plurals resolve against [Locale.getDefault] the way Android's resource table does:
 * `values-<lang>-r<REGION>`, then `values-<lang>`, then the unqualified `values`.
 */
object DesktopResources {
    private val loader = DesktopResources::class.java.classLoader

    private val index: Properties by lazy {
        Properties().apply {
            val stream = loader.getResourceAsStream("stash-res/index.properties")
                ?: error("stash-res/index.properties missing from classpath")
            stream.use { load(it) }
        }
    }

    /** BCP-47 tags of the shipped translations (`values-de-rDE` -> `de-DE`). */
    val locales: List<String> by lazy {
        loader.getResourceAsStream("stash-res/locales.txt")?.use { it.reader().readLines() }.orEmpty().filter { it.isNotBlank() }
    }

    private val translations = ConcurrentHashMap<String, Properties>()

    private fun translation(tag: String): Properties? =
        if (tag !in locales) null else translations.getOrPut(tag) {
            Properties().apply { loader.getResourceAsStream("stash-res/strings_$tag.properties")?.use { load(it) } }
        }

    /** The translation tables consulted for [locale], most specific first. */
    private fun chain(locale: Locale): List<Properties> {
        val lang = locale.language.let { if (it == "tl") "fil" else it }
        return listOfNotNull(
            locale.country.takeIf { it.isNotEmpty() }?.let { translation("$lang-$it") },
            translation(lang),
            locales.firstOrNull { it.substringBefore('-') == lang }?.let { translation(it) },
        ).distinct()
    }

    private fun entry(id: Int): String =
        index.getProperty(Integer.toHexString(id))
            ?: throw IllegalArgumentException("Resource ID #0x${Integer.toHexString(id)} not found") // Resources.NotFoundException analogue

    /** Classpath path of a file resource (font/drawable/mipmap/raw/xml). */
    fun path(id: Int): String = entry(id).also { require(it.startsWith("file:")) { "0x${Integer.toHexString(id)} is not a file resource" } }.removePrefix("file:")

    fun open(id: Int): InputStream =
        loader.getResourceAsStream(path(id)) ?: error("Missing resource file ${path(id)}")

    fun bytes(id: Int): ByteArray = open(id).use { it.readBytes() }

    fun string(id: Int, locale: Locale = Locale.getDefault()): String {
        val default = entry(id).also { require(it.startsWith("string:")) { "0x${Integer.toHexString(id)} is not a string resource" } }
        val key = Integer.toHexString(id)
        return chain(locale).firstNotNullOfOrNull { it.getProperty(key) } ?: default.removePrefix("string:")
    }

    /** Android's `Resources.getQuantityString(id, quantity)`: the CLDR plural category of [quantity] for [locale]. */
    fun quantityString(id: Int, quantity: Int, locale: Locale = Locale.getDefault()): String {
        require(entry(id) == "plurals:") { "0x${Integer.toHexString(id)} is not a plurals resource" }
        val key = Integer.toHexString(id)
        val translated = chain(locale)
        val category = pluralCategory(locale.language, quantity)
        for (table in translated + index) {
            // A locale's table is complete for its own categories; fall back to "other" within it.
            val hit = table.getProperty("$key.$category") ?: table.getProperty("$key.other")
            if (hit != null) return hit
        }
        error("plurals 0x$key has no items")
    }

    /** CLDR cardinal plural rules (integer operands) for the locales Stash ships. */
    fun pluralCategory(language: String, n: Int): String {
        val abs = kotlin.math.abs(n)
        val mod10 = abs % 10
        val mod100 = abs % 100
        return when (language) {
            "ko", "zh", "ja" -> "other"
            "fr", "pt" -> if (abs == 0 || abs == 1) "one" else "other"
            "hi", "fa" -> if (abs == 0 || abs == 1) "one" else "other"
            "fil", "tl" -> if (abs == 1 || abs == 2 || abs == 3 || mod10 !in setOf(4, 6, 9)) "one" else "other"
            "ru", "uk" -> when {
                mod10 == 1 && mod100 != 11 -> "one"
                mod10 in 2..4 && mod100 !in 12..14 -> "few"
                else -> "many"
            }
            "pl" -> when {
                abs == 1 -> "one"
                mod10 in 2..4 && mod100 !in 12..14 -> "few"
                else -> "many"
            }
            "ar" -> when {
                abs == 0 -> "zero"
                abs == 1 -> "one"
                abs == 2 -> "two"
                mod100 in 3..10 -> "few"
                mod100 in 11..99 -> "many"
                else -> "other"
            }
            else -> if (abs == 1) "one" else "other" // en, de, es, mr, ...
        }
    }
}
