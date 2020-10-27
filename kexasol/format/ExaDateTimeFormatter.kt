package com.badoo.kexasol.format

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.format.SignStyle
import java.time.temporal.ChronoField


/**
 * Exasol-specific date and timestamp formatter and parser.
 * Currently only default formats are supported.
 */
object ExaDateTimeFormatter {
    private val localDateFormatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4, 4, SignStyle.NOT_NEGATIVE)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT)
        .withChronology(IsoChronology.INSTANCE)

    private val localDateTimeFormatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4, 4, SignStyle.NOT_NEGATIVE)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendLiteral(' ')
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .optionalStart()
        .appendLiteral('.')
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, false)
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT)
        .withChronology(IsoChronology.INSTANCE)

    fun formatLocalDate(value: LocalDate): String {
        return value.format(localDateFormatter)
    }

    fun formatLocalDateTime(value: LocalDateTime): String {
        return value.format(localDateTimeFormatter)
    }

    fun parseLocalDate(value: String): LocalDate {
        return LocalDate.parse(value, localDateFormatter)
    }

    fun parseLocalDateTime(value: String): LocalDateTime {
        return LocalDateTime.parse(value, localDateTimeFormatter)
    }
}
