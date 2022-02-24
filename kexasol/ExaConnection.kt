package com.badoo.kexasol

import com.badoo.kexasol.api.ApiCommand
import com.badoo.kexasol.api.ApiResponse
import com.badoo.kexasol.exception.*
import com.badoo.kexasol.format.ExaQueryFormatter
import com.badoo.kexasol.json.ExaDataJsonAdapter
import com.badoo.kexasol.json.ExaMapJsonAdapter
import com.badoo.kexasol.mode.ExaStreamMode
import com.badoo.kexasol.net.ExaDsnParser
import com.badoo.kexasol.net.ExaNodeAddress
import com.badoo.kexasol.statement.ExaStatement
import com.badoo.kexasol.stream.*
import com.badoo.kexasol.stream.ExaStreamHttpThread
import com.badoo.kexasol.stream.ExaStreamQueryThread
import com.badoo.kexasol.ws.ExaWebSocketClient
import com.squareup.moshi.Moshi
import okio.BufferedSink
import okio.BufferedSource
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import javax.crypto.Cipher
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis

/**
 * Main class holding Exasol connection.
 *
 * Connection object can be shared between threads, but only one thread can execute commands simultaneously.
 * It is advised to run all commands inside the [use] block to ensure correct connection termination.
 *
 * @param[options] Exasol connection options
 */
class ExaConnection(val options: ExaConnectionOptions) : Closeable {
    /**
     * Response data from successful login command, does not change.
     * https://github.com/exasol/websocket-api/blob/master/docs/commands/loginV1.md
     */
    var loginInfo = mapOf<String, Any>()
        private set

    /**
     * Connection attributes, may change after any command.
     * https://github.com/exasol/websocket-api/blob/master/docs/WebsocketAPIV2.md#attributes-session-and-database-properties
     */
    var attributes = mapOf<String, Any>()
        private set

    /**
     * SESSION_ID of current connection. Can be empty if connection was not established.
     */
    val sessionId: String
        get() = loginInfo["sessionId"]?.toString() ?: ""

    /**
     * Actual protocol version of current connection.
     * Learn more about protocol versions: https://github.com/exasol/websocket-api
     */
    val protocolVersion: Int
        get() = loginInfo["protocolVersion"]?.let { (it as Long).toInt() } ?: 0

    /**
     * Currently opened schema, can be empty if schema was not opened.
     * It can be changed dynamically by calling [openSchema].
     */
    val currentSchema: String
        get() = attributes["currentSchema"]?.let { it as String } ?: ""

    /**
     * Number of commands executed by the current connection. It includes not only SQL queries, but all API commands.
     */
    var commandCount = 0L
        private set

    /**
     * Map of failed connection attempts to specific Exasol nodes addresses with causes.
     * Useful for debugging of connectivity issues.
     */
    var connectFailedAttempts = mutableMapOf<ExaNodeAddress, Throwable>()
        private set

    /**
     * Amount of time spent opening WebSocket connection, including failed attempts.
     */
    var connectTimeMillis = 0L
        private set

    /**
     * Amount of time spent performing the login procedure. If it is too high, connection server might be overloaded.
     */
    var loginTimeMillis = 0L
        private set

    /**
     * Amount if time spent executing last API command.
     */
    var lastCommandTimeMillis = 0L
        private set

    var isClosed = false
        private set

    internal val moshi = Moshi.Builder()
        .add(ExaMapJsonAdapter())
        .add(ExaDataJsonAdapter())
        .build()

    internal val logger = LoggerFactory.getLogger(ExaConstant.DRIVER_NAME)
    internal val logHash = generateLogHash()

    internal val dsnParser = ExaDsnParser(this)
    internal val wsClient = ExaWebSocketClient(this)

    internal val queryFormatter = ExaQueryFormatter(this)
    internal val executeLock = ReentrantLock()

