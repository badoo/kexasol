import com.badoo.kexasol.ExaConnectionOptions
import com.badoo.kexasol.net.ExaNodeAddress

import kotlin.test.Test

class Example10StreamParallelExport : KExasolExample() {
    /**
     * Fetch a large dataset in 4 parallel worker threads.
     *
     * Threads are used for example purposes only.
     * You may use completely separate processes running on separate servers to achieve linear scalability.
     */
    @Test
    fun streamParallelExport() {
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
                ChildExportThread(idx, options, nodeAddress)
            }

            // Get list of internal addresses from child thread objects
            val internalAddressList = childThreads.map { it.internalAddress }

            // Start child threads
            childThreads.forEach {
                it.start()
            }

            // Run EXPORT query in the main thread
            val st = exa.streamParallelExport(internalAddressList, "SELECT * FROM payments")
            println("EXPORT affected rows: ${st.rowCount}")

            // Wait for child threads to finish
            childThreads.forEach {
                it.join()
            }
        }
    }

    class ChildExportThread(val idx: Int, options: ExaConnectionOptions, nodeSocketAddress: ExaNodeAddress) : Thread() {
        val worker = options.streamWorker(nodeSocketAddress)
        val internalAddress = worker.internalAddress

        override fun run() {
            try {
                var processedRows = 0L

                worker.streamExportReader { source ->
                    source.forEach {
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
