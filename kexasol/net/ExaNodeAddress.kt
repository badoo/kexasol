package com.badoo.kexasol.net

import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Exasol node address used to establish connections
 */
class ExaNodeAddress(addr: InetAddress, port: Int) : InetSocketAddress(addr, port) {

    // Create object from String representation
    constructor (addrWithPort: String) : this(
        InetAddress.getByName(addrWithPort.substringBeforeLast(':')),
        addrWithPort.substringAfterLast(':').toInt()
    )

    // Return string representation which is human readable and acceptable by OkHttp
    override fun toString(): String {
        return if (address is Inet6Address) {
            "[${address.hostAddress}]:${port}"
        } else {
            "${address.hostAddress}:${port}"
        }
    }
}
