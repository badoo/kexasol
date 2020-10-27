package com.badoo.kexasol.stream

import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import okio.BufferedSource
import java.io.Closeable

/**
 * Convenient wrapper for reading from CSV BufferedSource initiated during streaming EXPORT.
 *
 * Iterate over CSV rows and output as [ExaStreamExportRow] objects.
 *
 * Please check example `02_stream.kt` for more details.
 *
 * @param[dataPipeSource] BufferedSource with CSV data
 */
class ExaStreamCSVReader(
    private val dataPipeSource: BufferedSource
) : Iterator<ExaStreamExportRow>, Closeable {
    private val parserSettings = CsvParserSettings().apply {
        this.inputBufferSize = 1024 * 1024
        this.maxColumns = 100000
        this.maxCharsPerColumn = 2000000
    }

    private val parser = CsvParser(parserSettings)
    private val columnNamesMap: Map<String, Int>
    private var nextRow: Array<String?>? = null

    init {
        parser.beginParsing(dataPipeSource.inputStream())

        columnNamesMap = parser.parseNext().withIndex().associateBy({ it.value }, { it.index })
        nextRow = parser.parseNext()
    }

    override fun hasNext(): Boolean {
        return nextRow != null
    }

    override fun next(): ExaStreamExportRow {
        val currentRow = nextRow!!
        nextRow = parser.parseNext()

        return ExaStreamExportRow(currentRow, columnNamesMap)
    }

    override fun close() {
        parser.stopParsing()
    }
}
