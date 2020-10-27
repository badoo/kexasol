package com.badoo.kexasol.format

import com.badoo.kexasol.ExaConnection
import com.badoo.kexasol.exception.ExaFormatterException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.regex.Matcher
import java.util.regex.Pattern


class ExaQueryFormatter(val connection: ExaConnection) {
    private val placeholderPattern = Pattern.compile("""\{([A-Za-z0-9_]+)(?:!([a-z])(\[])?)?}""")
    private val safeIdentifierPattern = Pattern.compile("""^[A-Za-z_][A-Za-z0-9_]*$""")

    /**
     * Format SQL query using named and typed placeholders.
     *
     * Examples placeholder syntax:
     * - `{name}` -> named single value placeholder;
     * - `{name!s}` -> named single value placeholder with type;
     * - `{name!s[]} -> named multi-value placeholder with type;
     *
     * Placeholder types:
     * - `!s` -> (default) cast value to String, escape single quotes and wrap it with single quotes;
     * - `!d` -> assert if value can be safely represented as Exasol exact DECIMAL, pass it without changes;
     * - `!f` -> assert if value can be safely represented as Exasol DOUBLE PRECISION with exponent, pass it without changes;
     * - `!i` -> assert if value can be safely represented as Exasol identifier (e.g. table name), pass it without changes;
     * - `!q` -> escape double quotes in value and wrap it with double quotes, suitable for Exasol identifier with special characters;
     * - `!r` -> (unsafe) pass value without any checks, suitable for raw SQL parts (e.g. dynamic sub-queries);
     *
     *
     * Types `!s`, `!d`, `!f` convert `null` value into `NULL` String without quotes.
     *
     * All placeholders must have a corresponding key in [queryParams] map. All keys in [queryParams] map mush have a corresponding placeholder.
     * Positional placeholders are not supported. All these measures are required to reduce possibility of human mistakes.
     *
     * Please see example `03_format.kt` for more details.
     *
     * @param[query] SQL query with named and typed placeholders
     * @param[queryParams] Map of names and substitution values for SQL query placeholders
     *
     * @throws ExaFormatterException
     * @return formatted SQL query
     */
    fun formatQuery(query: String, queryParams: Map<String, Any?>): String {
        val m = placeholderPattern.matcher(query)
        val b = StringBuilder()

        val usedParams = mutableSetOf<String>()

        while (m.find()) {
            val paramName = m.group(1)
            val type = m.group(2) ?: "s"
            val multi = m.group(3) != null

            if (!queryParams.containsKey(paramName)) {
                throw ExaFormatterException(connection, "Query parameter [$paramName] not found")
            }

            val paramValue = queryParams[paramName]

            if (multi) {
                if (paramValue is List<Any?>) {
                    if (paramValue.isNotEmpty()) {
                        m.appendReplacement(b, Matcher.quoteReplacement(paramValue.joinToString(", ") { v -> resolvePlaceholder(type, paramName, v) }))
                    } else {
                        throw ExaFormatterException(connection, "Query parameter [$paramName] is defined as array, but the List is empty")
                    }
                } else {
                    throw ExaFormatterException(connection, "Query parameter [$paramName] is defined as array, but value is not a List")
                }
            } else {
                m.appendReplacement(b, Matcher.quoteReplacement(resolvePlaceholder(type, paramName, paramValue)))
            }

            usedParams.add(paramName)
        }

        if (queryParams.keys.size > usedParams.size) {
            throw ExaFormatterException(connection, "Query parameters [${queryParams.keys.minus(usedParams).joinToString(", ")} are present in queryParams map, but not in query")
        }

        return m.appendTail(b).toString()
    }

    private fun resolvePlaceholder(type: String, paramName: String, paramValue: Any?): String {
        return when (type) {
            "s" -> quoteValue(paramValue)
            "d" -> safeDecimal(paramValue)
            "f" -> safeFloat(paramValue)
            "i" -> safeIdentifier(paramValue as String)
            "q" -> quoteIdentifier(paramValue as String)
            "r" -> paramValue as String
            else -> throw ExaFormatterException(connection, "Unknown placeholder type [$type] for parameter [$paramName]")
        }
    }

    /**
     * Remove spaces and newlines from the beginning of SQL query.
     * Remove spaces, newlines and semicolons from the end of SQL query.
     *
     * @param[query] SQL query
     *
     * @return trimmed SQL query
     */
    fun trimQuery(query: String): String {
        return query.trimStart(' ', '\n').trimEnd(' ', '\n', ';')
    }

