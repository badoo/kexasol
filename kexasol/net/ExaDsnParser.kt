package com.badoo.kexasol.net

import com.badoo.kexasol.ExaConnection
import com.badoo.kexasol.ExaConstant
import com.badoo.kexasol.exception.ExaConnectionDsnException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.regex.Pattern

internal class ExaDsnParser(val connection: ExaConnection) {
    private val dsnPattern: Pattern =
        Pattern.compile("""^(?<hostPrefix>.+?)(?:(?<rangeStart>\d+)\.\.(?<rangeEnd>\d+)(?<hostSuffix>.*?))?(?::(?<port>\d+))?$""")

    /**
     * Parse Exasol connection string (DSN), expand ranges and resolve IP addresses for all hosts
     * Return pairs of host:port tuples in random order
     * Random is necessary to guarantee the proper distribution of connections across all Exasol nodes
     */
    fun parse(dsn: String): List<ExaNodeAddress> {
        var currentPort = ExaConstant.DEFAULT_EXASOL_PORT
        val result = mutableListOf<ExaNodeAddress>()

        // Ports are applied in reverse order
        dsn.trim().split(",").reversed().forEach { part ->
            if (part.isBlank()) {
                throw ExaConnectionDsnException(connection, "Connection string is empty")
            }

            val m = dsnPattern.matcher(part)

            if (!m.matches()) {
                throw ExaConnectionDsnException(
                    connection, "Could not parse connection string part", extra = mapOf(
                        "part" to part
                    )
                )
            }

            // Optional port was specified
            m.group("port")?.let {
                currentPort = it.toInt()
            }

            // Hostname or IP address range was specified, resolve and expand it
            m.group("rangeStart")?.let {
                val hostPrefix = m.group("hostPrefix")
                val hostSuffix = m.group("hostSuffix")

                val rangeStart = m.group("rangeStart").toInt()
                val rangeEnd = m.group("rangeEnd").toInt()

                if (rangeStart > rangeEnd) {
                    throw ExaConnectionDsnException(
                        connection,
                        "Connection string part contains an invalid range, lower bound is higher than upper bound",
                        extra = mapOf(
                            "part" to part
                        )
                    )
                }

                val padLength = rangeStart.toString().length

                for (i in rangeStart..rangeEnd) {
                    val hostName = "${hostPrefix}${i.toString().padStart(padLength, '0')}${hostSuffix}"
                    result.addAll(resolveAllAddresses(hostName, currentPort))
                }
            } ?: run {
                result.addAll(resolveAllAddresses(m.group("hostPrefix"), currentPort))
            }
        }

        return result.distinct().shuffled()
    }

    private fun resolveAllAddresses(hostName: String, port: Int): List<ExaNodeAddress> {
        try {
            return InetAddress.getAllByName(hostName).map { ExaNodeAddress(it, port) }
        } catch (e: UnknownHostException) {
            throw ExaConnectionDsnException(
                connection, "Could not resolve IP address of hostname", extra = mapOf(
                    "hostName" to hostName
                )
            )
        }
    }
}
