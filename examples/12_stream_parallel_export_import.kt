import com.badoo.kexasol.ExaConnectionOptions
import com.badoo.kexasol.mode.ExaEncryptionMode
import com.badoo.kexasol.net.ExaNodeAddress
import kotlin.test.Test

class Example12StreamParallelExportImport : KExasolExample() {
    /**
     * Read a large dataset, process it and ingest results into a different table.
     * Do it in 4 parallel threads.
     *
     * Threads are used for example purposes only.
     * You may use completely separate processes running on separate servers to achieve linear scalability.
     */
    @Test
    fun streamParallelExportImport() {
        val options = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema,
            compression = true,
            encryption = ExaEncryptionMode.ENABLED_NO_CERT,
        )

        options.connect().use { exa ->
            // Get list of randomly shuffled addresses for specific number of workers
            val nodeAddressList = exa.streamParallelNodes(4)

            // Create child thread objects, but do not start them yet
            val childThreads = nodeAddressList.mapIndexed { idx, nodeAddress ->
                ChildExportImportThread(idx, options, nodeAddress)
            }

            // Get list of internal addresses from child thread objects
            val exportInternalAddressList = childThreads.map { it.exportInternalAddress }
            val importInternalAddressList = childThreads.map { it.importInternalAddress }

            // Start child threads
            childThreads.forEach {
                it.start()
            }

            // Run EXPORT query in the main thread
            val exportSt = exa.streamParallelExport(exportInternalAddressList, "SELECT * FROM payments")
            println("EXPORT affected rows: ${exportSt.rowCount}")

            // Clean up the target table before import
            exa.execute("TRUNCATE TABLE payments_copy")

            val paymentsCols = listOf("USER_ID", "PAYMENT_ID", "PAYMENT_TS", "GROSS_AMT", "NET_AMT")

            // Run IMPORT query in the main thread
            val importSt = exa.streamParallelImport(importInternalAddressList, "payments_copy", paymentsCols)
            println("IMPORT affected rows: ${importSt.rowCount}")

            // Wait for child threads to finish
            childThreads.forEach {
                it.join()
            }
        }
    }

    class ChildExportImportThread(val idx: Int, options: ExaConnectionOptions, nodeSocketAddress: ExaNodeAddress) : Thread() {
        val exportWorker = options.streamWorker(nodeSocketAddress)
        val exportInternalAddress = exportWorker.internalAddress

        val importWorker = options.streamWorker(nodeSocketAddress)
        val importInternalAddress = importWorker.internalAddress

        override fun run() {
            try {
                var exportProcessedRows = 0L
                val rowList = mutableListOf<Map<String, String?>>()

                exportWorker.streamExportReader { reader ->
                    reader.forEach { row ->
                        exportProcessedRows++
                        rowList.add(row.asMap())
                    }
                }

                println("Child $idx EXPORT finished, processed $exportProcessedRows rows")

                //------
                // do something with data here
                //------

                var importProcessedRows = 0L
                val cols = listOf("USER_ID", "PAYMENT_ID", "PAYMENT_TS", "GROSS_AMT", "NET_AMT")

                importWorker.streamImportWriter(cols) { writer ->
                    rowList.forEach {
                        writer.writeRow(it)
                        importProcessedRows++
                    }
                }

                println("Child $idx IMPORT finished, processed $importProcessedRows rows")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
