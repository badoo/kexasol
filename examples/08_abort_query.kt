import com.badoo.kexasol.ExaConnection
import com.badoo.kexasol.ExaConnectionOptions
import com.badoo.kexasol.exception.ExaQueryAbortException
import com.badoo.kexasol.exception.ExaQueryTimeoutException
import kotlin.test.Test

class Example08AbortQuery : KExasolExample() {

    /**
     * Stop long query automatically using query timeout
     */
    @Test
    fun queryTimeout() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema
        ).connect()

        exa.use {
            //3 seconds timeout
            exa.setQueryTimeout(3)

            try {
                // Run heavy query
                exa.execute("SELECT * FROM users a, users b, users c, payments d")
            } catch (e: ExaQueryTimeoutException) {
                println(e)
            }
        }
    }

    /**
     * It is possible to abort currently running query from another thread
     */
    @Test
    fun abortQuery() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema,
        ).connect()

        exa.use {
            // Run query in another thread
            val queryThread = QueryThread(exa)
            queryThread.start()

            // Wait for 2 seconds
            Thread.sleep(2000)

            // Terminate query in the main thread
            exa.abortQuery()

            queryThread.join()
        }
    }

    class QueryThread(val exa: ExaConnection) : Thread() {
        override fun run() {
            try {
                // Run heavy query
                exa.execute("SELECT * FROM users a, users b, users c, payments d")
            } catch (e: ExaQueryAbortException) {
                println(e)
            }
        }
    }
}
