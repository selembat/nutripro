package com.nutripro.karoo

import com.nutripro.engine.CarbBalanceTracker

/** Carb debt (spec 3.1): how far behind the target intake rate the rider currently is. */
class CarbDebtDataType(tracker: CarbBalanceTracker, extension: String) :
    SingleValueDataType(tracker, extension, "carb-debt", select = { it.carbDebtGrams })
