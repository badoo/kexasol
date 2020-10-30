import com.badoo.kexasol.ExaConnectionOptions
import com.badoo.kexasol.net.ExaNodeAddress
import java.math.BigDecimal
import java.time.LocalDateTime

import kotlin.test.Test

class Example11StreamParallelImport : KExasolExample() {
    /**
     * Ingest a large dataset in 4 parallel worker threads.
     *
     * Threads are used for example purposes only.
     * You may use completely separate processes running on separate servers to achieve linear scalability.
     */
    @Test
    fun streamParallelImport() {
        val options = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema
        )

        options.connect().use { exa ->
            // Get list of randomly shuffled addresses for specific number of workers
            val nodeAddressList = exa.streamParallelNodes(4)

            // Create child thread objects, but do not start them yet
            val childThreads = nodeAddressList.mapIndexed { idx, nodeAddress ->
                ChildImportThread(idx, options, nodeAddress)
            }

            // Get list of internal addresses from child thread objects
            val internalAddressList = childThreads.map { it.internalAddress }

            // Start child threads
            childThreads.forEach {
                it.start()
            }

            // Clean up the target table before import
            exa.execute("TRUNCATE TABLE payments_copy")

            val paymentsCols = listOf("USER_ID", "PAYMENT_ID", "PAYMENT_TS", "GROSS_AMT", "NET_AMT")

            // Run IMPORT query in the main thread
            val st = exa.streamParallelImport(internalAddressList, "payments_copy", paymentsCols)
            println("IMPORT affected rows: ${st.rowCount}")

            // Wait for child threads to finish
            childThreads.forEach {
                it.join()
            }
        }
    }

    class ChildImportThread(val idx: Int, options: ExaConnectionOptions, nodeSocketAddress: ExaNodeAddress) : Thread() {
        val worker = options.streamWorker(nodeSocketAddress)
        val internalAddress = worker.internalAddress

        override fun run() {
            try {
                val cols = listOf("USER_ID", "PAYMENT_ID", "PAYMENT_TS", "GROSS_AMT", "NET_AMT")
                var processedRows = 0L

                worker.streamImportWriter(cols) { writer ->
                    repeat(1000) {
                        writer.setLong("USER_ID", 1L)
                        writer.setString("PAYMENT_ID", "111-111-111")
                        writer.setLocalDateTime("PAYMENT_TS", LocalDateTime.parse("2020-01-01T03:04:05"))
                        writer.setBigDecimal("GROSS_AMT", BigDecimal("10.99"))
                        writer.setBigDecimal("NET_AMT", BigDecimal("8.99"))
                        writer.writeRow()

                        processedRows++
                    }
                }

                println("Child $idx finished, processed $processedRows rows")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
