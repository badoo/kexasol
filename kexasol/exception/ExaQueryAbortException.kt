package com.badoo.kexasol.exception

import com.badoo.kexasol.ExaConnection

open class ExaQueryAbortException(
    connection: ExaConnection,
    message: String?,
    cause: Throwable? = null,
    extra: Map<String, String?>? = null
) : ExaQueryException(connection, message, cause, extra)