    /**
     * [trimQuery], followed by [formatQuery] if [queryParams] are present.
     *
     * @param[query] SQL query with named and typed placeholders
     * @param[queryParams] Map of names and substitution values for placeholders
     *
     * @throws ExaFormatterException
     * @return trimmed and formatted SQL query
     */
    fun trimAndFormatQuery(query: String, queryParams: Map<String, Any?>?): String {
        val trimmedQuery = trimQuery(query)

        if (queryParams == null) {
            return trimmedQuery
        }

        return formatQuery(trimmedQuery, queryParams)
    }

    /**
     * Apply default Exasol identifier formatting according to connection option `quoteIdentifier`.
     *
     * @param[value] Raw identifier string
     * @return formatted or validated identifier for SQL query
     */
    fun formatIdentifier(value: String): String {
        return when (connection.options.quoteIdentifier) {
            true -> quoteIdentifier(value)
            false -> safeIdentifier(value)
        }
    }

    /**
     * Exasol escaping for string values, replace single quote `'` with two single quotes `''`.
     *
     * @param[value] String value
     * @return escaped string value
     */
    fun escapeString(value: String): String {
        return value.replace("'", "''")
    }

    /**
     * Exasol escaping for LIKE-patterns, prepend characters `\`, `%`, `_` with extra `\` to remove special meaning.
     *
     * @param[value] String value to be used in LIKE-pattern
     * @return escaped string value
     */
    fun escapeLikePattern(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    /**
     * Exasol escaping for identifiers, replace double quote `"` with two double quotes `""`.
     *
     * @param[value] String value
     * @return escaped string value
     */
    fun escapeIdentifier(value: String): String {
        return value.replace("\"", "\"\"")
    }

    /**
     * Convert object to string, [escapeString] it and wrap into single quotes `'`.
     *
     * NULL input values are returned as "NULL" string without quotes.
     *
     * @param[value] Object or string
     * @return string representation of object prepared for SQL query
     */
    fun quoteValue(value: Any?): String {
        if (value == null) {
            return "NULL"
        }

        val strValue = when (value) {
            is String -> value
            is LocalDate -> ExaDateTimeFormatter.formatLocalDate(value)
            is LocalDateTime -> ExaDateTimeFormatter.formatLocalDateTime(value)
            is BigDecimal -> value.toPlainString()
            else -> value.toString()
        }

        return "'${escapeString(strValue)}'"
    }

    /**
     * Escape identifier via [escapeIdentifier] it and wrap into single quotes `'`.
     *
     * @param[value] Identifier string value
     * @return identifier prepared for SQL query
     */
    fun quoteIdentifier(value: String): String {
        return "\"${escapeIdentifier(value)}\""
    }

    /**
     * Assert if input value can be represented safely as Exasol exact DECIMAL value.
     *
     * Allowed input types: `Short`, `Int`, `Long`, `BigDecimal`.
     *
     * `BigDecimal` is converted using method `.toPlainString()` to get rid of exponent part. `Double` is not allowed because it is not an "exact" type.
     *
     * NULL input values are returned as "NULL" string without quotes
     *
     * @param[value] Numeric value
     *
     * @throws ExaFormatterException
     * @return safe numeric value prepared for SQL query
     */
    fun safeDecimal(value: Any?): String {
        if (value == null) {
            return "NULL"
        }

        return when (value) {
            is Short -> value.toString()
            is Int -> value.toString()
            is Long -> value.toString()
            is BigDecimal -> value.toPlainString()
            else -> throw ExaFormatterException(connection, "Parameter value [$value] is not a safe decimal")
        }
    }

    /**
     * Assert if input value can be represented safely as Exasol DOUBLE PRECISION value with exponent.
     *
     * Allowed input types: `Double`, `BigDecimal`.
     *
     * `BigDecimal` is converted using method `.toString()` to keep the exponent part.
     *
     * NULL input values are returned as "NULL" string without quotes
     *
     * @param[value] Numeric value with
     *
     * @throws ExaFormatterException
     * @return safe numeric value prepared for SQL query
     */
    fun safeFloat(value: Any?): String {
        if (value == null) {
            return "NULL"
        }

        return when (value) {
            is Short -> value.toString()
            is Int -> value.toString()
            is Long -> value.toString()
            is Double -> value.toString()
            is BigDecimal -> value.toString()
            else -> throw ExaFormatterException(connection, "Parameter value [$value] is not a safe float")
        }
    }

    /**
     * Assert if input value can be represented safely as Exasol identifier without quotes.
     *
     * @param[value] Identifier string value
     *
     * @throws ExaFormatterException
     * @return safe identifier string value
     */
    fun safeIdentifier(value: String): String {
        val m = safeIdentifierPattern.matcher(value)

        if (!m.matches()) {
            throw ExaFormatterException(connection, "Parameter value [$value] is not a safe identifier")
        }

        return value
    }
}
