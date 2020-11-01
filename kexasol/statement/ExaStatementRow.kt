package com.badoo.kexasol.statement

import com.badoo.kexasol.format.ExaDateTimeFormatter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime


/**
 * Exasol row representation returned by [ExaStatement].
 *
 * Use common getters to retrieve individual values as specific data types.
 * Use special getters [asList] and [asMap] to retrieve the whole row at once as a collection.
 */
class ExaStatementRow(
    private val row: List<Any?>,
    private val columnNamesMap: Map<String, Int>
) {
    /*
     * Getters by column index
     */

    operator fun get(idx: Int): Any? {
        return row[idx]
    }

    fun getBoolean(idx: Int): Boolean? {
        return row[idx] as Boolean?
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
        return row[idx] as String?
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

    fun asList(): List<Any?> {
        return row
    }

    fun asMap(): Map<String, Any?> {
        return columnNamesMap.keys.zip(row).toMap()
    }

    /*
     * Non-getter functions
     */

    override fun toString(): String {
        return asMap().toString()
    }

    private fun findColumn(columnName: String): Int {
        return columnNamesMap[columnName] ?: throw IllegalArgumentException("Column name [$columnName] not found")
    }
}
