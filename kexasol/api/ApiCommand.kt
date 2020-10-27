package com.badoo.kexasol.api

import com.badoo.kexasol.json.JsonExaMap
import com.squareup.moshi.JsonClass

internal sealed class ApiCommand {
    @JsonClass(generateAdapter = true)
    data class AbortQueryV1(
        val command: String = "abortQuery"
    ) : ApiCommand()

    @JsonClass(generateAdapter = true)
    data class CloseResultSetV1(
        val command: String = "closeResultSet",
        val resultSetHandles: List<Long>
    ) : ApiCommand()

    @JsonClass(generateAdapter = true)
    data class DisconnectV1(
        val command: String = "disconnect"
    ) : ApiCommand()

    @JsonClass(generateAdapter = true)
    data class ExecuteV1(
        val command: String = "execute",
        val sqlText: String
    ) : ApiCommand()

    @JsonClass(generateAdapter = true)
    data class FetchV1(
        val command: String = "fetch",
        val resultSetHandle: Long,
        val startPosition: Long,
        val numBytes: Long
    ) : ApiCommand()

    @JsonClass(generateAdapter = true)
    data class GetAttributesV1(
        val command: String = "getAttributes"
    ) : ApiCommand()

    @JsonClass(generateAdapter = true)
    data class GetHostsV1(
        val command: String = "getHosts",
        val hostIp: String
    ) : ApiCommand()

    @JsonClass(generateAdapter = true)
    data class LoginV1(
        val command: String = "login",
        val protocolVersion: Int = 1
    ) : ApiCommand()

    @JsonClass(generateAdapter = true)
    data class LoginAuthV1(
        val username: String,
        val password: String,
        val useCompression: Boolean,
        val sessionId: Long? = null,
        val clientName: String? = null,
        val driverName: String? = null,
        val clientOs: String? = null,
        val clientOsUsername: String? = null,
        val clientLanguage: String? = null,
        val clientVersion: String? = null,
        val clientRuntime: String? = null,
        val attributes: Map<String, Any>? = null
    ) : ApiCommand()

    @JsonClass(generateAdapter = true)
    data class SettAttributesV1(
        val command: String = "setAttributes",
        @JsonExaMap val attributes: Map<String, Any>
    ) : ApiCommand()
}
