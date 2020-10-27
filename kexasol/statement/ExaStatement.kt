package com.badoo.kexasol.statement

import com.badoo.kexasol.ExaConnection
import com.badoo.kexasol.ExaConstant
import com.badoo.kexasol.api.ApiCommand
import com.badoo.kexasol.api.ApiResponse
import com.badoo.kexasol.exception.ExaException
import java.io.Closeable

/**
 * Exasol statement initiated by [ExaConnection.execute] call.
 *
 * Result set is fetched in chunks. Result set can be fetched only once and only in forward direction.
 *
 * This object closes automatically once fetching is finished. In most of use cases there is no need to close it manually.
 *
 * Please check example `01_fetch.kt` for more details.
 *
 * @param[connection] reference to connection object performing the SQL query
 * @param[query] SQL query with named and typed placeholders
 * @param[queryParams] Map of names and substitution values for SQL query placeholders
 * @param[snapshotExecution] Execute query in a snapshot mode
 */
class ExaStatement(
    val connection: ExaConnection,
    val query: String,
    val queryParams: Map<String, Any?>? = null,
    val snapshotExecution: Boolean = false,
) : Iterator<ExaStatementRow>, Closeable {
    /**
     * Ordered List of result set columns.
     * Column data types and properties are described here: Data types and properties: https://github.com/exasol/websocket-api/blob/master/docs/WebsocketAPIV1.md#data-types-type-names-and-properties
     */
    var columns: List<ExaStatementColumn> = listOf()
        private set

    /**
     * Map of result set column names, useful for efficient [findColumn]-like calls.
     */
    var columnNamesMap: Map<String, Int> = mapOf()
        private set

    /**
     * Number of rows selected or affected by SQL query.
     */
    var rowCount: Long = 0L
        private set

    /**
     * The final SQL query text which was sent to Exasol for execution after formatting.
     */
    var formattedQuery = ""
        private set

    /**
     * Execution time of SQL query in milliseconds, measured by WebSocket response time.
     */
    var executionTimeMillis = 0L
        private set

    var isClosed = false
        private set

    private var resultSetHandle: Long = 0L

    private var numRowsTotal: Long = 0L
    private var numRowsChunk: Long = 0L

    private var posTotal: Long = 0L
    private var posChunk: Long = 0L

    private var dataIterators: List<Iterator<Any?>> = listOf()

    init {
        formattedQuery = connection.queryFormatter.trimAndFormatQuery(query, queryParams)

        if (snapshotExecution) {
            formattedQuery = "${ExaConstant.SNAPSHOT_EXECUTION_PREFIX}${formattedQuery}"
        }

        val response = connection.executeCommand<ApiResponse.ExecuteV1>(
            ApiCommand.ExecuteV1(
                sqlText = formattedQuery
            )
        )

        executionTimeMillis = connection.lastCommandTimeMillis

        val r = response.responseData.results[0]

        when (r.resultType) {
            "resultSet" -> {
                columns = r.resultSet!!.columns.map {
                    ExaStatementColumn(
                        it.name, it.dataType.type, it.dataType.precision, it.dataType.scale, it.dataType.size,
                        it.dataType.characterSet, it.dataType.withLocalTimeZone, it.dataType.fraction, it.dataType.srid
                    )
                }

                columnNamesMap = columns.withIndex().associateBy({ it.value.name }, { it.index })

                //Detect duplicate column names
                if (columns.size > columnNamesMap.size) {
                    columns
                        .groupingBy { it.name }
                        .reduce { key, _, _ ->
                            throw ExaException(
                                connection,
                                "Duplicate column name in resultSet: $key"
                            )
                        }
                }

                numRowsTotal = r.resultSet.numRows
                numRowsChunk = r.resultSet.numRowsInMessage

                rowCount = numRowsTotal

                r.resultSet.resultSetHandle?.let {
                    resultSetHandle = r.resultSet.resultSetHandle
                }

                r.resultSet.data?.let {
                    dataIterators = r.resultSet.data.map { it.iterator() }
                }
            }

            "rowCount" -> {
                rowCount = r.rowCount!!
            }

            else -> throw ExaException(connection, "Unknown resultType [${r.resultType}]")
        }
    }

    private fun fetchNextChunk() {
        val response = connection.executeCommand<ApiResponse.FetchV1>(
            ApiCommand.FetchV1(
                resultSetHandle = resultSetHandle,
                startPosition = posTotal,
                numBytes = ExaConstant.DEFAULT_FETCH_SIZE,
            )
        )

        response.responseData.data?.let {
            dataIterators = response.responseData.data.map { it.iterator() }
        }

        numRowsChunk = response.responseData.numRows
        posChunk = 0L
    }

    /**
     * Return single [ExaStatementRow] row for data sets with exactly one row.
     *
     * It helps to avoid unnecessary iteration.
     *
     * @return ExaStatementRow
     */
    fun onlyRow(): ExaStatementRow {
        if (numRowsTotal == 1L && posTotal == 0L) {
            return next()
        }

        throw ExaException(
            connection,
            "onlyRow expects resultSet with exactly 1 row, and onlyRow should be called only once",
            extra = mapOf(
                "rowCount" to numRowsTotal.toString(),
                "position" to posTotal.toString()
            )
        )
    }

    /**
     * Find column index by column name. Column names in Exasol are normally upper cased.
     *
     * @param[columnName] Name of the column
     * @return Int
     */
    fun findColumn(columnName: String): Int {
        return columnNamesMap[columnName] ?: throw IllegalArgumentException("Column name [$columnName] not found")
    }

    override fun hasNext(): Boolean {
        val hasNext = posTotal < numRowsTotal

        if (!hasNext) {
            close()
        }

        return hasNext
    }

    override fun next(): ExaStatementRow {
        if (posChunk >= numRowsChunk) {
            fetchNextChunk()
        }

        posTotal++
        posChunk++

        return ExaStatementRow(dataIterators.map { it.next() }, columnNamesMap)
    }

    /**
     * Release data set handle on server side.
     *
     * Statements are closed automatically once the iteration is finished.
     *
     * You do not need to call close() for statements explicitly in the most of cases.
     */
    override fun close() {
        if (!isClosed) {
            numRowsTotal = 0L
            numRowsChunk = 0L

            posTotal = 0L
            posChunk = 0L

            dataIterators = listOf()

            if (resultSetHandle > 0L) {
                this.connection.executeCommand<ApiResponse.CloseResultSetV1>(
                    ApiCommand.CloseResultSetV1(
                        resultSetHandles = listOf(resultSetHandle)
                    )
                )

                resultSetHandle = 0L
            }

            isClosed = true
        }
    }
}
