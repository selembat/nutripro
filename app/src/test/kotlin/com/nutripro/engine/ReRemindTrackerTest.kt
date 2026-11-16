package com.nutripro.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReRemindTrackerTest {
    @Test
    fun `no re-remind before an alert has fired`() {
        val tracker = ReRemindTracker()
        assertFalse(tracker.isReRemindDue(nowSec = 1000L, reRemindAfterSec = 10 * 60L))
    }

    @Test
    fun `re-remind fires after the interval if nothing was logged`() {
        val tracker = ReRemindTracker()
        tracker.onAlertFired(nowSec = 0L)
        assertFalse(tracker.isReRemindDue(nowSec = 599L, reRemindAfterSec = 10 * 60L))
        assertTrue(tracker.isReRemindDue(nowSec = 600L, reRemindAfterSec = 10 * 60L))
    }

    @Test
    fun `logging intake cancels the pending re-remind`() {
        val tracker = ReRemindTracker()
        tracker.onAlertFired(nowSec = 0L)
        tracker.onIntakeLogged()
        assertFalse(tracker.isReRemindDue(nowSec = 600L, reRemindAfterSec = 10 * 60L))
    }
}
