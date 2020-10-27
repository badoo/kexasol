import com.badoo.kexasol.ExaConnectionOptions
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test

class Example01Fetch : KExasolExample() {
    /**
     * Fetch result set using basic iteration in forEach block
     */
    @Test
    fun fetchRow() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema
        ).connect()

        exa.use {
            val st = exa.execute(
                """
                SELECT user_id
                    , user_name
                    , register_dt
                    , last_visit_ts
                    , is_active
                    , user_rating
                    , game_score
                    , status
                FROM users
                ORDER BY user_id
                LIMIT 2
            """
            )

            st.forEach { row ->
                //Access column by index
                println("USER_ID (Long): ${row.getLong(0)}")

                //Access column by name
                println("USER_NAME (String): ${row.getString("USER_NAME")}")

                //Get LocalDate using built-in formatter
                println("REGISTER_DT (LocalDate): ${row.getLocalDate("REGISTER_DT")}")

                //Get LocalDateTime using built-in formatter
                println("LAST_VISIT_TS (LocalDateTime): ${row.getLocalDateTime("LAST_VISIT_TS")}")

                //Get Boolean
                println("IS_ACTIVE (Boolean): ${row.getBoolean("IS_ACTIVE")}")

                //Get BigDecimal
                println("USER_RATING (BigDecimal): ${row.getBigDecimal("USER_RATING")}")

                //Get Double
                println("GAME_SCORE (Double): ${row.getDouble("GAME_SCORE")}")

                //Get the whole row as List<Any?>
                println("ROW (List<Any?>): ${row.asList()}")

                //Get the whole row as Map<String,Any?>
                println("ROW (Map<String,Any?>): ${row.asMap()}")

                println("--- END OF ROW ---")
            }
        }
    }

    /**
     * Fetch result set as Sequence, apply data class
     */
    @Test
    fun fetchObject() {
        data class User(
            val userId: Long?,
            val userName: String?,
            val registerDt: LocalDate?,
            val lastVisitTs: LocalDateTime?,
            val isFemale: Boolean?,
            val userRating: BigDecimal?,
            val userScore: Double?,
            val status: String?
        )

        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema
        ).connect()

        exa.use {
            val st = exa.execute(
                """
                SELECT user_id
                    , user_name
                    , register_dt
                    , last_visit_ts
                    , is_active
                    , user_rating
                    , game_score
                    , status
                FROM users
                ORDER BY user_id
                LIMIT 100
            """
            )

            //Convert rows into data class objects using Sequence.map
            st.asSequence().map { row ->
                User(
                    row.getLong("USER_ID"),
                    row.getString("USER_NAME"),
                    row.getLocalDate("REGISTER_DT"),
                    row.getLocalDateTime("LAST_VISIT_TS"),
                    row.getBoolean("IS_ACTIVE"),
                    row.getBigDecimal("USER_RATING"),
                    row.getDouble("GAME_SCORE"),
                    row.getString("STATUS")
                )
            }.filter {
                it.userRating!! >= BigDecimal("0.5")
            }.take(3).forEach {
                println(it)
            }
        }
    }

    /**
     * Fetch result set with exactly one row using .onlyRow()
     */
    @Test
    fun fetchOnlyRow() {
        ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema
        ).connect().use { exa ->

            val st = exa.execute(
                """
                SELECT count(user_id) AS cnt
                    , avg(length(game_score)) AS avg_game_score
                FROM users
            """
            )

            st.onlyRow().let { row ->
                println("CNT (Long): ${row.getLong(0)}")
                println("AVG_GAME_SCORE (Double): ${row.getDouble(1)}")
            }
        }
    }

    /**
     * Fetch result set with exactly one row and one column
     */
    @Test
    fun fetchSingleValue() {
        ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema
        ).connect().use { exa ->

            val st = exa.execute(
                """
                SELECT CURRENT_USER
            """
            )

            println(st.onlyRow().getString(0))
        }
    }
}
