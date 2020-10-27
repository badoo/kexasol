import com.badoo.kexasol.ExaConnectionOptions
import com.badoo.kexasol.enum.ExaEncryptionMode
import com.badoo.kexasol.exception.ExaStreamException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test

class Example02Stream : KExasolExample() {
    /**
     * Fetch large dataset as raw CSV stream
     */
    @Test
    fun streamExportRaw() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema,
        ).connect()

        exa.streamExport(" SELECT * FROM users ORDER BY user_id LIMIT 3") { source ->
            //first line is header
            println(source.readUtf8Line())

            //other lines represent data
            println(source.readUtf8Line())
            println(source.readUtf8Line())
            println(source.readUtf8Line())
        }
    }

    /**
     * Fetch large dataset with CSV reader
     */
    @Test
    fun streamExportReader() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema,
            compression = true,
        ).connect()

        try {
            exa.streamExportReader("SELECT * FROM users ORDER BY user_id LIMIT 3") { reader ->
                reader.forEach { row ->
                    //Access column by index
                    println("USER_ID (Long): ${row.getLong(0)}, USER_NAME (String): ${row.getString("USER_NAME")}")
                }
            }
        } catch (e: ExaStreamException) {
            e.printStackTrace()
        }
    }

    /**
     * Import large dataset as raw CSV stream
     */
    @Test
    fun streamImportRaw() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema,
            encryption = ExaEncryptionMode.ENABLED_NO_CERT,
        ).connect()

        val cols = listOf(
            "USER_ID", "USER_NAME", "REGISTER_DT", "LAST_VISIT_TS", "IS_ACTIVE", "STATUS", "USER_RATING"
        )

        val st = exa.streamImport("users_copy", cols) { sink ->
            // write raw CSV lines
            sink.writeUtf8("1,abc,2020-01-01,\"2020-02-01 03:04:05\",0,ACTIVE,15.55\n")
            sink.writeUtf8("2,cde,2020-01-02,\"2020-02-02 03:04:05\",1,DELETED,25.55\n")

            // null values are represented as empty strings
            sink.writeUtf8("3,,,,,,\n")
        }

        println("IMPORT affected rows: ${st.rowCount}")
    }

    /**
     * Import large dataset with CSV writer
     */
    @Test
    fun streamImportWriter() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema,
            compression = true,
            encryption = ExaEncryptionMode.ENABLED_NO_CERT,
        ).connect()

        val cols = listOf(
            "USER_ID", "USER_NAME", "REGISTER_DT", "IS_ACTIVE", "LAST_VISIT_TS", "STATUS", "USER_RATING"
        )

        val st = exa.streamImportWriter("users_copy", cols) { writer ->
            // Set individual columns by index or column name, flush with .writeRow() call
            writer.setLong(0, 1)
            writer.setString("USER_NAME", "abc")
            writer.setLocalDate("REGISTER_DT", LocalDate.parse("2020-01-01"))
            writer.setBoolean(3, false)
            writer.setLocalDateTime(4, LocalDateTime.parse("2020-02-01T03:04:05"))
            writer.setString("STATUS", "ACTIVE")
            writer.setBigDecimal("USER_RATING", BigDecimal("15.55"))
            writer.writeRow()

            // Write entire row from ordered List
            val list: List<Any?> = listOf(
                2L,
                "cde",
                LocalDate.parse("2020-01-02"),
                true,
                LocalDateTime.parse("2020-02-02T03:04:05"),
                "DELETED",
                BigDecimal("25.55")
            )

            writer.writeRow(list)

            // Write an entire row from Map
            val map: Map<String, Any?> = mapOf(
                "USER_ID" to 3L,
                "USER_NAME" to "cde",
                "REGISTER_DT" to LocalDate.parse("2020-01-02"),
                "IS_ACTIVE" to true,
                "LAST_VISIT_TS" to LocalDateTime.parse("2020-02-02T03:04:05"),
                "STATUS" to "DELETED",
                "USER_RATING" to BigDecimal("25.55")
            )

            writer.writeRow(map)
        }

        println("IMPORT affected rows: ${st.rowCount}")
    }

    /**
     * Import large dataset from Iterable emitting lists
     */
    @Test
    fun streamImportIteratorList() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema
        ).connect()

        val cols = listOf(
            "USER_ID", "USER_NAME", "REGISTER_DT", "IS_ACTIVE", "STATUS"
        )

        // Easy import from basic list of lists
        val listOfLists = listOf<List<Any?>>(
            listOf(1L, "abc", LocalDate.parse("2020-01-01"), false, "ACTIVE"),
            listOf(2L, "cde", LocalDate.parse("2020-01-02"), true, "DELETED"),
        )

        val st = exa.streamImportIterator("users_copy", cols, listOfLists.iterator())
        println("IMPORT affected rows: ${st.rowCount}")
    }

    /**
     * Import large dataset from Iterable emitting maps
     */
    @Test
    fun streamImportIteratorSequence() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema
        ).connect()

        val cols = listOf(
            "USER_ID", "USER_NAME", "REGISTER_DT", "IS_ACTIVE", "STATUS"
        )

        // Import from sequence
        val sequenceOfMaps = sequence {
            yield(
                mapOf(
                    "USER_ID" to 1L,
                    "USER_NAME" to "abc",
                    "REGISTER_DT" to LocalDate.parse("2020-01-01"),
                    "IS_ACTIVE" to false,
                    "STATUS" to "ACTIVE",
                )
            )

            yield(
                mapOf(
                    "USER_ID" to 2L,
                    "USER_NAME" to "cde",
                    "REGISTER_DT" to LocalDate.parse("2020-01-02"),
                    "IS_ACTIVE" to true,
                    "STATUS" to "DELETED",
                )
            )
        }

        val st = exa.streamImportIterator("users_copy", cols, sequenceOfMaps.iterator())
        println("IMPORT affected rows: ${st.rowCount}")
    }
}
