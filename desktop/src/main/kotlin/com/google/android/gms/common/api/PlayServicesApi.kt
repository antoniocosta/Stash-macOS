package com.google.android.gms.common.api

import android.app.PendingIntent

/** Desktop shim for Play Services' Status. */
class Status(val statusCode: Int, val statusMessage: String? = null, val resolution: PendingIntent? = null) {
    fun hasResolution(): Boolean = resolution != null
    val isSuccess: Boolean get() = statusCode <= CommonStatusCodes.SUCCESS
}

/** Desktop shim for Play Services' ApiException. */
open class ApiException(val status: Status) : Exception("${status.statusCode}: ${status.statusMessage.orEmpty()}") {
    val statusCode: Int get() = status.statusCode
}

/** Desktop shim for Play Services' Scope. */
data class Scope(val scopeUri: String)

/** Play Services' CommonStatusCodes (same values). */
object CommonStatusCodes {
    const val SUCCESS = 0
    const val SERVICE_VERSION_UPDATE_REQUIRED = 2
    const val SERVICE_DISABLED = 3
    const val SIGN_IN_REQUIRED = 4
    const val INVALID_ACCOUNT = 5
    const val RESOLUTION_REQUIRED = 6
    const val NETWORK_ERROR = 7
    const val INTERNAL_ERROR = 8
    const val DEVELOPER_ERROR = 10
    const val ERROR = 13
    const val INTERRUPTED = 14
    const val TIMEOUT = 15
    const val CANCELED = 16
    const val API_NOT_CONNECTED = 17
    const val API_UNAVAILABLE = 17 // ConnectionResult.API_UNAVAILABLE surfaces as this status
}