    init {
        val nodeAddressList = dsnParser.parse(options.dsn)
        var connectAttemptCount = 0

        connectTimeMillis = measureTimeMillis {
            for (nodeAddress in nodeAddressList) {
                connectAttemptCount++

                logDebug("Connection attempt $connectAttemptCount of ${nodeAddressList.size} to $nodeAddress")
                val exc = wsClient.attemptConnect(nodeAddress)

                if (exc == null) {
                    logDebug("Connection to $nodeAddress was established successfully")
                    break
                } else {
                    logDebug("Failed to connect to $nodeAddress: ${exc.message}")
                    connectFailedAttempts[nodeAddress] = exc

                    // All nodes had an exception -> connection failed with last exception text
                    if (connectAttemptCount == nodeAddressList.size) {
                        throw ExaConnectionException(
                            this, "Failed to connect to all Exasol nodes", cause = exc, extra = mapOf(
                                "failedAttempts" to connectAttemptCount.toString()
                            )
                        )
                    }
                }
            }
        }

        loginTimeMillis = measureTimeMillis {
            val responseLogin = executeCommand<ApiResponse.LoginV1>(
                ApiCommand.LoginV1()
            )

            val responseLoginAuth = executeCommand<ApiResponse.LoginAuthV1>(
                ApiCommand.LoginAuthV1(
                    username = options.user,
                    password = encryptPassword(
                        responseLogin.responseData.publicKeyModulus,
                        responseLogin.responseData.publicKeyExponent
                    ),
                    useCompression = options.compression,
                    driverName = "${ExaConstant.DRIVER_NAME} ${ExaConstant.DRIVER_VERSION}",
                    clientName = options.clientName,
                    clientVersion = options.clientVersion,
                    clientOs = "${System.getProperty("os.name")} ${System.getProperty("os.version")}",
                    clientOsUsername = System.getProperty("user.name"),
                    clientRuntime = "Kotlin ${KotlinVersion.CURRENT}",
                    attributes = mapOf(
                        "autocommit" to options.autocommit,
                        "currentSchema" to options.schema,
                        "queryTimeout" to options.queryTimeout,
                    )
                )
            )

            loginInfo = responseLoginAuth.responseData

            if (options.compression) {
                wsClient.enableCompression()
            }
        }

        refreshAttributes()
    }

    /**
     * Execute SQL query with optional formatting.
     *
     * @see ExaQueryFormatter
     *
     * Optional snapshot execution mode prevents locks while querying system tables and views
     * Learn more: https://www.exasol.com/support/browse/EXASOL-2646
     *
     * @param[query] SQL query with named and typed placeholders
     * @param[queryParams] Map of names and substitution values for SQL query placeholders
     * @param[snapshotExecution] Execute query in a snapshot mode
     *
     * @throws ExaQueryException
     * @return ExaStatement
     */
    @JvmOverloads
    fun execute(query: String, queryParams: Map<String, Any?>? = null, snapshotExecution: Boolean = false): ExaStatement {
        return ExaStatement(this, query, queryParams, snapshotExecution)
    }

    fun commit(): ExaStatement {
        return execute("COMMIT")
    }

    fun rollback(): ExaStatement {
        return execute("ROLLBACK")
    }

    /**
     * Abort query which is currently running by this connection.
     *
     * This function does not block and returns immediately. It is supposed to be called from another thread.
     *
     * There are three possible outcomes of this call:
     *
     * 1. Execution finishes successfully before the `abortQuery` takes effect;
     * 2. Execution is aborted, [ExaQueryAbortException] will be thrown;
     * 3. Execution is aborted, but Exasol server terminates the entire connection;
     *
     * Please see example `08_abort_query.kt` for more details.
     */
    fun abortQuery() {
        executeCommandWithoutResponse(ApiCommand.AbortQueryV1())
    }

    /**
     * IMPORT a large amount of data into table using raw CSV streaming.
     *
     * Explicit list of column names is required to protect from unexpected changes in table structure.
     *
     * Please see example `02_stream.kt` for more details.
     *
     * @param[table] Table name in the [currentSchema]
     * @param[columns] List of ordered column names
     * @param[operation] Lambda which accepts [okio.BufferedSink] and writes CSV data into it
     *
     * @throws ExaStreamException
     * @return ExaStatement
     */
    fun streamImport(table: String, columns: List<String>, operation: (BufferedSink) -> Unit): ExaStatement {
        return executeStream(ExaStreamMode.IMPORT, queryOrTable = table, columns = columns, sinkOperation = operation)
    }

