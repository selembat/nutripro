package com.nutripro.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertChannelTest {
    private val maxHoldSec = 5 * 60L
    private val reRemindAfterSec = 10 * 60L

    @Test
    fun `fires once when due and safe, not again on later ticks`() {
        val channel = AlertChannel()
        assertTrue(channel.evaluate(nowSec = 0L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
        // Still due and safe on every subsequent tick - must NOT fire again every second.
        assertFalse(channel.evaluate(nowSec = 1L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
        assertFalse(channel.evaluate(nowSec = 2L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
        assertFalse(channel.evaluate(nowSec = 300L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
    }

    @Test
    fun `re-fires only once the re-remind interval elapses`() {
        val channel = AlertChannel()
        assertTrue(channel.evaluate(nowSec = 0L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
        assertFalse(channel.evaluate(nowSec = 599L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
        assertTrue(channel.evaluate(nowSec = 600L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
        // And again only after another full interval, not on every tick thereafter.
        assertFalse(channel.evaluate(nowSec = 601L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
    }

    @Test
    fun `clearing baseDue re-arms the channel to fire again next time it's due`() {
        val channel = AlertChannel()
        assertTrue(channel.evaluate(nowSec = 0L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
        assertFalse(channel.evaluate(nowSec = 1L, baseDue = false, isUnsafe = false, maxHoldSec, reRemindAfterSec))
        // Debt crossed the threshold again later - should fire fresh, not wait for a re-remind.
        assertTrue(channel.evaluate(nowSec = 2L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
    }

    @Test
    fun `holds while unsafe and does not spam once released`() {
        val channel = AlertChannel()
        assertFalse(channel.evaluate(nowSec = 0L, baseDue = true, isUnsafe = true, maxHoldSec, reRemindAfterSec))
        assertFalse(channel.evaluate(nowSec = 60L, baseDue = true, isUnsafe = true, maxHoldSec, reRemindAfterSec))
        assertTrue(channel.evaluate(nowSec = 90L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
        assertFalse(channel.evaluate(nowSec = 91L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
    }

    @Test
    fun `onIntakeLogged cancels a pending re-remind`() {
        val channel = AlertChannel()
        assertTrue(channel.evaluate(nowSec = 0L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
        channel.onIntakeLogged()
        assertFalse(channel.evaluate(nowSec = 600L, baseDue = true, isUnsafe = false, maxHoldSec, reRemindAfterSec))
    }
}
