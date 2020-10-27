package com.badoo.kexasol.stream

import okio.*
import java.net.ProtocolException

internal class ExaStreamChunkedSource(source: Source) : Source {
    private val sourceBuffer = source.buffer()
    private var remainingBytesInChunk = -1L

    override fun timeout(): Timeout = ForwardingTimeout(sourceBuffer.timeout())

    override fun read(sink: Buffer, byteCount: Long): Long {
        if (remainingBytesInChunk <= 0L) {
            remainingBytesInChunk = sourceBuffer.readHexadecimalUnsignedLong()
            sourceBuffer.skip(2)

            if (remainingBytesInChunk == 0L) {
                sourceBuffer.skip(2)
                return -1L
            }
        }

        val readBytes = sourceBuffer.read(sink, minOf(byteCount, remainingBytesInChunk))

        if (readBytes == -1L) {
            throw ProtocolException("Unexpected end of stream")
        }

        remainingBytesInChunk -= readBytes

        if (remainingBytesInChunk == 0L) {
            sourceBuffer.skip(2)
        }

        return readBytes
    }

    override fun close() {
        sourceBuffer.close()
    }
}
