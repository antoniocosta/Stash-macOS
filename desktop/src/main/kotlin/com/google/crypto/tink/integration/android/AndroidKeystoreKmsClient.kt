package com.google.crypto.tink.integration.android

import com.google.crypto.tink.Aead
import java.security.GeneralSecurityException

/**
 * Desktop stand-in for Tink's AndroidKeystoreKmsClient. Android Keystore master keys don't exist on
 * macOS (the desktop AndroidKeysetManager keeps the keyset in the login Keychain instead), so there
 * is never a key to open or delete; upstream only reaches this after the Keystore lookup succeeded.
 */
class AndroidKeystoreKmsClient {
    @Throws(GeneralSecurityException::class)
    fun getAead(keyUri: String): Aead = throw GeneralSecurityException("Android Keystore is not available on macOS")

    @Throws(GeneralSecurityException::class)
    fun deleteKey(keyUri: String) {}
}
