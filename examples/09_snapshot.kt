import com.badoo.kexasol.ExaConnectionOptions
import com.badoo.kexasol.exception.ExaException
import kotlin.test.Test

class Example09Snapshot : KExasolExample() {

    /**
     * Snapshot execution mode prevents locks while querying system tables and views
     * Learn more: https://www.exasol.com/support/browse/EXASOL-2646
     */
    @Test
    fun snapshotTransaction() {
        val options = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema
        )

        // Open three separate connections
        val exa1 = options.connect()
        val exa2 = options.connect()
        val exa3 = options.connect()

        try {
            exa1.setAutocommit(false)
            exa1.execute("SELECT * FROM users_copy")
            exa1.execute("INSERT INTO payments_copy(user_id) VALUES (1)")

            exa2.execute("INSERT INTO users_copy(user_id) VALUES (1)")

            // Snapshot execution prevents lock here
            val st = exa3.execute(
                """
                SELECT column_name, column_type 
                FROM EXA_ALL_COLUMNS 
                WHERE column_schema=CURRENT_SCHEMA AND column_table='PAYMENTS_COPY'
            """, snapshotExecution = true
            )

            st.forEach { row ->
                println(row)
            }
        } catch (e: ExaException) {
            println(e)
        } finally {
            exa1.close()
            exa2.close()
            exa3.close()
        }
    }
}
