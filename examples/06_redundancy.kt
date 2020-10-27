import com.badoo.kexasol.ExaConnectionOptions
import kotlin.test.Test

class Example06Redundancy : KExasolExample() {

    /**
     * Similar to standard JDBC / ODBC drivers, KExasol iterates over all IP addresses resolved from DSN,
     * until it finds a working node or runs out of addresses to check
     */
    @Test
    fun redundancy() {
        val exa = ExaConnectionOptions(
            dsn = "0.42.42.40..49,${credentials.dsn}",
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema
        ).connect()

        println("Connection was successful after ${exa.connectFailedAttempts.size} attempts, it took ${exa.connectTimeMillis}ms")

        exa.connectFailedAttempts.forEach {
            println("${it.key}: ${it.value.message}")
        }
    }
}
