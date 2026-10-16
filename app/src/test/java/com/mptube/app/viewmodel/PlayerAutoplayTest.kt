package com.mptube.app.viewmodel

import com.mptube.app.domain.AutoplayPolicy
import com.mptube.app.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerAutoplayTest {
    private val a = Video("a", "A", "Channel", "")
    private val b = Video("b", "B", "Channel", "")
    private val c = Video("c", "C", "Channel", "")

    @Test
    fun picksFirstVideoThatHasNotBeenPlayed() {
        val next = AutoplayPolicy.pickNext(
            relatedVideos = listOf(a, b, c),
            playedVideoIds = setOf("a"),
            autoplayCount = 1
        )
        assertEquals("b", next?.videoId)
    }

    @Test
    fun returnsNullWhenAllCandidatesWerePlayed() {
        val next = AutoplayPolicy.pickNext(
            relatedVideos = listOf(a, b),
            playedVideoIds = setOf("a", "b"),
            autoplayCount = 2
        )
        assertNull(next)
    }

    @Test
    fun stopsAtAutoplayLimit() {
        val next = AutoplayPolicy.pickNext(
            relatedVideos = listOf(c),
            playedVideoIds = emptySet(),
            autoplayCount = AutoplayPolicy.MAX_VIDEOS
        )
        assertNull(next)
    }
}
