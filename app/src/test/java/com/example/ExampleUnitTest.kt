package com.example

import com.example.model.PingType
import com.example.model.SoundPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun pingType_formatting_correct() {
        val messageNeedYou = PingType.NEED_YOU.formatMessage("Alex")
        assertEquals("Alex needs you right now.", messageNeedYou)

        val messageBored = PingType.BORED.formatMessage("Jordan")
        assertEquals("Jordan: Hi, how is your day going? 😊", messageBored)
    }

    @Test
    fun pingType_fromString_fallback() {
        assertEquals(PingType.BORED, PingType.fromString("bored"))
        assertEquals(PingType.BORED, PingType.fromString("IM_BORED"))
        assertEquals(PingType.NEED_YOU, PingType.fromString("NEED_YOU"))
        assertEquals(PingType.NEED_YOU, PingType.fromString("invalid_value"))
    }

    @Test
    fun soundPreset_lookup() {
        assertEquals(SoundPreset.URGENT, SoundPreset.fromId("urgent_ping"))
        assertEquals(SoundPreset.BORED, SoundPreset.fromId("bored_ping"))
        assertEquals(SoundPreset.SOFT_BELL, SoundPreset.fromId("soft_bell"))
        assertEquals(SoundPreset.RADAR, SoundPreset.fromId("radar_ping"))
        assertEquals(SoundPreset.URGENT, SoundPreset.fromId("unknown"))
    }

    @Test
    fun pingType_vibrationPatterns_valid() {
        assertTrue(PingType.NEED_YOU.vibrationPattern.isNotEmpty())
        assertTrue(PingType.BORED.vibrationPattern.isNotEmpty())
    }
}
