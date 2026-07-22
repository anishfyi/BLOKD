package dev.anishfyi.blokd.dns

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolverChainTest {

    private val answer = byteArrayOf(1, 2, 3)

    @Test
    fun firstTierThatAnswersIsReported() {
        val outcome = ResolverChain.run(
            listOf(
                ResolverTier.PRIMARY to { answer },
                ResolverTier.SECONDARY to { byteArrayOf(9) },
            ),
        )

        assertArrayEquals(answer, outcome.response)
        assertEquals(ResolverTier.PRIMARY, outcome.tier)
    }

    @Test
    fun fallsThroughToSecondaryWhenPrimaryReturnsNull() {
        val outcome = ResolverChain.run(
            listOf(
                ResolverTier.PRIMARY to { null },
                ResolverTier.SECONDARY to { answer },
            ),
        )

        assertArrayEquals(answer, outcome.response)
        assertEquals(ResolverTier.SECONDARY, outcome.tier)
    }

    @Test
    fun reportsUnfilteredWhenBothEncryptedTiersFail() {
        val outcome = ResolverChain.run(
            listOf(
                ResolverTier.PRIMARY to { null },
                ResolverTier.SECONDARY to { null },
                ResolverTier.UNFILTERED to { answer },
            ),
        )

        assertEquals(ResolverTier.UNFILTERED, outcome.tier)
    }

    @Test
    fun allNullYieldsFailedAndNoResponse() {
        val outcome = ResolverChain.run(
            listOf(
                ResolverTier.PRIMARY to { null },
                ResolverTier.SECONDARY to { null },
            ),
        )

        assertNull(outcome.response)
        assertEquals(ResolverTier.FAILED, outcome.tier)
    }

    @Test
    fun emptyChainFailsClosed() {
        val outcome = ResolverChain.run(emptyList())

        assertNull(outcome.response)
        assertEquals(ResolverTier.FAILED, outcome.tier)
    }

    @Test
    fun laterStepsAreNotInvokedOnceAnswered() {
        var secondaryCalled = false
        val outcome = ResolverChain.run(
            listOf(
                ResolverTier.PRIMARY to { answer },
                ResolverTier.SECONDARY to {
                    secondaryCalled = true
                    answer
                },
            ),
        )

        assertTrue(outcome.response != null)
        assertFalse("secondary step must not run after primary answers", secondaryCalled)
    }
}