    /**
     * IMPORT a large amount of data into table using [ExaStreamCSVWriter] wrapper.
     *
     * Explicit list of column names is required to protect from unexpected changes in table structure.
     *
     * Please see example `02_stream.kt` for more details.
     *
     * @param[table] Table name in the [currentSchema]
     * @param[columns] List of ordered column names
     * @param[operation] Lambda which accepts [ExaStreamCSVWriter] and writes raw CSV data using it
     *
     * @throws ExaStreamException
     * @return ExaStatement
     */
    fun streamImportWriter(table: String, columns: List<String>, operation: (ExaStreamCSVWriter) -> Unit): ExaStatement {
        return executeStream(ExaStreamMode.IMPORT, queryOrTable = table, columns = columns, sinkOperation = { sink ->
            ExaStreamCSVWriter(columns, sink).use { writer ->
                operation(writer)
            }
        })
    }

    /**
     * IMPORT data into table from Iterator emitting Lists of values.
     *
     * This is a convenient substitution for basic "INSERT multiple rows" scenario.
     *
     * Please see example `02_stream.kt` for more details.
     *
     * @param[table] Table name in the [currentSchema]
     * @param[columns] List of ordered column names
     * @param[iterator] Iterator emitting Lists of values
     *
     * @throws ExaStreamException
     * @return ExaStatement
     */
    @JvmName("streamImportIteratorList")
    fun streamImportIterator(table: String, columns: List<String>, iterator: Iterator<List<Any?>>): ExaStatement {
        return executeStream(ExaStreamMode.IMPORT, queryOrTable = table, columns = columns, sinkOperation = { sink ->
            ExaStreamCSVWriter(columns, sink).use { writer ->
                iterator.forEach {
                     writer.writeRow(it)
                }
            }
        })
    }

    /**
     * IMPORT data into table from Iterator emitting Maps of column names and values.
     *
     * This is a convenient substitution for basic "INSERT multiple rows" scenario.
     *
     * Please see example `02_stream.kt` for more details.
     *
     * @param[table] Table name in the [currentSchema]
     * @param[columns] List of ordered column names
     * @param[iterator] Iterator emitting Lists of values
     *
     * @throws ExaStreamException
     * @return ExaStatement
     */
    @JvmName("streamImportIteratorMap")
    fun streamImportIterator(table: String, columns: List<String>, iterator: Iterator<Map<String, Any?>>): ExaStatement {
        return executeStream(ExaStreamMode.IMPORT, queryOrTable = table, columns = columns, sinkOperation = { sink ->
            ExaStreamCSVWriter(columns, sink).use { writer ->
                iterator.forEach {
                    writer.writeRow(it)
                }
            }
        })
    }

    /**
     * EXPORT a large amount from table or SQL query using raw CSV streaming.
     *
     * First line contains column names.
     *
     * Please see example `02_stream.kt` for more details.
     *
     * @param[queryOrTable] Table name in the [currentSchema] or SQL query with named and typed placeholders
     * @param[queryParams] Map of names and substitution values for SQL query placeholders
     * @param[columns] List of ordered column names
     * @param[operation] Lambda which accepts [okio.BufferedSource] and reads raw CSV data from it
     *
     * @throws ExaStreamException
     * @return ExaStatement
     */
    fun streamExport(queryOrTable: String, queryParams: Map<String, Any?>? = null, columns: List<String>? = null, operation: (BufferedSource) -> Unit): ExaStatement {
        return executeStream(ExaStreamMode.EXPORT, queryOrTable, queryParams, columns, sourceOperation = operation)
    }

    /**
     * EXPORT a large amount from table or SQL query using [ExaStreamCSVReader] wrapper.
     *
     * Please see example `02_stream.kt` for more details.
     *
     * @param[queryOrTable] Table name in the [currentSchema] or SQL query with named and typed placeholders
     * @param[queryParams] Map of names and substitution values for SQL query placeholders
     * @param[columns] List of ordered column names
     * @param[operation] Lambda which accepts [ExaStreamCSVReader] and reads CSV data using it
     *
     * @throws ExaStreamException
     * @return ExaStatement
     */
    fun streamExportReader(queryOrTable: String, queryParams: Map<String, Any?>? = null, columns: List<String>? = null, operation: (ExaStreamCSVReader) -> Unit): ExaStatement {
        return executeStream(ExaStreamMode.EXPORT, queryOrTable, queryParams, columns, sourceOperation = { source ->
            ExaStreamCSVReader(source).use { reader ->
                operation(reader)
            }
        })
    }

