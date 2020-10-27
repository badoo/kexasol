package com.badoo.kexasol.exception

import com.badoo.kexasol.ExaConnection

open class ExaCommandAuthException(
    connection: ExaConnection,
    message: String?,
    cause: Throwable? = null,
    extra: Map<String, String?>? = null
) : ExaCommandException(connection, message, cause, extra)
