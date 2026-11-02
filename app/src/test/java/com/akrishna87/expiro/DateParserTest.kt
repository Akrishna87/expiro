package com.akrishna87.expiro

import com.akrishna87.expiro.data.DateParser
import com.akrishna87.expiro.data.Gs1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateParserTest {
    private val today = LocalDate.of(2026, 11, 2)
    private fun parse(text: String) = DateParser.findExpiry(text, today)

    @Test fun labelledExpiryBeatsManufactureDate() =
        assertEquals(LocalDate.of(2027, 11, 12), parse("MFD 01/10/2026 EXP 12/11/2027"))

    @Test fun monthNameWithTwoDigitYear() =
        assertEquals(LocalDate.of(2026, 11, 12), parse("BEST BEFORE 12 NOV 26"))

    @Test fun dayMonthNameYearWithDashes() =
        assertEquals(LocalDate.of(2026, 12, 5), parse("Use by: 5-Dec-2026"))

    @Test fun monthAndYearOnlyMeansEndOfMonth() {
        assertEquals(LocalDate.of(2027, 11, 30), parse("BB 11.2027"))
        assertEquals(LocalDate.of(2027, 3, 31), parse("EXP 03/2027"))
    }

    @Test fun multiLineTextPicksLabelledDate() =
        assertEquals(LocalDate.of(2027, 6, 11), parse("Mfg 12/06/2026\nExp 11/06/2027\nBatch A123"))

    @Test fun fullDateIsNotAlsoReadAsMonthYear() =
        assertEquals(LocalDate.of(2027, 1, 10), parse("PKD 10 JAN 2026 BEST BEFORE 10 JAN 2027"))

    @Test fun plainDates() {
        assertEquals(LocalDate.of(2026, 11, 12), parse("12/11/26"))
        assertEquals(LocalDate.of(2026, 12, 31), parse("Expiry 31.12.2026"))
    }

    @Test fun noDate() {
        assertNull(parse("hello world"))
        assertNull(parse("Net wt 500 g  2 x 250"))
    }

    @Test fun gs1ExpiryAndProductNumber() {
        assertEquals(LocalDate.of(2027, 11, 12), Gs1.parseExpiry("(01)09506000134352(17)271112(10)AB12"))
        assertEquals(LocalDate.of(2027, 11, 12), Gs1.parseExpiry("010950600013435217271112"))
        assertEquals(LocalDate.of(2026, 9, 5), Gs1.parseExpiry("010950600013435215260905"))
        assertNull(Gs1.parseExpiry("8901234567890"))
        assertEquals("9506000134352", Gs1.productNumber("(01)09506000134352(17)271112"))
        assertNull(Gs1.productNumber("8901234567890"))
    }
}