    /**
     * Return shuffled list of Exasol node addresses used to initiate workers for parallel CSV steaming.
     *
     * Please see examples `10_stream_parallel_export.kt`, `11_stream_parallel_import.kt` for more details.
     *
     * If number of workers is larger than amount of Exasol nodes, addresses will be wrapped around and repeated.
     * If number of workers is omitted, return exactly one address per Exasol node.
     *
     * @param[numberOfWorkers] Number of workers for parallel CSV streaming
     * @return List<ExaNodeAddress>
     */
    fun streamParallelNodes(numberOfWorkers: Int? = null): List<ExaNodeAddress> {
        val response = executeCommand<ApiResponse.GetHostsV1>(
            ApiCommand.GetHostsV1(
                hostIp = wsClient.wsSocketAddress.address.hostAddress
            )
        )

        val nodesList = mutableListOf<ExaNodeAddress>()

        repeat(numberOfWorkers ?: response.responseData.numNodes) { idx ->
            nodesList.add(ExaNodeAddress("${response.responseData.nodes[idx % response.responseData.numNodes]}:${wsClient.wsSocketAddress.port}"))
        }

        return nodesList
    }

    /**
     * IMPORT a large amount of data in parallel using CSV streaming.
     *
     * This function builds and executes IMPORT SQL query only.
     *
     * The actual processing is done by workers (processes or threads), which should be initiated before calling this function.
     * List of [ExaStreamInternalAddress] should be obtained from workers and passed as an argument.
     *
     * Please see example `11_stream_parallel_import.kt` for more details.
     *
     * @param[internalAddressList] List of Exasol internal IP addresses obtained from workers
     * @param[table] Table name in the [currentSchema]
     * @param[columns] List of ordered column names
     *
     * @throws ExaQueryException
     * @return ExaStatement
     */
    fun streamParallelImport(internalAddressList: List<ExaStreamInternalAddress>, table: String, columns: List<String>): ExaStatement {
        return executeParallelStream(ExaStreamMode.IMPORT, internalAddressList, queryOrTable = table, columns = columns)
    }

    /**
     * EXPORT a large amount of data in parallel using CSV streaming.
     *
     * This function builds and executes EXPORT SQL query only.
     *
     * The actual processing is done by workers (processes or threads), which should be initiated before calling this function.
     * List of [ExaStreamInternalAddress] should be obtained from workers and passed as an argument.
     *
     * Please see example `10_stream_parallel_export.kt` for more details.
     *
     * @param[internalAddressList] List of Exasol internal IP addresses obtained from workers
     * @param[queryOrTable] Table name in the [currentSchema] or SQL query with named and typed placeholders
     * @param[queryParams] Map of names and substitution values for SQL query placeholders
     * @param[columns] List of ordered column names
     *
     * @throws ExaQueryException
     * @return ExaStatement
     */
    fun streamParallelExport(internalAddressList: List<ExaStreamInternalAddress>, queryOrTable: String, queryParams: Map<String, Any?>? = null, columns: List<String>? = null): ExaStatement {
        return executeParallelStream(
            ExaStreamMode.EXPORT,
            internalAddressList,
            queryOrTable = queryOrTable,
            queryParams = queryParams,
            columns = columns
        )
    }

    /**
     * Autocommit mode TRUE means all SQL queries will be committed implicitly.
     * With mode FALSE you can run multiple SQL queries in one transaction and call [commit] or [rollback] functions explicitly to finish transaction.
     *
     * Please see example `05_transaction.kt` for more details.
     *
     * @param[value] Enable or disable autocommit
     */
    fun setAutocommit(value: Boolean) {
        setAttributes(mapOf("autocommit" to value))
    }

    /**
     * Exasol query timeout in seconds, managed by Exasol server.
     * Queries terminated by timeout will throw [ExaQueryTimeoutException].
     *
     * Please see example `08_abort_query.kt` for more details.
     *
     * @param[value] Timeout in seconds, "0" means to timeout
     */
    fun setQueryTimeout(value: Long) {
        setAttributes(mapOf("queryTimeout" to value))
    }

    /**
     * Change currently opened schema.
     * Please note: [rollback] call may revert current schema back to original value.
     *
     * @param[schema] Schema name to open
     */
    fun openSchema(schema: String) {
        setAttributes(mapOf("currentSchema" to schema))
    }

