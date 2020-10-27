package com.badoo.kexasol.statement

/**
 * Data types and properties: https://github.com/exasol/websocket-api/blob/master/docs/WebsocketAPIV1.md#data-types-type-names-and-properties
 */
data class ExaStatementColumn(
    val name: String,
    val type: String,
    val precision: Int?,
    val scale: Int?,
    val size: Int?,
    val characterSet: String?,
    val withLocalTimeZone: Boolean?,
    val fraction: Int?,
    val srid: Int?
)
