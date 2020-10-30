package com.badoo.kexasol.stream

import com.badoo.kexasol.ExaConnection
import com.badoo.kexasol.enum.ExaEncryptionMode
import com.badoo.kexasol.enum.ExaStreamMode
import com.badoo.kexasol.statement.ExaStatement

internal class ExaStreamQueryThread(
    private val connection: ExaConnection,
    private val mode: ExaStreamMode,
    private val internalAddressList: List<ExaStreamInternalAddress>,
    private val columns: List<String>?,
    private val queryOrTable: String,
    private val queryParams: Map<String, Any?>?,
    private val dependentHttpThread: ExaStreamHttpThread? = null
) : Thread() {

    lateinit var statement: ExaStatement
    var lastException: Exception? = null

    override fun run() {
        try {
            statement = connection.execute(buildQuery())
        } catch (exc: Exception) {
            lastException = exc
            dependentHttpThread?.interrupt()
        }
    }

    fun buildQuery(): String {
        val queryParts: MutableList<String> = mutableListOf()

        queryParts.addAll(
            when (mode) {
                ExaStreamMode.IMPORT -> buildImportHeaderParts()
                ExaStreamMode.EXPORT -> buildExportHeaderParts()
            }
        )

        queryParts.addAll(buildFileParts())

        queryParts.addAll(
            when (mode) {
                ExaStreamMode.IMPORT -> buildImportParameterParts()
                ExaStreamMode.EXPORT -> buildExportParameterParts()
            }
        )

        return queryParts.joinToString(separator = "\n")
    }

    private fun buildImportHeaderParts(): List<String> {
        return listOf(
            "IMPORT INTO ${connection.queryFormatter.formatIdentifier(queryOrTable)}${buildColumnsList()} FROM CSV"
        )
    }

    private fun buildExportHeaderParts(): List<String> {
        val parts: MutableList<String> = mutableListOf()

        if (queryOrTable.contains(" ")) {
            parts.add(
                "EXPORT (\n${
                    connection.queryFormatter.trimAndFormatQuery(
                        queryOrTable,
                        queryParams
                    )
                }\n) INTO CSV"
            )
        } else {
            parts.add("EXPORT ${connection.queryFormatter.formatIdentifier(queryOrTable)}${buildColumnsList()} INTO CSV")
        }

        return parts
    }

    private fun buildFileParts(): List<String> {
        val fileExt = when (connection.options.compression) {
            true -> "csv.gz"
            false -> "csv"
        }

        val protocolPrefix = when (connection.options.encryption) {
            ExaEncryptionMode.DISABLED -> "http://"
            else -> "https://"
        }

        return internalAddressList.mapIndexed { idx, addr ->
            "AT '${protocolPrefix}${addr}' FILE '${idx.toString().padStart(3, '0')}.${fileExt}'"
        }
    }

    private fun buildImportParameterParts(): List<String> {
        return listOf()
    }

    private fun buildExportParameterParts(): List<String> {
        return listOf(
            "WITH COLUMN NAMES"
        )
    }

    private fun buildColumnsList(): String {
        return columns?.let {
            columns.joinToString(
                separator = ",",
                prefix = "(",
                postfix = ")"
            ) { connection.queryFormatter.formatIdentifier(it) }
        } ?: ""
    }

    fun joinWithException() {
        super.join()

        lastException?.let {
            throw it
        }
    }
}