    /**
     * Set multiple connection attributes in one call.
     * Full list of possible attributes: https://github.com/exasol/websocket-api/blob/master/docs/WebsocketAPIV2.md#attributes-session-and-database-properties
     *
     * @param[attr] Map of new values for attributes
     */
    fun setAttributes(attr: Map<String, Any>) {
        executeCommand<ApiResponse.SetAttributesV1>(
            ApiCommand.SettAttributesV1(
                attributes = attr
            )
        )

        // SetAttributes response is inconsistent, so all attributes should be refreshed explicitly after each call
        refreshAttributes()
    }

    /**
     * Close() should always be called on connection object when it is no longer needed.
     *
     * It is advised to take advantage of Kotlin `.use{}` function to call close automatically in case of exception.
     *
     * Non-closed connections may linger in `EXA_ALL_SESSIONS` for a long time, and `LOGOUT_TIME` will be inconsistent.
     */
    override fun close() {
        if (!isClosed) {
            if (wsClient.isConnected) {
                wsClient.close()
            }

            isClosed = true
            logDebug("Connection was closed")
        }
    }

    private fun refreshAttributes() {
        executeCommand<ApiResponse.GetAttributesV1>(ApiCommand.GetAttributesV1())
    }

    internal inline fun <reified T : ApiResponse> executeCommand(command: ApiCommand): T {
        executeLock.withLock {
            commandCount++

            val adapterCommand = moshi.adapter<ApiCommand>(command::class.java)

            val adapterCommonResponse = moshi.adapter(ApiResponse.CommonResponse::class.java)
            val adapterSpecificResponse = moshi.adapter(T::class.java)

            if (logger.isDebugEnabled) {
                logDebugJson("Request #${commandCount}", adapterCommand.indent("    ").toJson(command))
            }

            val jsonStringResponse = wsClient.sendCommandAndWaitForResponse(adapterCommand.toJson(command))
            lastCommandTimeMillis = wsClient.lastResponseTimeMillis

            val commonResponse = adapterCommonResponse.fromJson(jsonStringResponse) ?: throw ExaException(this, "Could not parse the common response")

            commonResponse.attributes?.let {
                attributes = attributes.plus(commonResponse.attributes)
            }

            if (commonResponse.status == "ok") {
                val specificResponse = adapterSpecificResponse.fromJson(jsonStringResponse) ?: throw ExaException(this, "Could not parse the specific response")

                if (logger.isDebugEnabled) {
                    logDebugJson("Response #${commandCount}", adapterSpecificResponse.indent("    ").toJson(specificResponse))
                }

                return specificResponse
            }

            if (commonResponse.status == "error") {
                if (logger.isDebugEnabled) {
                    logDebugJson("Response #${commandCount}", adapterCommonResponse.indent("    ").toJson(commonResponse))
                }

                when (command) {
                    is ApiCommand.ExecuteV1 -> {
                        when (commonResponse.exception!!.sqlCode) {
                            "R0001" -> throw ExaQueryTimeoutException(
                                this, commonResponse.exception.text, extra = mapOf(
                                    "sqlCode" to commonResponse.exception.sqlCode,
                                    "sqlText" to command.sqlText
                                )
                            )

                            "R0003" -> throw ExaQueryAbortException(
                                this, commonResponse.exception.text, extra = mapOf(
                                    "sqlCode" to commonResponse.exception.sqlCode,
                                    "sqlText" to command.sqlText
                                )
                            )

                            "R0004" -> {
                                // Exasol server will no longer respond to any commands from this connection
                                close()

                                throw ExaQueryConnectionKilledException(
                                    this, commonResponse.exception.text, extra = mapOf(
                                        "sqlCode" to commonResponse.exception.sqlCode,
                                        "sqlText" to command.sqlText
                                    )
                                )
                            }

                            else -> throw ExaQueryException(
                                this, commonResponse.exception.text, extra = mapOf(
                                    "sqlCode" to commonResponse.exception.sqlCode,
                                    "sqlText" to command.sqlText
                                )
                            )
                        }
                    }

                    is ApiCommand.LoginAuthV1 -> throw ExaCommandAuthException(
                        this, commonResponse.exception!!.text, extra = mapOf(
                            "sqlCode" to commonResponse.exception.sqlCode
                        )
                    )

                    else -> throw ExaCommandException(
                        this, commonResponse.exception!!.text, extra = mapOf(
                            "sqlCode" to commonResponse.exception.sqlCode
                        )
                    )
                }
            }

            throw ExaException(
                this, "Invalid response status", extra = mapOf(
                    "status" to commonResponse.status
                )
            )
        }
    }

