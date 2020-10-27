package com.badoo.kexasol.stream

import com.badoo.kexasol.format.ExaDateTimeFormatter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime


/**
 * Exasol row representation returned by [ExaStreamCSVReader]
 */
class ExaStreamExportRow(
    private val row: Array<String?>,
    private val columnNamesMap: Map<String, Int>
) {
    /*
     * Getters by column index
     */

    operator fun get(idx: Int): String? {
        return getString(idx)
    }

    fun getBoolean(idx: Int): Boolean? {
        return when (row[idx]) {
            "1" -> true
            "0" -> false
            "" -> null
            else -> throw IllegalArgumentException("Column value [${row[idx]}] cannot be represented as Boolean")
        }
    }

    fun getDouble(idx: Int): Double? {
        return getString(idx)?.toDouble()
    }

    fun getInt(idx: Int): Int? {
        return getString(idx)?.toInt()
    }

    fun getLong(idx: Int): Long? {
        return getString(idx)?.toLong()
    }

    fun getBigDecimal(idx: Int): BigDecimal? {
        return getString(idx)?.toBigDecimal()
    }

    fun getString(idx: Int): String? {
        return row[idx]
    }

    fun getLocalDate(idx: Int): LocalDate? {
        return getString(idx)?.let { ExaDateTimeFormatter.parseLocalDate(it) }
    }

    fun getLocalDateTime(idx: Int): LocalDateTime? {
        return getString(idx)?.let { ExaDateTimeFormatter.parseLocalDateTime(it) }
    }

    /*
     * Getters by column name
     */

    operator fun get(name: String): Any? {
        return get(findColumn(name))
    }

    fun getBoolean(name: String): Boolean? {
        return getBoolean(findColumn(name))
    }

    fun getDouble(name: String): Double? {
        return getDouble(findColumn(name))
    }

    fun getInt(name: String): Int? {
        return getInt(findColumn(name))
    }

    fun getLong(name: String): Long? {
        return getLong(findColumn(name))
    }

    fun getBigDecimal(name: String): BigDecimal? {
        return getBigDecimal(findColumn(name))
    }

    fun getString(name: String): String? {
        return getString(findColumn(name))
    }

    fun getLocalDate(name: String): LocalDate? {
        return getLocalDate(findColumn(name))
    }

    fun getLocalDateTime(name: String): LocalDateTime? {
        return getLocalDateTime(findColumn(name))
    }

    /*
     * Collection getters
     */

    fun asList(): List<String?> {
        return row.asList()
    }

    fun asMap(): Map<String, String?> {
        return columnNamesMap.keys.zip(row).toMap()
    }

    override fun toString(): String {
        return asMap().toString()
    }

    private fun findColumn(columnName: String): Int {
        return columnNamesMap[columnName] ?: throw IllegalArgumentException("Column name [$columnName] not found")
    }
}
