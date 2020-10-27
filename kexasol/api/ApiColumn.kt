package com.badoo.kexasol.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ApiColumn(
    val name: String,
    val dataType: DataType
) {
    @JsonClass(generateAdapter = true)
    data class DataType(
        val type: String,
        val precision: Int?,
        val scale: Int?,
        val size: Int?,
        val characterSet: String?,
        val withLocalTimeZone: Boolean?,
        val fraction: Int?,
        val srid: Int?
    )
}
