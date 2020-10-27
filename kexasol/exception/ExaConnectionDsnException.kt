package com.badoo.kexasol.exception

import com.badoo.kexasol.ExaConnection

open class ExaConnectionDsnException(
    connection: ExaConnection,
    message: String?,
    cause: Throwable? = null,
    extra: Map<String, String?>? = null
) : ExaConnectionException(connection, message, cause, extra)
