## SQL Formatting

KExasol provides custom Exasol-specific formatter.

You are not forced to use this formatter. You may use any other formatter you like and pass the final result into `execute()` and other relevant functions.

## Types of placeholders

Formatter supports only named placeholders with optional type. Positional placeholders are not supported on purpose. It helps to reduce the probability of human mistake and improves code clarity.

Examples of placeholder syntax:

```
{foo} - named placeholder with default string quoting
{foo!d} - named placeholder with type "safe decimal"
{foo!i[]} - named placeholder with type "safe identifier" and multiple comma-separated values
```

If type was not defined, formatter assumes it is a string value by default (`!s`).

| Conversion  | Description |
| --- | --- |
| `!s` | (default) cast value to String, escape single quotes and wrap it with single quotes |
| `!d` | assert if value can be safely represented as Exasol exact DECIMAL, pass it without changes |
| `!f` | assert if value can be safely represented as Exasol DOUBLE PRECISION with exponent, pass it without changes |
| `!i` | assert if value can be safely represented as Exasol identifier (e.g. table name), pass it without changes |
| `!q` | escape double quotes in value and wrap it with double quotes, suitable for Exasol identifier with special characters |
| `!r` | (unsafe) pass value without any checks, suitable for raw SQL parts (e.g. dynamic sub-queries) |

Types `!s`, `!d`, `!f` convert `null` value into `NULL` String without quotes.

All placeholders must have a corresponding key in `queryParams` map. All keys in `queryParams` map mush have a corresponding placeholder.

Please see example [03_format.kt](/examples/03_format.kt) for more details.

## Complete example

```kotlin
val queryParams = mapOf(
    "stringValue" to "abc",
    "nullValue" to null,
    "tableName1" to "users",
    "schemaName2" to "KEXASOL_TEST",
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
```

Result:

```sql
SELECT 'abc' AS random_value
    , NULL AS null_value
    , u.user_id
    , sum(gross_amt) AS gross_amt
FROM users u
    JOIN KEXASOL_TEST."PAYMENTS" p ON (u.user_id=p.user_id)
WHERE u.user_rating >= 0.5
    AND u.game_score > 10.0
    AND u.is_active IS TRUE
    AND u.status IN ('ACTIVE', 'PASSIVE', 'SUSPENDED')
    AND u.user_rating NOT IN (10, 20)
    AND u.register_dt >= '2020-01-01'
    AND u.last_visit_ts < '2025-01-01 03:04:05.'
    AND u.user_name LIKE '%a\_b\%c%'
GROUP BY 1,2,3
ORDER BY 4 DESC
LIMIT 10
```