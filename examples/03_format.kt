import com.badoo.kexasol.ExaConnectionOptions
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test

class Example03Format : KExasolExample() {
    /**
     * Format complex query using placeholders and queryParams
     */
    @Test
    fun formatExecute() {
        val exa = ExaConnectionOptions(
            dsn = credentials.dsn,
            user = credentials.user,
            password = credentials.password,
            schema = credentials.schema
        ).connect()

        val queryParams = mapOf(
            "stringValue" to "abc",
            "nullValue" to null,
            "tableName1" to "users",
            "schemaName2" to credentials.schema,
            "tableName2" to "PAYMENTS",
            "userRating" to BigDecimal("0.5"),
            "gameScore" to 1e1,
            "isActive" to "TRUE",
            "userStatuses" to listOf("ACTIVE", "PASSIVE", "SUSPENDED"),
            "excludeUserRating" to listOf(10, 20),
            "minRegisterDate" to LocalDate.parse("2020-01-01"),
            "maxLastVisitTs" to LocalDateTime.parse("2025-01-01T03:04:05"),
            "userNameLike" to "%${exa.queryFormatter.escapeLikePattern("a_b%c")}%",
            "limit" to 10
        )

        val st = exa.execute(
            """
            SELECT {stringValue} AS random_value
                , {nullValue} AS null_value
                , u.user_id
                , sum(gross_amt) AS gross_amt
            FROM {tableName1!i} u
                JOIN {schemaName2!i}.{tableName2!q} p ON (u.user_id=p.user_id)
            WHERE u.user_rating >= {userRating!d}
                AND u.game_score > {gameScore!f}
                AND u.is_active IS {isActive!r}
                AND u.status IN ({userStatuses!s[]})
                AND u.user_rating NOT IN ({excludeUserRating!d[]})
                AND u.register_dt >= {minRegisterDate}
                AND u.last_visit_ts < {maxLastVisitTs}
                AND u.user_name LIKE {userNameLike}
            GROUP BY 1,2,3
            ORDER BY 4 DESC
            LIMIT {limit!d}
        """, queryParams
        )

        println(st.formattedQuery)
    }
}
