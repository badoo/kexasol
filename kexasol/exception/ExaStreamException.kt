package com.badoo.kexasol.exception

import com.badoo.kexasol.ExaConnection

class ExaStreamException(
    connection: ExaConnection,
    message: String?,
    cause: Throwable? = null,
    extra: Map<String, String?>? = null
) : ExaException(connection, message, cause, extra)
