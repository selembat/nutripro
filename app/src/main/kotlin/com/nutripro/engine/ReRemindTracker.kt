package com.nutripro.engine

/** Pure Kotlin re-remind tracker (spec 3.4): nudges again if nothing was logged after an alert fired. */
class ReRemindTracker {
    private var lastAlertFiredAtSec: Long? = null

    fun onAlertFired(nowSec: Long) {
        lastAlertFiredAtSec = nowSec
    }

    fun onIntakeLogged() {
        lastAlertFiredAtSec = null
    }

    /** [reRemindAfterSec] is read fresh on every call. */
    fun isReRemindDue(nowSec: Long, reRemindAfterSec: Long): Boolean {
        val firedAt = lastAlertFiredAtSec ?: return false
        return nowSec - firedAt >= reRemindAfterSec
    }
}
