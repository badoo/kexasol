package com.badoo.kexasol

import com.badoo.kexasol.enum.ExaEncryptionMode
import com.badoo.kexasol.net.ExaNodeAddress
import com.badoo.kexasol.stream.ExaStreamWorker

/**
 * @param[dsn] Exasol connection string, same format as standard JDBC / ODBC drivers
 * @param[user] Username
 * @param[password] Password
 * @param[schema] Open schema after connection
 * @param[autocommit] Enable autocommit after connection
 * @param[compression] Enable zlib compression for WebSocket requests and for CSV streaming
 * @param[encryption] Enable SSL encryption for WebSocket requests and for CSV streaming
 * @param[quoteIdentifier] Add double quotes and escape identifiers passed to relevant functions. It is useful if have to refer to object names with lower-cased letter or special characters.
 * @param[connectionTimeout]Timeout for opening WebSocket connection to Exasol node in seconds
 * @param[socketTimeout] Timeout for WebSocket reads and writes in seconds. Normally it should never be triggered due to PONG frames sent by Exasol server every second.
 * @param[queryTimeout] Query timeout in seconds. 0 means "no timeout".
 * @param[clientName] Custom client application name stored in Exasol system tables
 * @param[clientVersion] Custom client application version stored in Exasol system tables
 * @param[verboseException] Add additional debug information for toString() call of exceptions derived from `ExaException`
 * @param[loggerJsonMaxLength] Maximum length of JSON dumped into debug logs, helps to prevent unnecessary log bloating
 */
class ExaConnectionOptions(
    val dsn: String,
    val user: String = "",
    val password: String = "",
    val schema: String = "",
    val autocommit: Boolean = true,
    val compression: Boolean = false,
    val encryption: ExaEncryptionMode = ExaEncryptionMode.DISABLED,
    val quoteIdentifier: Boolean = false,
    val connectionTimeout: Long = ExaConstant.DEFAULT_CONNECTION_TIMEOUT,
    val socketTimeout: Long = ExaConstant.DEFAULT_SOCKET_TIMEOUT,
    val queryTimeout: Long = 0L,
    val clientName: String = ExaConstant.DRIVER_NAME,
    val clientVersion: String = ExaConstant.DRIVER_VERSION,
    val verboseException: Boolean = true,
    val loggerJsonMaxLength: Int? = ExaConstant.DEFAULT_LOGGER_JSON_MAX_LENGTH,
) {
    /**
     * Open new Exasol connection using provided connection options.
     */
    fun connect(): ExaConnection {
        return ExaConnection(this)
    }

    /**
     * Initiate new stream worker in child process or thread using provided connection options.
     *
     * [dsn], [compression] and [encryption] should match the parent process.
     * [user] and [password] are not used by stream workers, so you may leave it empty.
     *
     * @param[nodeAddress] Specific Exasol node address for StreamWorker to connect
     */
    fun streamWorker(nodeAddress: ExaNodeAddress): ExaStreamWorker {
        return ExaStreamWorker(this, nodeAddress)
    }
}
