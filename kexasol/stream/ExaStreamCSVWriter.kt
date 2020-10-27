package com.badoo.kexasol.stream

import com.badoo.kexasol.format.ExaDateTimeFormatter
import com.univocity.parsers.csv.CsvWriter
import com.univocity.parsers.csv.CsvWriterSettings
import okio.BufferedSink
import java.io.Closeable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Convenient wrapper for writing into CSV BufferedSink initiated during streaming IMPORT.
 *
 * Please check example `02_stream.kt` for more details.
 *
 * @param[columns] List of ordered column names
 * @param[dataPipeSink] BufferedSink for CSV data
 */
class ExaStreamCSVWriter(
    private val columns: List<String>,
    private val dataPipeSink: BufferedSink
) : Closeable {
    private val writerSettings = CsvWriterSettings().apply {
        this.maxColumns = 100000
        this.maxCharsPerColumn = 2000000
    }

    private val writer = CsvWriter(dataPipeSink.outputStream(), writerSettings)

    private val columnNamesMap: Map<String, Int> = columns.withIndex().associateBy({ it.value }, { it.index })
    private val row: Array<String> = Array(columns.size) { "" }

    /**
     * Write CSV row constructed by individual set*() calls.
     *
     * For example:
     *
     * writer.setLong(0, 123L)
     * writer.setString(1, "ABC")
     * writer.writeRow()
     */
    fun writeRow() {
        writer.writeRow(row)
    }

    /**
     * Write CSV row constructed entirely from List of values.
     */
    fun writeRow(list: List<Any?>) {
        list.forEachIndexed { idx, value ->
            when (value) {
                is Boolean? -> setBoolean(idx, value)
                is Int? -> setInt(idx, value)
                is Long? -> setLong(idx, value)
                is BigDecimal? -> setBigDecimal(idx, value)
                is String? -> setString(idx, value)
                is LocalDate? -> setLocalDate(idx, value)
                is LocalDateTime? -> setLocalDateTime(idx, value)
                else -> setString(idx, value?.toString())
            }
        }

        writeRow()
    }

    /**
     * Write CSV row constructed entirely from Map of values.
     */
    fun writeRow(map: Map<String, Any?>) {
        map.forEach { (columnName, value) ->
            when (value) {
                is Boolean? -> setBoolean(columnName, value)
                is Int? -> setInt(columnName, value)
                is Long? -> setLong(columnName, value)
                is BigDecimal? -> setBigDecimal(columnName, value)
                is String? -> setString(columnName, value)
                is LocalDate? -> setLocalDate(columnName, value)
                is LocalDateTime? -> setLocalDateTime(columnName, value)
                else -> setString(columnName, value?.toString())
            }
        }

        writeRow()
    }

    override fun close() {
        writer.close()
    }

    /*
     * Setters by column index
     */

    operator fun set(idx: Int, value: String?) {
        setString(idx, value)
    }

    fun setBoolean(idx: Int, value: Boolean?) {
        row[idx] = when (value) {
            true -> "1"
            false -> "0"
            null -> ""
        }
    }

    fun setDouble(idx: Int, value: Double?) {
        setString(idx, value?.toString())
    }

    fun setInt(idx: Int, value: Int?) {
        setString(idx, value?.toString())
    }

    fun setLong(idx: Int, value: Long?) {
        setString(idx, value?.toString())
    }

    fun setBigDecimal(idx: Int, value: BigDecimal?) {
        setString(idx, value?.toPlainString())
    }

    fun setString(idx: Int, value: String?) {
        row[idx] = value ?: ""
    }

    fun setLocalDate(idx: Int, value: LocalDate?) {
        setString(idx, value?.let { ExaDateTimeFormatter.formatLocalDate(value) })
    }

    fun setLocalDateTime(idx: Int, value: LocalDateTime?) {
        setString(idx, value?.let { ExaDateTimeFormatter.formatLocalDateTime(value) })
    }

    /*
     * Setters by column name
     */

    operator fun set(columnName: String, value: String?) {
        setString(findColumn(columnName), value)
    }

    fun setBoolean(columnName: String, value: Boolean?) {
        setBoolean(findColumn(columnName), value)
    }

    fun setDouble(columnName: String, value: Double?) {
        setDouble(findColumn(columnName), value)
    }

    fun setInt(columnName: String, value: Int?) {
        setInt(findColumn(columnName), value)
    }

    fun setLong(columnName: String, value: Long?) {
        setLong(findColumn(columnName), value)
    }

    fun setBigDecimal(columnName: String, value: BigDecimal?) {
        setBigDecimal(findColumn(columnName), value)
    }

    fun setString(columnName: String, value: String?) {
        setString(findColumn(columnName), value)
    }

    fun setLocalDate(columnName: String, value: LocalDate?) {
        setLocalDate(findColumn(columnName), value)
    }

    fun setLocalDateTime(columnName: String, value: LocalDateTime?) {
        setLocalDateTime(findColumn(columnName), value)
    }

    private fun findColumn(columnName: String): Int {
        return columnNamesMap[columnName] ?: throw IllegalArgumentException("Column name [$columnName] not found")
    }
}
