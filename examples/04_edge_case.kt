import com.badoo.kexasol.ExaConnectionOptions
import com.badoo.kexasol.enum.ExaEncryptionMode
import com.badoo.kexasol.stream.ExaStreamCSVReader
import com.badoo.kexasol.stream.ExaStreamCSVWriter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test


class Example04EdgeCase : KExasolExample() {
    /**
     * Insert lowest and highest values for each of the core data types
     * And SELECT + fetch it back
     */
    @Test
    fun edgeCaseFetch() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema,
            compression = true
        ).connect()

        exa.use {
            val insertQuery = "INSERT INTO edge_case VALUES ({dec36_0!d}, {dec36_36!d}, {dbl!f}, {bl}, {dt}, {ts}, {var100}, {var2000000})"
            val selectQuery = "SELECT dec36_0, dec36_36, dbl, bl, dt, ts, var100, LENGTH(var2000000) AS len_var FROM edge_case"

            exa.execute("TRUNCATE TABLE edge_case")

            exa.execute(insertQuery, edgeCaseData[0])
            exa.execute(insertQuery, edgeCaseData[1])
            exa.execute(insertQuery, edgeCaseData[2])

            val st = exa.execute(selectQuery)

            st.forEach { row ->
                println(row.asMap())
            }
        }
    }

    /**
     * IMPORT lowest and highest values for each of the core data types
     * And EXPORT it back
     */
    @Test
    fun edgeCaseStream() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema,
            compression = true
        ).connect()

        exa.use {
            val cols = listOf("dec36_0", "dec36_36", "dbl", "bl", "dt", "ts", "var100", "var2000000")

            exa.streamImport("edge_case", cols) { streamSink ->
                ExaStreamCSVWriter(cols, streamSink).use { writer ->
                    writer.writeRow(edgeCaseData[0])
                    writer.writeRow(edgeCaseData[1])
                    writer.writeRow(edgeCaseData[2])
                }
            }

            exa.streamExport("SELECT dec36_0, dec36_36, dbl, bl, dt, ts, var100, LENGTH(var2000000) AS len_var FROM edge_case") { streamSource ->
                ExaStreamCSVReader(streamSource).use { reader ->
                    reader.forEach { row ->
                        println(row.asMap())
                    }
                }
            }
        }
    }

    /**
     * Query longer than 2M chars
     */
    @Test
    fun edgeCaseLongQuery() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema,
            compression = true,
            encryption = ExaEncryptionMode.ENABLED_NO_CERT,
        ).connect()

        exa.use {
            val st = exa.execute(
                "SELECT {val1} AS val1, {val2} AS val2, {val3} AS val3, {val4} AS val4, {val5} AS val5", mapOf(
                    "val1" to edgeCaseData[0]["var2000000"],
                    "val2" to edgeCaseData[0]["var2000000"],
                    "val3" to edgeCaseData[0]["var2000000"],
                    "val4" to edgeCaseData[0]["var2000000"],
                    "val5" to edgeCaseData[0]["var2000000"],
                )
            )

            println("Query length: ${st.formattedQuery.length}")
            println("Result column length: ${st.onlyRow().getString(0)?.length}")
        }
    }

    private val edgeCaseData = listOf(
        mapOf(
            "dec36_0" to BigDecimal("+${"9".repeat(36)}"),
            "dec36_36" to BigDecimal("+0.${"9".repeat(36)}"),
            "dbl" to 1.7e308,
            "bl" to true,
            "dt" to LocalDate.parse("9999-12-31"),
            "ts" to LocalDateTime.parse("9999-12-31T23:59:59.999"),
            "var100" to "a".repeat(100),
            "var2000000" to "ひ".repeat(2000000),
        ),
        mapOf(
            "dec36_0" to BigDecimal("-${"9".repeat(36)}"),
            "dec36_36" to BigDecimal("-0.${"9".repeat(36)}"),
            "dbl" to -1.7e308,
            "bl" to false,
            "dt" to LocalDate.parse("0001-01-01"),
            "ts" to LocalDateTime.parse("0001-01-01T00:00:00.000"),
            "var100" to "",
            "var2000000" to "ひ",
        ),
        mapOf(
            "dec36_0" to null,
            "dec36_36" to null,
            "dbl" to null,
            "bl" to null,
            "dt" to null,
            "ts" to null,
            "var100" to null,
            "var2000000" to null,
        ),
    )
}
