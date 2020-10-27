import com.badoo.kexasol.ExaConnectionOptions
import kotlin.test.Test

class Example05Transaction : KExasolExample() {

    /**
     * Dealing with transactions, explicit ROLLBACK and COMMIT
     */
    @Test
    fun transaction() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema
        ).connect()

        exa.use {
            exa.setAutocommit(false)

            val st1 = exa.execute("TRUNCATE TABLE users")
            println("TRUNCATE affected rows: ${st1.rowCount}")

            exa.rollback()
            println("TRUNCATE was rolled back")

            val st2 = exa.execute("SELECT count(*) FROM users")
            println("SELECT count: ${st2.onlyRow().getString(0)}")

            exa.commit()
            println("SELECT was committed")
        }
    }
}
