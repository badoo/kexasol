## Data types

KExasol allows retrieving individual values from `ExaStatementRow` and `ExaStreamExportRow` using common getters for specific data types.

| Getter | Kotlin type | Optimal for Exasol type |
| --- | --- | --- |
| getBoolean | Kotlin [`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/) | BOOLEAN |
| getDouble | Kotlin [`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/) | DOUBLE PRECISION |
| getInt | Kotlin [`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/) | DECIMAL(<9,0) |
| getLong | Kotlin [`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/) | DECIMAL(<18,0) |
| getBigDecimal | [`java.math.BigDecimal`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/java.math.-big-decimal/) | other DECIMAL |
| getString | Kotlin [`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/) | All types |
| getLocalDate | [`java.time.LocalDate`](https://docs.oracle.com/javase/8/docs/api/java/time/LocalDate.html) | DATE |
| getLocalDateTime | [`java.time.LocalDateTime`](https://docs.oracle.com/javase/8/docs/api/java/time/LocalDateTime.html) | TIMESTAMP |

All common getters can return NULL.

Common getters can be used with column 0-based index or with column name. Please note: column names in Exasol are upper-cased in most of the cases.

For example:

```kotlin
row.getLong(0)                      // get first column as Long
row.getBigDecimal(1)                // get second column as BigDecimal
row.getString("USER_NAME")          // get column USER_NAME as String
row.getLocalDate("REGISTER_DATE")   // date column REGISTER_DATE as LocalDate
```

It is also possible to extract the whole row as a collection using special getters `asList()` and `asMap()`.

For `ExaStatementRow`:
- returned collections contain elements of data type `Any?`;
- Exasol NULL values are represented as `null`;
- Exasol BOOLEAN values are represented as Kotlin Boolean;
- All other values are represented as type String;

For `ExaStreamExportRow`:
- returned collections contain elements of data type `String?`;
- Exasol NULL values are represented as `null`;
- Exasol BOOLEAN values are represented as strings `1` and `0`;
- All others values are represented as type String;

### Writing data types

While writing data using `streamImport*` functions, very similar setters are available. For example:

```kotlin
writer.setLong(0, 1L)                                                      // set first column as Long
writer.setString("USER_NAME", "abc")                                       // set column USER_NAME as String
writer.setLocalDate("REGISTER_DATE", LocalDate.parse("2020-01-01"))        // set column REGISTER_DATE as LocalDate
writer.writeRow()                                                          // flush row
```

It is also possible to write the whole row using `List` and `Map` collections:

```kotlin
writer.writeRow(
    listOf(1L, "abc", LocalDate.parse("2020-01-01"))
)

writer.writeRow(
    mapOf(
        "USER_ID" to 1L,
        "USER_NAME" to "abc",
        "REGISTER_DATE" to LocalDate.parse("2020-01-01")
    )
)
```

### DATE and TIMESTAMP alternatives?

You are not forced to use `LocalDate` and `LocalDateTime` to represent Exasol `DATE` and `TIMESTAMP` types. You can use any other custom formatters and parsers instead. You can read and write temporal data using basic `getString()` and `setString()` and apply any required transformations.

### Examples

Please check examples [01_fetch.kt](/examples/01_fetch.kt) and [02_stream.kt](/examples/02_stream.kt) for more details.
