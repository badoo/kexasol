package com.badoo.kexasol.exception

import com.badoo.kexasol.ExaConnection

open class ExaException(
    val connection: ExaConnection,
    message: String?,
    cause: Throwable? = null,
    val extra: Map<String, String?>? = null
) : Exception(message, cause) {

    override fun toString(): String {
        return if (connection.options.verboseException) {
            this::class.qualifiedName.orEmpty() + ":\n" + getVerboseMessage()
        } else {
            super.toString()
        }
    }

    private fun getVerboseMessage(): String {
        val data = mutableMapOf(
            "message" to super.message,
            "sessionId" to connection.sessionId,
            "dsn" to connection.options.dsn,
            "user" to connection.options.user,
            "schema" to connection.currentSchema
        )

        extra?.let {
            data.putAll(extra)
        }

        val padLength = data.keys.map { it.length }.maxOrNull()!!

        return "(\n" + data.map { (K, V) -> "    ${K.padStart(padLength)}  =>  ${V.orEmpty()}" }
            .joinToString(separator = "\n") + "\n)"
    }
}
