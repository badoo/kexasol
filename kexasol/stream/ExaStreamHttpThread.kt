package com.badoo.kexasol.stream

import com.badoo.kexasol.ExaConnectionOptions
import com.badoo.kexasol.enum.ExaEncryptionMode
import com.badoo.kexasol.net.ExaNodeAddress
import com.badoo.kexasol.net.ExaSelfSignedSslSocketFactory
import okio.*
import java.io.IOException
import java.net.InetAddress
import java.net.ProtocolException
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import kotlin.concurrent.withLock

internal class ExaStreamHttpThread(
    private val options: ExaConnectionOptions,
    private val nodeAddress: ExaNodeAddress
) : Thread() {

    private var socket = Socket().apply {
        this.connect(nodeAddress, options.connectionTimeout.toInt() * 1000)
        this.soTimeout = options.socketTimeout.toInt() * 1000
    }

    private var socketInputBuffer: BufferedSource = socket.source().buffer()
    private var socketOutputBuffer: BufferedSink = socket.sink().buffer()

    private val dataPipe = Pipe(1024L * 1024L) // 1Mb

    val dataSinkBuffer = dataPipe.sink.buffer()
    val dataSourceBuffer = dataPipe.source.buffer()

    val internalAddress: ExaStreamInternalAddress
    var lastException: Exception? = null

    init {
        internalAddress = requestInternalAddress()
    }

    private fun requestInternalAddress(): ExaStreamInternalAddress {
        // Send special packet to establish proxy connection for HTTP transport
        socketOutputBuffer.writeIntLe(0x02212102)
        socketOutputBuffer.writeIntLe(1)
        socketOutputBuffer.writeIntLe(1)
        socketOutputBuffer.flush()

        // Skip the first int in response (protocol version?)
        socketInputBuffer.skip(4)

        // Second int is a port number
        val port = socketInputBuffer.readIntLe()

        // Following 16 bytes represent Exasol internal IPv4 address
        val ipAddress = socketInputBuffer.readUtf8(16).trim()

        return ExaStreamInternalAddress(InetAddress.getByName(ipAddress), port)
    }

    override fun run() {
        try {
            runInner()
        } catch (exc: Exception) {
            lastException = exc
            dataPipe.cancel()
        } finally {
            socket.close()
        }
    }

    private fun runInner() {
        // Wait for incoming HTTP request initiated by Exasol
        // Allow other threads to interrupt this wait
        waitForIncomingData()

        if (options.encryption != ExaEncryptionMode.DISABLED) {
            createSslSocket()
        }

        val method = socketInputBuffer.readUtf8Line()!!.split(" ")[0]

        while (socketInputBuffer.readUtf8Line() != "") {
            // skip all other headers
        }

        when (method) {
            "PUT" -> handlePut()
            "GET" -> handleGet()
            else -> throw ProtocolException("Unexpected HTTP method")
        }
    }

    fun joinWithException() {
        super.join()

        lastException?.let {
            throw it
        }
    }

    private fun waitForIncomingData() {
        socket.soTimeout = 200

        while (true) {
            try {
                socketInputBuffer.request(1)
                socket.soTimeout = options.socketTimeout.toInt() * 1000
                return
            } catch (e: SocketTimeoutException) {
                continue
            }
        }
    }

    private fun createSslSocket() {
        // Use server SSLSocket with self-signed certificate created on the fly
        socket = (ExaSelfSignedSslSocketFactory(options).wrapSocket(socket, socketInputBuffer.inputStream())).apply {
            socketInputBuffer = this.source().buffer()
            socketOutputBuffer = this.sink().buffer()

            this.startHandshake()
        }
    }

    private fun handlePut() {
        val wrappedInputBuffer = when (options.compression) {
            true -> GzipSource(ExaStreamChunkedSource(socketInputBuffer)).buffer()
            false -> ExaStreamChunkedSource(socketInputBuffer).buffer()
        }

        wrappedInputBuffer.readAll(dataSinkBuffer)
        dataSinkBuffer.close()

        sendHeaders()
    }

    private fun handleGet() {
        sendHeaders()

        val wrappedOutputBuffer = when (options.compression) {
            true -> GzipSink(socketOutputBuffer).buffer()
            false -> socketOutputBuffer
        }

        wrappedOutputBuffer.writeAll(dataSourceBuffer)
        wrappedOutputBuffer.flush()

        dataSourceBuffer.close()
    }

    private fun sendHeaders() {
        socketOutputBuffer.writeUtf8("HTTP/1.1 200 OK\r\n")
        socketOutputBuffer.writeUtf8("Connection: close\r\n")
        socketOutputBuffer.writeUtf8("\r\n")
        socketOutputBuffer.flush()
    }
}
