package com.badoo.kexasol.ws

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import kotlin.concurrent.withLock


internal class ExaWebSocketListener(private val client: ExaWebSocketClient) : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        client.wsLock.withLock {
            client.isConnected = true
            client.lastResponse = ExaWebSocketResponse(ExaWebSocketResponseType.OPEN)
            client.wsLockCondition.signal()
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        client.wsLock.withLock {
            client.lastResponse = ExaWebSocketResponse(ExaWebSocketResponseType.TEXT, text = text)
            client.wsLockCondition.signal()
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        client.wsLock.withLock {
            client.lastResponse = ExaWebSocketResponse(ExaWebSocketResponseType.BYTES, bytes = bytes)
            client.wsLockCondition.signal()
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        client.wsLock.withLock {
            client.isConnected = false
            client.lastResponse = ExaWebSocketResponse(ExaWebSocketResponseType.FAILURE, cause = t)
            client.wsLockCondition.signal()
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, "")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        client.isConnected = false
    }
}
