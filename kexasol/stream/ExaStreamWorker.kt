package com.badoo.kexasol.stream

import com.badoo.kexasol.ExaConnectionOptions
import com.badoo.kexasol.exception.ExaStreamException
import com.badoo.kexasol.mode.ExaStreamMode
import com.badoo.kexasol.net.ExaNodeAddress
import okio.BufferedSink
import okio.BufferedSource
import java.io.Closeable

/**
 * This class is supposed to be instantiated in child processes during parallel CSV streaming.
 *
 * 1. Open "proxy" connection to Exasol using `ExaNodeAddress` obtained from [streamParallelNodes].
 * 2. Get `internalAddress` from Exasol server and make it available for [streamParallelImport] or [streamParallelExport] function.
 * 3. Call stream* function of worker to wait for incoming HTTP request from Exasol and do the actual work by reading or writing data.
 *
 * @param[options] Exasol connection options (user and password can be omitted)
 * @param[nodeAddress] Exasol node addressed obtained from streamParallelNodes
 */
class ExaStreamWorker(val options: ExaConnectionOptions, nodeAddress: ExaNodeAddress) : Closeable {
    private val streamHttpThread = ExaStreamHttpThread(options, nodeAddress).apply {
        this.start()
    }

    /**
     * Internal IPv4 address from Exasol private network.
     * Get these addresses from all child processes and use it for streamParallel* calls.
     */
    val internalAddress = streamHttpThread.internalAddress

    /**
     * IMPORT a large amount of data from child process using raw CSV streaming.
     *
     * Please see example `11_stream_parallel_import.kt` for more details.
     *
     * @param[operation] Lambda which accepts [okio.BufferedSink] and writes raw CSV data into it
     */
    fun streamImport(operation: (BufferedSink) -> Unit) {
        processStream(mode = ExaStreamMode.IMPORT, sinkOperation = operation)
    }

    /**
     * IMPORT a large amount of data from child process using [ExaStreamCSVWriter] wrapper.
     *
     * Please see example `11_stream_parallel_import.kt` for more details.
     *
     * @param[columns] List of ordered column names
     * @param[operation] Lambda which accepts [ExaStreamCSVWriter] and writes data using it
     */
    fun streamImportWriter(columns: List<String>, operation: (ExaStreamCSVWriter) -> Unit) {
        processStream(mode = ExaStreamMode.IMPORT, sinkOperation = { sink ->
            ExaStreamCSVWriter(columns, sink).use { writer ->
                operation(writer)
            }
        })
    }

    /**
     * EXPORT a large amount of data into child process using raw CSV streaming.
     *
     * First line contains column names.
     *
     * Please see example `10_stream_parallel_export.kt` for more details.
     *
     * @param[operation] Lambda which accepts [okio.BufferedSource] and reads raw CSV data from it
     */
    fun streamExport(operation: (BufferedSource) -> Unit) {
        processStream(mode = ExaStreamMode.EXPORT, sourceOperation = operation)
    }

    /**
     * EXPORT a large amount of data into child process using [ExaStreamCSVReader] wrapper.
     *
     * Please see example `10_stream_parallel_export.kt` for more details.
     *
     * @param[operation] Lambda which accepts [ExaStreamCSVReader] and reads data from it
     */
    fun streamExportReader(operation: (ExaStreamCSVReader) -> Unit) {
        processStream(mode = ExaStreamMode.EXPORT, sourceOperation = { source ->
            ExaStreamCSVReader(source).use { reader ->
                operation(reader)
            }
        })
    }

    private fun processStream(
        mode: ExaStreamMode,
        sourceOperation: ((BufferedSource) -> Unit)? = null,
        sinkOperation: ((BufferedSink) -> Unit)? = null
    ) {
        try {
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

            streamHttpThread.joinWithException()

        } catch (exc: Throwable) {
            if (streamHttpThread.isAlive) {
                streamHttpThread.interrupt()
                streamHttpThread.join()
            }

            if (exc != streamHttpThread.lastException) {
                exc.addSuppressed(streamHttpThread.lastException)
            }

            throw exc
        }
    }

    override fun close() {
        if (streamHttpThread.isAlive) {
            streamHttpThread.interrupt()
            streamHttpThread.join()
        }
    }
}
