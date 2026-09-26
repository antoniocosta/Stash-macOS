package com.google.crypto.tink.integration.android

import android.content.Context
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkProtoKeysetFormat
import java.util.Base64

/**
 * Desktop stand-in for Tink's AndroidKeysetManager (not in the JVM Tink jar).
 *
 * Android keeps the keyset in SharedPreferences wrapped by an Android Keystore
 * master key. On macOS the serialized keyset itself is kept in the login
 * Keychain (generic password, service "com.stash.desktop"), so it is protected
 * by the OS secure store in the same spirit.
 */
class AndroidKeysetManager private constructor(val keysetHandle: KeysetHandle) {

    class Builder {
        private var keysetName: String = "stash_keyset"
        private var template: KeyTemplate? = null

        fun withSharedPref(context: Context, keysetName: String, prefFileName: String?): Builder =
            apply { this.keysetName = if (prefFileName == null) keysetName else "$prefFileName/$keysetName" }

        fun withKeyTemplate(val_: KeyTemplate): Builder = apply { template = val_ }

        @Suppress("UNUSED_PARAMETER")
        fun withMasterKeyUri(uri: String): Builder = this

        @Synchronized
        fun build(): AndroidKeysetManager {
            Keychain.read(keysetName)?.let { stored ->
                val handle = TinkProtoKeysetFormat.parseKeyset(
                    Base64.getDecoder().decode(stored), InsecureSecretKeyAccess.get(),
                )
                return AndroidKeysetManager(handle)
            }
            val t = checkNotNull(template) { "withKeyTemplate() is required to create a new keyset" }
            val handle = KeysetHandle.generateNew(t)
            val bytes = TinkProtoKeysetFormat.serializeKeyset(handle, InsecureSecretKeyAccess.get())
            Keychain.write(keysetName, Base64.getEncoder().encodeToString(bytes))
            return AndroidKeysetManager(handle)
        }
    }

    private object Keychain {
        private const val SERVICE = "com.stash.desktop"

        fun read(account: String): String? {
            val p = ProcessBuilder("security", "find-generic-password", "-s", SERVICE, "-a", account, "-w")
                .redirectErrorStream(false).start()
            val out = p.inputStream.bufferedReader().readText().trim()
            return if (p.waitFor() == 0 && out.isNotEmpty()) out else null
        }

        fun write(account: String, secret: String) {
            val p = ProcessBuilder(
                "security", "add-generic-password", "-U", "-s", SERVICE, "-a", account, "-w", secret,
            ).start()
            check(p.waitFor() == 0) { "Keychain write failed: " + p.errorStream.bufferedReader().readText() }
        }
    }
}
