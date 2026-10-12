package com.karthikkilari.expiryradar.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class ExpiryStatus {
    SAFE,
    SOON,
    TODAY,
    EXPIRED,
}

object Expiry {
    fun days(date: LocalDate, today: LocalDate = LocalDate.now()) =
        ChronoUnit.DAYS.between(today, date).toInt()

    fun status(
        date: LocalDate,
        today: LocalDate = LocalDate.now(),
        warning: Int = 7,
    ): ExpiryStatus {
        val days = days(date, today)
        return when {
            days < 0 -> ExpiryStatus.EXPIRED
            days == 0 -> ExpiryStatus.TODAY
            days <= warning.coerceAtLeast(0) -> ExpiryStatus.SOON
            else -> ExpiryStatus.SAFE
        }
    }

    fun label(date: LocalDate, today: LocalDate = LocalDate.now()): String =
        when (val d = days(date, today)) {
            0 -> "Expires today"
            1 -> "Expires tomorrow"
            else -> if (d < 0) "Expired ${-d} days ago" else "$d days remaining"
        }
}

data class DateCandidate(val date: LocalDate, val context: String, val ambiguous: Boolean = false)

object DateParser {
    private val pattern = Regex("(?<!\\d)(\\d{1,4})[/.-](\\d{1,4})(?:[/.-](\\d{2,4}))?(?!\\d)")

    fun candidates(text: String): List<DateCandidate> =
        text
            .lineSequence()
            .flatMap { line ->
                pattern.findAll(line).flatMap { m ->
                    val label =
                        Regex(
                                "(?i)best\\s*before|use\\s*by|exp(?:iry|ires|iration)?\\b|mfg\\b|manufactur\\w*"
                            )
                            .findAll(line.take(m.range.first))
                            .lastOrNull()
                            ?.value
                            .orEmpty()
                    val context =
                        when {
                            Regex("(?i)mfg|manufactur").containsMatchIn(label) -> "Manufacturing"
                            label.isNotEmpty() -> "Expiry"
                            else -> "Unlabelled"
                        }
                    val a = m.groupValues[1].toInt()
                    val b = m.groupValues[2].toInt()
                    val c = m.groupValues[3].toIntOrNull()
                    val triples =
                        when {
                            c == null && a in 1..12 && b in 2000..2199 ->
                                listOf(Triple(b, a, YearMonth.of(b, a).lengthOfMonth()))
                            c != null && a in 2000..2199 -> listOf(Triple(a, b, c))
                            c != null && c in 2000..2199 ->
                                if (a in 1..12 && b in 1..12 && a != b)
                                    listOf(Triple(c, b, a), Triple(c, a, b))
                                else listOf(Triple(c, b, a))
                            else -> emptyList()
                        }
                    triples
                        .mapNotNull { (year, month, day) ->
                            runCatching {
                                    DateCandidate(
                                        LocalDate.of(year, month, day),
                                        context,
                                        triples.size > 1,
                                    )
                                }
                                .getOrNull()
                        }
                        .asSequence()
                }
            }
            .distinct()
            .toList()
}

object ReminderPolicy {
    fun parse(value: String): List<Int> =
        value
            .split(',')
            .filter { it.isNotBlank() }
            .map {
                it.trim().toInt().also { n ->
                    require(n in 0..3650) { "Reminder days must be between 0 and 3650" }
                }
            }
            .distinct()
            .sortedDescending()

    fun due(expiry: LocalDate, offsets: List<Int>, today: LocalDate): List<Int> =
        offsets.filter { expiry.minusDays(it.toLong()) == today }
}
