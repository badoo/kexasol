package com.badoo.kexasol.stream

import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Exasol private network IPv4 address used for CSV streaming
 */
class ExaStreamInternalAddress(addr: InetAddress, port: Int) : InetSocketAddress(addr, port) {

    // Create object from String representation
    constructor (addrWithPort: String) : this(
        InetAddress.getByName(addrWithPort.substringBeforeLast(':')),
        addrWithPort.substringAfterLast(':').toInt()
    )

    override fun toString(): String {
        return "${address.hostAddress}:${port}"
    }
}
