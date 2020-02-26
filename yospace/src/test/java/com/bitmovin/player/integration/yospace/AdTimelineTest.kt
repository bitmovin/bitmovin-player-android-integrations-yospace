package com.bitmovin.player.integration.yospace

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource


class AdTimelineTest {

    private lateinit var adBreaks: List<AdBreak>

    @BeforeEach
    fun setUp() {
        adBreaks = listOf(
            // 30 - 60
            AdBreak(relativeStart = 30.0, duration = 30.0, absoluteStart = 30.0, absoluteEnd = 60.0),
            // 90 - 120
            AdBreak(relativeStart = 120.0, duration = 30.0, absoluteStart = 90.0, absoluteEnd = 120.0),
            // 150 - 180
            AdBreak(relativeStart = 210.0, duration = 30.0, absoluteStart = 150.0, absoluteEnd = 180.0)
        )
    }

    @Test
    fun `Find active ad break`() {
        val time = 35.0
        val expectedAdBreak = adBreaks[0]
        val adBreak = adBreaks.find { it.absoluteStart < time && it.absoluteStart + it.duration > time }
        assert(adBreak != null) { "No active ad break found" }
        assert(adBreak == expectedAdBreak) { "Incorrect ad break found: value=$time, expected=$expectedAdBreak, result=$adBreak" }
    }

    @ParameterizedTest
    @CsvSource(
        "25.0, 0.0",
        "50.0, 0.0",
        "75.0, 30.0",
        "100.0, 30.0"
    )
    fun `Sum ad break durations before given time`(time: Double, durations: Double) {
        val sum = adBreaks.filter { it.absoluteEnd < time }.sumByDouble { it.duration }
        assert(sum == durations) { "Passed ad break durations calculation failed: value=$time, expected=$durations, result=$sum" }
    }

    @ParameterizedTest
    @CsvSource(
        "25.0, 25.0",
        "50.0, 30.0",
        "75.0, 45.0",
        "100.0, 60.0"
    )
    fun `Convert absolute time to relative time`(absoluteTime: Double, relativeTime: Double) {
        val adBreak = adBreaks.find { it.absoluteStart < absoluteTime && it.absoluteStart + it.duration > absoluteTime }
        val passedAdBreakDurations = adBreaks.filter { it.absoluteEnd < absoluteTime }.sumByDouble { it.duration }
        val result = adBreak?.let { it.absoluteStart - passedAdBreakDurations } ?: absoluteTime - passedAdBreakDurations
        assert(result == relativeTime) { "Absolute to relative conversion failed: value=$absoluteTime, expected=$relativeTime, result=$result" }
    }

    @ParameterizedTest
    @CsvSource(
        "25.0, 25.0",
        "50.0, 80.0",
        "75.0, 105.0",
        "100.0, 130.0"
    )
    fun `Convert relative time to absolute time`(relativeTime: Double, absoluteTime: Double) {
        val result = relativeTime + adBreaks.filter { it.relativeStart < relativeTime }.sumByDouble { it.duration }
        assert(result == absoluteTime) { "Absolute to relative conversion failed: value=$relativeTime, expected=$absoluteTime, result=$result" }
    }

    @Test
    fun `Sum ad break durations`() {
        val expected = 90.0
        val result = adBreaks.sumByDouble { it.duration }
        assert(result == expected) { "Sum of ad break durations is incorrect: expected=$expected, result=$result" }
    }
}
