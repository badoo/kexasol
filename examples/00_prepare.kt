import com.badoo.kexasol.ExaConnectionOptions
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.test.Test

class Example00Prepare : KExasolExample() {
    /**
     * Prepare dataset for all other examples
     */
    @Test
    fun prepare() {
        ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            autocommit = false
        ).connect().use { exa ->

            // Prepare schema

            exa.execute(
                "CREATE SCHEMA IF NOT EXISTS {schemaName!i}", mapOf(
                    "schemaName" to credentials.schema
                )
            )

            exa.openSchema(credentials.schema)

            // Prepare tables

            exa.execute(
                """
                CREATE OR REPLACE TABLE users
                (
                    user_id         DECIMAL(18,0),
                    user_name       VARCHAR(255),
                    register_dt     DATE,
                    last_visit_ts   TIMESTAMP,
                    is_active       BOOLEAN,
                    user_rating     DECIMAL(10,5),
                    game_score      DOUBLE,
                    status          VARCHAR(50)
                )
            """
            )

            exa.execute(
                """
                CREATE OR REPLACE TABLE payments
                (
                    user_id         DECIMAL(18,0),
                    payment_id      VARCHAR(255),
                    payment_ts      TIMESTAMP,
                    gross_amt       DECIMAL(15,5),
                    net_amt         DECIMAL(15,5)
                )
            """
            )

            exa.execute("CREATE OR REPLACE TABLE users_copy LIKE users")
            exa.execute("CREATE OR REPLACE TABLE payments_copy LIKE payments")

            exa.execute(
                """
                CREATE OR REPLACE TABLE edge_case
                (
                    dec36_0         DECIMAL(36,0),
                    dec36_36        DECIMAL(36,36),
                    dbl             DOUBLE,
                    bl              BOOLEAN,
                    dt              DATE,
                    ts              TIMESTAMP,
                    var100          VARCHAR(100),
                    var2000000      VARCHAR(2000000)
                )
            """
            )

            exa.execute(
                """
                CREATE OR REPLACE TABLE "cameCaseName"
                (
                    user_id         DECIMAL(18,0),
                    "userName"      VARCHAR(255)
                )
            """
            )

            // Generate data
            val usersCols =
                listOf("USER_ID", "USER_NAME", "REGISTER_DT", "LAST_VISIT_TS", "IS_ACTIVE", "USER_RATING", "GAME_SCORE", "STATUS")
            val userStatuses = listOf("ACTIVE", "PENDING", "SUSPENDED", "DISABLED")

            exa.streamImportWriter("users", usersCols) { writer ->
                repeat(10000) { idx ->
                    writer.setLong("USER_ID", idx + 1L)
                    writer.setString("USER_NAME", randomString())
                    writer.setLocalDate("REGISTER_DT", randomDate())
                    writer.setLocalDateTime("LAST_VISIT_TS", randomDateTime())
                    writer.setBoolean("IS_ACTIVE", Random.nextBoolean())
                    writer.setBigDecimal(
                        "USER_RATING",
                        BigDecimal(Random.nextLong(1L, 100L)).divide(BigDecimal(100L), 2, RoundingMode.HALF_DOWN)
                    )
                    writer.setDouble(
                        "GAME_SCORE", when (Random.nextLong(1L, 10L)) {
                            10L -> null
                            else -> Random.nextDouble(0.001, 100.000)
                        }
                    )
                    writer.setString("STATUS", userStatuses.random())
                    writer.writeRow()
                }
            }

            val paymentsCols = listOf("USER_ID", "PAYMENT_ID", "PAYMENT_TS", "GROSS_AMT", "NET_AMT")

            exa.streamImportWriter("payments", paymentsCols) { writer ->
                repeat(100000) {
                    val grossAmt = Random.nextDouble(1.0, 10000.00) / 100
                    val netAmt = grossAmt * 0.7

                    writer.setLong("USER_ID", Random.nextLong(1L, 10000L))
                    writer.setString(
                        "PAYMENT_ID", listOf(
                            Random.nextLong(1L, 300L),
                            Random.nextLong(1L, 300L),
                            Random.nextLong(1L, 300L)
                        ).joinToString("-")
                    )
                    writer.setLocalDateTime("PAYMENT_TS", randomDateTime())
                    writer.setBigDecimal("GROSS_AMT", grossAmt.toBigDecimal())
                    writer.setBigDecimal("NET_AMT", netAmt.toBigDecimal())
                    writer.writeRow()
                }
            }

            exa.commit()

            println("Data in schema [${credentials.schema}] was prepared")
        }
    }

    private fun randomString(): String {
        val randomChars = ('a'..'z') + ('0'..'9')
        return (1..10).map { randomChars.random() }.joinToString("")
    }

    private fun randomDate(): LocalDate {
        return LocalDate.of(2020, 1, 1).plusDays(Random.nextLong(0L, 365L))
    }

    private fun randomDateTime(): LocalDateTime {
        return LocalDateTime.of(2020, 1, 1, 0, 0, 0).plusSeconds(Random.nextLong(0L, 365L * 24L * 60L * 60L))
    }
}
