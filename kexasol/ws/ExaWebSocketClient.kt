package com.badoo.kexasol.ws

import com.badoo.kexasol.ExaConnection
import com.badoo.kexasol.enum.ExaEncryptionMode
import com.badoo.kexasol.net.ExaNoCertHostnameVerifier
import com.badoo.kexasol.net.ExaNoCertTrustManager
import com.badoo.kexasol.net.ExaNodeAddress
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okio.*
import java.io.Closeable
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.SSLContext
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis


internal class ExaWebSocketClient(
    val connection: ExaConnection
): Closeable {
    val wsLock = ReentrantLock()
    val wsLockCondition: Condition = wsLock.newCondition()

    val okHttpClient: OkHttpClient

    lateinit var webSocket: WebSocket
    lateinit var wsSocketAddress: ExaNodeAddress
    lateinit var lastResponse: ExaWebSocketResponse

    var compressionEnabled = false
    var lastResponseTimeMillis: Long = 0L

    var isConnected = false

    init {
        val builder = OkHttpClient.Builder()

        builder.connectTimeout(connection.options.connectionTimeout, TimeUnit.SECONDS)
        builder.readTimeout(connection.options.socketTimeout, TimeUnit.SECONDS)
        builder.writeTimeout(connection.options.socketTimeout, TimeUnit.SECONDS)

        // Enable SSL, but disable verification
        // JDBC driver does exactly the same by default
        if (connection.options.encryption == ExaEncryptionMode.ENABLED_NO_CERT) {
            val trustManager = ExaNoCertTrustManager()
            val hostnameVerifier = ExaNoCertHostnameVerifier()

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(trustManager), SecureRandom())

            builder
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .hostnameVerifier(hostnameVerifier)
        }

        okHttpClient = builder.build()
    }

    fun attemptConnect(nodeAddress: ExaNodeAddress): Throwable? {
        val schema = when (connection.options.encryption) {
            ExaEncryptionMode.DISABLED -> "ws://"
            else -> "wss://"
        }

        val httpRequest = Request.Builder()
            .url("${schema}${nodeAddress}")
            .build()

        wsLock.withLock {
            lastResponseTimeMillis = measureTimeMillis {
                webSocket = okHttpClient.newWebSocket(httpRequest, ExaWebSocketListener(this))
                wsLockCondition.await()
            }

            return when (lastResponse.type) {
                ExaWebSocketResponseType.OPEN -> {
                    wsSocketAddress = nodeAddress
                    null
                }
                ExaWebSocketResponseType.FAILURE -> lastResponse.cause
                else -> throw RuntimeException("Unexpected WebSocket result type")
            }
        }
    }

    fun sendCommandAndWaitForResponse(json: String): String {
        wsLock.withLock {
            lastResponseTimeMillis = measureTimeMillis {
                when (compressionEnabled) {
                    true -> webSocket.send(compressMessage(json))
                    false -> webSocket.send(json)
                }

                wsLockCondition.await()
            }

            return when (lastResponse.type) {
                ExaWebSocketResponseType.TEXT -> lastResponse.text!!
                ExaWebSocketResponseType.BYTES -> decompressMessage(lastResponse.bytes!!)
                ExaWebSocketResponseType.FAILURE -> throw lastResponse.cause!!
                else -> throw RuntimeException("Unexpected WebSocket result type")
            }
        }
    }

    fun sendCommandWithoutResponse(json: String) {
        when (compressionEnabled) {
            true -> webSocket.send(compressMessage(json))
            false -> webSocket.send(json)
        }
    }

    fun enableCompression() {
        compressionEnabled = true
    }

    override fun close() {
        if (isConnected) {
            webSocket.close(1000, null)
        }
    }

    private fun compressMessage(input: String): ByteString {
        val dataBuffer = Buffer()
        val deflaterBuffer = dataBuffer.deflate().buffer()

        deflaterBuffer.writeUtf8(input)
        deflaterBuffer.close()

        return dataBuffer.readByteString()
    }

    private fun decompressMessage(input: ByteString): String {
        val dataBuffer = Buffer()
        val inflaterBuffer = dataBuffer.inflate().buffer()

        dataBuffer.write(input)
        dataBuffer.close()

        return inflaterBuffer.readUtf8()
    }
}
