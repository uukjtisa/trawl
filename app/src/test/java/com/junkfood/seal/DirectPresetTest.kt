package com.junkfood.seal

// NEW FILE (Trawl project, 2026-08-31). Not inherited from upstream.
//
// The rung table is the whole reason these presets are named by direction rather than by
// position, so it is the part worth pinning down. A resolver's ladder is usually two entries and
// occasionally one; a "medium" setting over two rungs has no meaning, and the real question is
// which way to round when the ladder cannot honour the request.
//
// Pure arithmetic, no Android, no network.

import com.junkfood.seal.util.CONVERT_M4A
import com.junkfood.seal.util.CONVERT_MP3
import com.junkfood.seal.util.DirectPreset
import com.junkfood.seal.util.DirectPresets
import com.junkfood.seal.util.QualityRung
import com.junkfood.seal.util.QualityRung.BEST
import com.junkfood.seal.util.QualityRung.LOWEST
import com.junkfood.seal.util.QualityRung.MID_HIGH
import com.junkfood.seal.util.QualityRung.MID_LOW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectPresetTest {

    private fun row(size: Int) = QualityRung.entries.map { it.indexIn(size) }

    @Test
    fun `a one-rung ladder sends every setting to the only entry`() {
        // A direct link resolves to exactly one file. No setting can mean anything else.
        assertEquals(listOf(0, 0, 0, 0), row(1))
    }

    @Test
    fun `a two-rung ladder rounds outward, which is the case that needed naming`() {
        // TikTok: clean and watermarked. There is no middle, so mid-high IS best and mid-low IS
        // lowest -- and the names say which way they lean rather than pretending to a position.
        assertEquals(listOf(BEST, MID_HIGH, MID_LOW, LOWEST).map { it.indexIn(2) }, listOf(0, 0, 1, 1))
    }

    @Test
    fun `an odd ladder has a real middle and both directions find it`() {
        assertEquals(listOf(0, 1, 1, 2), row(3))
        assertEquals(listOf(0, 2, 2, 4), row(5))
    }

    @Test
    fun `an even ladder splits the two middle rungs either side of centre`() {
        assertEquals(listOf(0, 1, 2, 3), row(4))
        assertEquals(listOf(0, 2, 3, 5), row(6))
    }

    @Test
    fun `best is always first and lowest always last, at every size`() {
        for (n in 1..12) {
            assertEquals("best at size $n", 0, BEST.indexIn(n))
            assertEquals("lowest at size $n", n - 1, LOWEST.indexIn(n))
        }
    }

    @Test
    fun `the middle rungs never cross and never leave the ladder`() {
        for (n in 1..12) {
            val high = MID_HIGH.indexIn(n)
            val low = MID_LOW.indexIn(n)
            assert(high <= low) { "mid-high ($high) must not fall below mid-low ($low) at size $n" }
            assert(high in 0 until n) { "mid-high out of range at size $n" }
            assert(low in 0 until n) { "mid-low out of range at size $n" }
        }
    }

    @Test
    fun `an empty ladder picks nothing rather than throwing`() {
        assertNull(BEST.pick(emptyList<String>()))
        assertNull(LOWEST.pick(emptyList<String>()))
    }

    @Test
    fun `pick reads the ladder best-first`() {
        val ladder = listOf("1080p", "720p", "360p")
        assertEquals("1080p", BEST.pick(ladder))
        assertEquals("720p", MID_HIGH.pick(ladder))
        assertEquals("360p", LOWEST.pick(ladder))
    }

    // ------------------------------------------------------------------ storage

    @Test
    fun `a preset survives a round trip`() {
        val p = DirectPreset(MID_LOW, CONVERT_MP3)
        assertEquals(p, DirectPreset.parse(p.serialise()))
    }

    @Test
    fun `anything unparseable falls back to the default rather than crashing`() {
        assertEquals(DirectPreset.Default, DirectPreset.parse(null))
        assertEquals(DirectPreset.Default, DirectPreset.parse(""))
        assertEquals(DirectPreset.Default, DirectPreset.parse("garbage"))
        assertEquals(DirectPreset.Default, DirectPreset.parse("best"))
        assertEquals(DirectPreset.Default, DirectPreset.parse("best|notanumber"))
        // An unknown rung name degrades to the default rung, keeping the container.
        assertEquals(DirectPreset(BEST, CONVERT_M4A), DirectPreset.parse("wat|$CONVERT_M4A"))
    }

    @Test
    fun `platform and type key separately, and a label with punctuation still keys`() {
        val a = DirectPresets.keyFor("TikTok", audio = true)
        val v = DirectPresets.keyFor("TikTok", audio = false)
        assertEquals("direct_preset_tiktok_audio", a)
        assertEquals("direct_preset_tiktok_video", v)
        // "X / Twitter" is a real platform label from DirectResolvers.
        assertEquals("direct_preset_x_twitter_audio", DirectPresets.keyFor("X / Twitter", true))
        assertEquals("direct_preset_direct_link_video", DirectPresets.keyFor("Direct link", false))
    }
}
