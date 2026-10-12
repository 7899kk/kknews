package com.karthikkilari.expiryradar

import com.karthikkilari.expiryradar.backup.Csv
import com.karthikkilari.expiryradar.domain.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class DomainTest {
    private val today = LocalDate.of(2027, 8, 15)

    @Test
    fun statusBoundaries() {
        assertEquals(ExpiryStatus.EXPIRED, Expiry.status(today.minusDays(1), today))
        assertEquals(ExpiryStatus.TODAY, Expiry.status(today, today))
        assertEquals(ExpiryStatus.SOON, Expiry.status(today.plusDays(7), today))
        assertEquals(ExpiryStatus.SAFE, Expiry.status(today.plusDays(8), today))
    }

    @Test
    fun zeroWarning() {
        assertEquals(ExpiryStatus.SAFE, Expiry.status(today.plusDays(1), today, 0))
    }

    @Test
    fun labels() {
        assertEquals("Expires tomorrow", Expiry.label(today.plusDays(1), today))
        assertEquals("Expired 2 days ago", Expiry.label(today.minusDays(2), today))
    }

    @Test
    fun leapDay() {
        assertEquals(1, Expiry.days(LocalDate.of(2028, 2, 29), LocalDate.of(2028, 2, 28)))
        assertEquals(2, Expiry.days(LocalDate.of(2028, 3, 1), LocalDate.of(2028, 2, 28)))
    }

    @Test
    fun yearRollover() {
        assertEquals(1, Expiry.days(LocalDate.of(2028, 1, 1), LocalDate.of(2027, 12, 31)))
    }

    @Test
    fun localDateAcrossZones() {
        val instant = Instant.parse("2027-08-15T20:00:00Z")
        val india = instant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()
        val us = instant.atZone(ZoneId.of("America/New_York")).toLocalDate()
        assertEquals(ExpiryStatus.EXPIRED, Expiry.status(today, india))
        assertEquals(ExpiryStatus.TODAY, Expiry.status(today, us))
    }

    @Test
    fun ocrIso() {
        assertEquals(
            LocalDate.of(2027, 12, 30),
            DateParser.candidates("Best Before 2027-12-30").single().date,
        )
    }

    @Test
    fun ocrDmy() {
        val result = DateParser.candidates("EXP: 15-08-2027").single()
        assertEquals(today, result.date)
        assertEquals("Expiry", result.context)
    }

    @Test
    fun ocrMonthYear() {
        assertEquals(LocalDate.of(2027, 12, 31), DateParser.candidates("EXP 12/2027").single().date)
    }

    @Test
    fun ocrLeapMonth() {
        assertEquals(LocalDate.of(2028, 2, 29), DateParser.candidates("EXP 02/2028").single().date)
    }

    @Test
    fun ocrAmbiguous() {
        val c = DateParser.candidates("Use By 05/06/2027")
        assertEquals(2, c.size)
        assertTrue(c.all { it.ambiguous })
    }

    @Test
    fun ocrMfgContext() {
        assertEquals("Manufacturing", DateParser.candidates("MFG 15/08/2027").single().context)
    }

    @Test
    fun invalidDatesAreRejected() {
        assertTrue(DateParser.candidates("EXP 31/02/2027").isEmpty())
        assertTrue(DateParser.candidates("EAN 1234567890123").isEmpty())
    }

    @Test
    fun reminderOffsetsDeduplicate() {
        assertEquals(listOf(30, 7, 1, 0), ReminderPolicy.parse("7,1,7,30,0"))
    }

    @Test
    fun reminderDueOnCalendarDate() {
        assertEquals(listOf(7), ReminderPolicy.due(today.plusDays(7), listOf(30, 7, 1, 0), today))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeReminderRejected() {
        ReminderPolicy.parse("-1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun tooLargeReminderRejected() {
        ReminderPolicy.parse("3651")
    }

    @Test
    fun csvEscapingAndNewlines() {
        val rows =
            listOf(listOf("name", "notes"), listOf("Milk, full cream", "He said \"use\"\nToday"))
        assertEquals(rows, Csv.decode(Csv.encode(rows)))
    }

    @Test
    fun csvCrLfAndEmpty() {
        assertEquals(listOf(listOf("a", "b"), listOf("", "")), Csv.decode("a,b\r\n,\r\n"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun brokenCsvRejected() {
        Csv.decode("\"unclosed")
    }

    @Test
    fun largeCollectionDates() {
        val count =
            (0 until 20000).count {
                Expiry.status(today.plusDays((it % 30).toLong()), today) == ExpiryStatus.SOON
            }
        assertTrue(count > 4000)
    }

    @Test
    fun differentLabelsOnSameLine() {
        val dates = DateParser.candidates("MFG 01/2026 EXP 12/2027")
        assertEquals(listOf("Manufacturing", "Expiry"), dates.map { it.context })
    }
}