    internal fun executeCommandWithoutResponse(command: ApiCommand) {
        val adapterCommand = moshi.adapter<ApiCommand>(command::class.java)

        if (logger.isDebugEnabled) {
            logDebugJson("Request without response", adapterCommand.indent("    ").toJson(command))
        }

        wsClient.sendCommandWithoutResponse(adapterCommand.toJson(command))
    }

    internal fun executeStream(
        mode: ExaStreamMode,
        queryOrTable: String,
        queryParams: Map<String, Any?>? = null,
        columns: List<String>? = null,
        sourceOperation: ((BufferedSource) -> Unit)? = null,
        sinkOperation: ((BufferedSink) -> Unit)? = null
    ): ExaStatement {
        val streamHttpThread = ExaStreamHttpThread(options, wsClient.wsSocketAddress)

        val streamQueryThread = ExaStreamQueryThread(
            this,
            mode,
            listOf(streamHttpThread.internalAddress),
            columns,
            queryOrTable,
            queryParams,
            streamHttpThread
        )

        try {
            streamHttpThread.start()
            streamQueryThread.start()

            when (mode) {
                ExaStreamMode.IMPORT -> {
                    sinkOperation!!(streamHttpThread.dataSinkBuffer)
                    streamHttpThread.dataSinkBuffer.close()
                }
                ExaStreamMode.EXPORT -> {
                    sourceOperation!!(streamHttpThread.dataSourceBuffer)
                    streamHttpThread.dataSourceBuffer.close()
                }
            }

            streamQueryThread.joinWithException()
            streamHttpThread.joinWithException()

            return streamQueryThread.statement
        } catch (exc: Throwable) {
            if (streamHttpThread.isAlive) {
                streamHttpThread.interrupt()
                streamHttpThread.join()
            }

            // Allow a bit of time for SQL process to terminate normally
            streamQueryThread.join(1000)

            // If SQL process is still alive, abort it explicitly
            if (streamQueryThread.isAlive) {
                abortQuery()
                streamQueryThread.join()
            }

            // Create combined super-exception
            val streamExc = ExaStreamException(
                this,
                message = exc.message,
                cause = exc,
                extra = mapOf(
                    "httpMessage" to streamHttpThread.lastException?.message,
                    "queryMessage" to streamQueryThread.lastException?.message
                )
            )

            if (exc != streamHttpThread.lastException && exc != streamQueryThread.lastException) {
                streamExc.addSuppressed(exc)
            }

            streamQueryThread.lastException?.let {
                streamExc.addSuppressed(streamQueryThread.lastException)
            }

            streamHttpThread.lastException?.let {
                streamExc.addSuppressed(streamHttpThread.lastException)
            }

            throw streamExc
        }
    }

    internal fun executeParallelStream(
        mode: ExaStreamMode,
        internalAddressList: List<ExaStreamInternalAddress>,
        queryOrTable: String,
        queryParams: Map<String, Any?>? = null,
        columns: List<String>? = null
    ): ExaStatement {
        val streamQueryThread = ExaStreamQueryThread(
            this,
            mode,
            internalAddressList,
            columns,
            queryOrTable,
            queryParams,
        )

        return execute(streamQueryThread.buildQuery())
    }

    internal fun logDebug(message: String) {
        logger.debug("[$logHash] $message")
    }

    internal fun logDebugJson(message: String, jsonString: String) {
        if (options.loggerJsonMaxLength != null && jsonString.length >= options.loggerJsonMaxLength) {
            logger.debug("[$logHash] $message:" +
                    "\n${jsonString.substring(0, options.loggerJsonMaxLength)}" +
                    "\n------ TRUNCATED TOO LONG MESSAGE ------\n"
            )
        } else {
            logger.debug("[$logHash] $message:\n$jsonString")
        }
    }

    private fun encryptPassword(publicKeyModulus: String, publicKeyExponent: String): String {
        val keySpec = RSAPublicKeySpec(BigInteger(publicKeyModulus, 16), BigInteger(publicKeyExponent, 16))
        val pubkey = KeyFactory.getInstance("RSA").generatePublic(keySpec)

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, pubkey)

        return Base64.getEncoder().encodeToString(cipher.doFinal(options.password.toByteArray()))
    }

    private fun generateLogHash(): String {
        val allowedChars = ('a'..'z') + ('0'..'9')
        return (1..8).map { allowedChars.random() }.joinToString("")
    }
}
