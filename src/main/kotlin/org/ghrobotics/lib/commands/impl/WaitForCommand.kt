package org.ghrobotics.lib.commands.impl

import org.ghrobotics.lib.commands.FalconCommand
import org.ghrobotics.lib.utils.BooleanSource
import org.ghrobotics.lib.utils.observabletype.ObservableValue

class WaitForCommand() : FalconCommand() {
    init {
        executeFrequency = 0
    }

    constructor(condition: ObservableValue<Boolean>) : this() {
        finishCondition += condition
    }

    constructor(condition: BooleanSource) : this() {
        finishCondition += condition
    }
}