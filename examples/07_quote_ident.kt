import com.badoo.kexasol.ExaConnectionOptions
import kotlin.test.Test

class Example07QuoteIdent : KExasolExample() {

    /**
     * Exasol identifiers are UPPER-cased by default.
     *
     * It is highly recommended to keep it that way and avoid using special characters or lower-cased letters.
     *
     * However, if you have to deal with such identifier, please set option `quoteIdentifier=true` to use proper escaping.
     */
    @Test
    fun quoteIdent() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema,
            quoteIdentifier = true,
        ).connect()

        exa.use {
            // Camelcase table name
            exa.execute(
                "TRUNCATE TABLE {tableName!q}", mapOf(
                    "tableName" to "cameCaseName"
                )
            )

            // Camelcase column name
            val cols = listOf("USER_ID", "userName")

            val data = listOf(
                listOf(1L, "abc"),
                listOf(2L, "cde"),
                listOf(3L, "fgh"),
            )

            val importSt = exa.streamImportIterator("cameCaseName", cols, data.iterator())
            println("IMPORT affected row: ${importSt.rowCount}")

            val selectSt = exa.execute(
                "SELECT {colName1!i}, {colName2!q} FROM {tableName!q}", mapOf(
                    "colName1" to "user_id",
                    "colName2" to "userName",
                    "tableName" to "cameCaseName",
                )
            )

            selectSt.forEach { row ->
                println("USER_ID: ${row.getString("USER_ID")}, userName: ${row.getString("userName")}")
            }
        }
    }
}
