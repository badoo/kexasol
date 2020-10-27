package com.badoo.kexasol.exception

import com.badoo.kexasol.ExaConnection

open class ExaFormatterException(
    connection: ExaConnection,
    message: String?,
    cause: Throwable? = null,
    extra: Map<String, String?>? = null
) : ExaException(connection, message, cause, extra)
