package android.security.keystore

import java.security.InvalidKeyException
import java.security.spec.AlgorithmParameterSpec

/*
 * Desktop shims for android.security.keystore. There is no "AndroidKeyStore" JCA provider on the
 * JVM, so KeyStore / KeyPairGenerator.getInstance(..., "AndroidKeyStore") throw and upstream takes
 * its software-key fallback (sealed by Tink, whose keyset lives in the macOS Keychain).
 */

class KeyGenParameterSpec private constructor(val keystoreAlias: String, val purposes: Int) : AlgorithmParameterSpec {
    class Builder(private val keystoreAlias: String, private val purposes: Int) {
        fun setAlgorithmParameterSpec(spec: AlgorithmParameterSpec) = this
        fun setDigests(vararg digests: String) = this
        fun setBlockModes(vararg blockModes: String) = this
        fun setEncryptionPaddings(vararg paddings: String) = this
        fun setKeySize(keySize: Int) = this
        fun setUserAuthenticationRequired(required: Boolean) = this
        fun build() = KeyGenParameterSpec(keystoreAlias, purposes)
    }
}

object KeyProperties {
    const val KEY_ALGORITHM_EC = "EC"
    const val KEY_ALGORITHM_RSA = "RSA"
    const val KEY_ALGORITHM_AES = "AES"
    const val PURPOSE_ENCRYPT = 1
    const val PURPOSE_DECRYPT = 2
    const val PURPOSE_SIGN = 4
    const val PURPOSE_VERIFY = 8
    const val PURPOSE_AGREE_KEY = 64
    const val DIGEST_SHA256 = "SHA-256"
    const val BLOCK_MODE_GCM = "GCM"
    const val ENCRYPTION_PADDING_NONE = "NoPadding"
}

class KeyPermanentlyInvalidatedException(message: String? = null) : InvalidKeyException(message)
