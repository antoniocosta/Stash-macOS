package com.google.android.gms.auth.api.identity

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

/**
 * Desktop shim for Google Identity Services' authorization API. Its Android OAuth client is
 * matched by package name and signing key, which a macOS app doesn't have, so — like a device
 * without Play Services — every request fails with [CommonStatusCodes.API_UNAVAILABLE].
 * (The desktop BuildConfig also sets GOOGLE_DRIVE_ENABLED = false, so the UI never offers it.)
 */
object Identity {
    @JvmStatic
    fun getAuthorizationClient(context: Context): AuthorizationClient = AuthorizationClient
}

object AuthorizationClient {
    private fun unavailable() = ApiException(Status(CommonStatusCodes.API_UNAVAILABLE, "Google Play services are not available on macOS"))

    fun authorize(request: AuthorizationRequest): Task<AuthorizationResult> = Tasks.forException(unavailable())

    fun clearToken(request: ClearTokenRequest): Task<Void?> = Tasks.forException(unavailable())

    fun getAuthorizationResultFromIntent(intent: Intent?): AuthorizationResult = throw unavailable()
}

class AuthorizationRequest private constructor() {
    enum class ResourceParameter { PICKER_OAUTH_TRIGGER, PICKER_ALLOW_MULTIPLE, PICKER_MIMETYPES, PICKER_ALLOW_FOLDER_SELECTION }

    object Prompt {
        const val CONSENT = 1
        const val SELECT_ACCOUNT = 2
    }

    class Builder internal constructor() {
        fun setRequestedScopes(scopes: List<Scope>) = this
        fun setAccount(account: Account) = this
        fun setOptOutIncludingGrantedScopes(optOut: Boolean) = this
        fun addResourceParameter(parameter: ResourceParameter, value: String) = this
        fun setPrompt(prompt: Int) = this
        fun build() = AuthorizationRequest()
    }

    companion object {
        @JvmStatic fun builder() = Builder()
    }
}

class AuthorizationResult private constructor(
    val accessToken: String?,
    val pendingIntent: PendingIntent?,
    val tokenResponseParams: Map<String, String>?,
) {
    fun hasResolution(): Boolean = pendingIntent != null
}

class ClearTokenRequest private constructor() {
    class Builder internal constructor() {
        fun setToken(token: String) = this
        fun build() = ClearTokenRequest()
    }

    companion object {
        @JvmStatic fun builder() = Builder()
    }
}
