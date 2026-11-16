package com.nutripro.karoo

import com.nutripro.engine.CarbBalanceTracker

/** Fluid debt (spec 3.2): estimated ml behind the sweat-based drink target. */
class FluidDebtDataType(tracker: CarbBalanceTracker, extension: String) :
    SingleValueDataType(tracker, extension, "fluid-debt", select = { it.fluidDebtMl })
