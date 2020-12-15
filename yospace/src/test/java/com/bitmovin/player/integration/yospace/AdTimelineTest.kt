package com.bitmovin.player.integration.yospace

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.exp


class AdTimelineTest {

    private lateinit var adTimeline: AdTimeline

    @BeforeEach
    fun setUp() {
        val adBreaks = listOf(
            AdBreak(id = "1", relativeStart = 30.0, duration = 30.0, absoluteStart = 30.0, absoluteEnd = 60.0),
            AdBreak(id = "2", relativeStart = 60.0, duration = 30.0, absoluteStart = 90.0, absoluteEnd = 120.0),
            AdBreak(id = "3", relativeStart = 90.0, duration = 30.0, absoluteStart = 150.0, absoluteEnd = 180.0),
            AdBreak(id = "4", relativeStart = 120.0, duration = 30.0, absoluteStart = 210.0, absoluteEnd = 240.0),
            AdBreak(id = "5", relativeStart = 150.0, duration = 30.0, absoluteStart = 270.0, absoluteEnd = 300.0)
        )
        adTimeline = AdTimeline(adBreaks)
    }

    @ParameterizedTest
    @CsvSource(
        /* Ad break 1 */
        "30.0, 1",
        "45.0, 1",
        "60.0, 1",
        /* Ad break 2 */
        "90.0, 2",
        "105.0, 2",
        "120.0, 2",
        /* Ad break 3 */
        "150.0, 3",
        "165.0, 3",
        "180.0, 3",
        /* Ad break 4 */
        "210.0, 4",
        "225.0, 4",
        "240.0, 4",
        /* Ad break 5 */
        "270.0, 5",
        "285.0, 5",
        "290.0, 5"
    )
    fun `Find current ad break`(time: Double, id: String) {
        val expected = adTimeline.adBreaks.find { it.id == id }
        val result = adTimeline.currentAdBreak(time)
        assert(result != null) { "No ad break found" }
        assert(result == expected) { "Expected ${expected}, result $result" }
    }

    @ParameterizedTest
    @CsvSource(
        "0.0",
        "15.0",
        "29.99",
        "60.01",
        "75.0",
        "89.9",
        "120.01",
        "135.0",
        "149.99",
        "180.01",
        "195.0",
        "209.99",
        "240.01",
        "255.0",
        "269.99",
        "300.01"
    )
    fun `Find current ad break null`(time: Double) {
        val expected = null
        val result = adTimeline.currentAdBreak(time)
        assert(expected == result) { "Expected ${expected}, result $result" }
    }

    @ParameterizedTest
    @CsvSource(
        /* Ad break 1 */
        "31.0, 1",
        "45.0, 1",
        "60.0, 1",
        /* Ad break 2 */
        "61.0, 2",
        "75.0, 2",
        "90.0, 2",
        /* Ad break 3 */
        "91.0, 3",
        "105.0, 3",
        "120.0, 3",
        /* Ad break 4 */
        "121.0, 4",
        "135.0, 4",
        "150.0, 4",
        /* Ad break 5 */
        "150.01, 5",
        "165.25, 5",
        "180.5, 5",
        "500.75, 5"
    )
    fun `Find previous break`(time: Double, id: String) {
        val expected = adTimeline.adBreaks.find { it.id == id }
        val result = adTimeline.previousAdBreak(time)
        assert(result != null) { "No ad break found" }
        assert(expected == result) { "Expected ${expected}, result $result" }
    }

    @ParameterizedTest
    @CsvSource(
        "0.0",
        "10.0",
        "15.0",
        "30.0"
    )
    fun `Find previous ad break null`(time: Double) {
        val expected = null
        val result = adTimeline.previousAdBreak(time)
        assert(expected == result) { "Expected ${expected}, result $result" }
    }

    @ParameterizedTest
    @CsvSource(
        /* Ad break 1 */
        "0.0, 1",
        "15.0, 1",
        "29.0, 1",
        /* Ad break 2 */
        "30.0, 2",
        "45.0, 2",
        "59.0, 2",
        /* Ad break 3 */
        "60.0, 3",
        "75.0, 3",
        "89.0, 3",
        /* Ad break 4 */
        "90.0, 4",
        "105.0, 4",
        "119.0, 4",
        /* Ad break 5 */
        "120.01, 5",
        "145.25, 5",
        "149.99, 5"
    )
    fun `Find next ad break`(time: Double, id: String) {
        val expected = adTimeline.adBreaks.find { it.id == id }
        val result = adTimeline.nextAdBreak(time)
        assert(result != null) { "No ad break found" }
        assert(expected == result) { "Expected ${expected}, result $result" }
    }

    @ParameterizedTest
    @CsvSource(
        "150.000001",
        "151.0",
        "10000.0"
    )
    fun `Find next ad break null`(time: Double) {
        val expected = null
        val result = adTimeline.nextAdBreak(time)
        assert(expected == result) { "Expected ${expected}, result $result" }
    }

    @ParameterizedTest
    @CsvSource(
        "0.0, 0.0",
        "25.0, 25.0",
        "50.0, 30.0",
        "75.0, 45.0",
        "100.0, 60.0",
        "125.0, 65.0",
        "150.0, 90.0",
        "175.0, 90.0",
        "200.0, 110.0",
        "225.0, 120.0",
        "250.0, 130.0",
        "275.0, 150.0",
        "300.0, 150.0",
        "325.0, 175.0",
        "350.0, 200.0"
    )
    fun `Convert absolute time to relative time`(absoluteTime: Double, relativeTime: Double) {
        val result = adTimeline.absoluteToRelative(absoluteTime)
        assert(result == relativeTime) { "Expected ${relativeTime}, result $result" }
    }

    @ParameterizedTest
    @CsvSource(
        "0.0, 0.0",
        "25.0, 25.0",
        "50.0, 80.0",
        "75.0, 135.0",
        "100.0, 190.0",
        "125.0, 245.0",
        "150.0, 270.0",
        "175.0, 325.0",
        "200.0, 350.0"
    )
    fun `Convert relative time to absolute time`(relativeTime: Double, absoluteTime: Double) {
        val result = adTimeline.absoluteToRelative(absoluteTime)
        assert(result == relativeTime) { "Expected ${relativeTime}, result $result" }
    }

    @Test
    fun `Sum ad break durations`() {
        val expected = 150.0
        val result = adTimeline.totalAdBreakDurations()
        assert(result == expected) { "Expected ${expected}, result $result" }
    }

    @ParameterizedTest
    @CsvSource(
        "0.0, 0.0",
        "25.0, 0.0",
        "50.0, 0.0",
        "75.0, 30.0",
        "100.0, 30.0",
        "125.0, 60.0",
        "150.0, 60.0",
        "175.0, 60.0",
        "200.0, 90.0",
        "225.0, 90.0",
        "250.0, 120.0",
        "275.0, 120.0",
        "300.0, 120.0",
        "325.0, 150.0",
        "350.0, 150.0"
    )
    fun `Sum passed ad break durations`(time: Double, duration: Double) {
        val result = adTimeline.totalPassedAdBreakDurations(time)
        assert(duration == result) { "Expected ${duration}, result $result" }
    }
}
