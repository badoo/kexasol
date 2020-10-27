package com.badoo.kexasol.ws

import okio.ByteString

internal data class ExaWebSocketResponse(
    val type: ExaWebSocketResponseType,
    val text: String? = null,
    val bytes: ByteString? = null,
    val cause: Throwable? = null,
)
