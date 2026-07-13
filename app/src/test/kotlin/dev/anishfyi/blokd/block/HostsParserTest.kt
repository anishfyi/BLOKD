package dev.anishfyi.blokd.block

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostsParserTest {

    @Test
    fun parsesHostsAndPlainLinesAndSkipsNoise() {
        val lines = sequenceOf(
            "# a comment",
            "0.0.0.0 ads.example.com",
            "127.0.0.1 localhost",
            "tracker.example.net",
            "   ",
            "0.0.0.0 evil.example.org # inline comment",
            "0.0.0.0 not_a_domain",
        )
        val set = HostsParser.parse(lines)
        assertTrue(set.contains("ads.example.com"))
        assertTrue(set.contains("tracker.example.net"))
        assertTrue(set.contains("evil.example.org"))
        assertFalse(set.contains("localhost"))
        assertFalse(set.contains("not_a_domain"))
    }
}
