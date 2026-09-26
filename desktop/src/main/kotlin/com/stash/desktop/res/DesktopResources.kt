package com.stash.desktop.res

import java.io.InputStream
import java.util.Properties

/**
 * Runtime side of the generated Android resources (see `generateAndroidResources` in
 * desktop/build.gradle.kts): maps an upstream `R.*` id to its classpath file or string value.
 */
object DesktopResources {
    private val index: Properties by lazy {
        Properties().apply {
            val stream = DesktopResources::class.java.classLoader.getResourceAsStream("stash-res/index.properties")
                ?: error("stash-res/index.properties missing from classpath")
            stream.use { load(it) }
        }
    }

    private fun entry(id: Int): String =
        index.getProperty(Integer.toHexString(id))
            ?: throw IllegalArgumentException("Resource ID #0x${Integer.toHexString(id)} not found") // Resources.NotFoundException analogue

    /** Classpath path of a file resource (font/drawable/mipmap/raw). */
    fun path(id: Int): String = entry(id).also { require(it.startsWith("file:")) { "0x${Integer.toHexString(id)} is not a file resource" } }.removePrefix("file:")

    fun open(id: Int): InputStream =
        DesktopResources::class.java.classLoader.getResourceAsStream(path(id)) ?: error("Missing resource file ${path(id)}")

    fun bytes(id: Int): ByteArray = open(id).use { it.readBytes() }

    fun string(id: Int): String = entry(id).also { require(it.startsWith("string:")) { "0x${Integer.toHexString(id)} is not a string resource" } }.removePrefix("string:")
}
