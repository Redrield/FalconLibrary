package org.ghrobotics.lib.commands.impl

import org.ghrobotics.lib.commands.FalconCommand
import org.ghrobotics.lib.utils.BooleanSource
import org.ghrobotics.lib.utils.observabletype.ObservableVariable

class ConditionalCommand(
        private val condition: BooleanSource,
        private val onTrue: FalconCommand,
        private val onFalse: FalconCommand? = null
) : FalconCommand(onTrue.requiredSubsystems + (onFalse?.requiredSubsystems ?: emptyList())) {
    private val commandFinishedCondition = ObservableVariable(false)

    var commandSelected: FalconCommand? = null
        private set

    override suspend fun initialize() {
        val commandSelected = if (condition()) onTrue else onFalse
        this.commandSelected = commandSelected

        if (commandSelected == null) {
            commandFinishedCondition.value = true
        } else {
            commandFinishedCondition.value = false
            commandSelected.internalStart {
                commandFinishedCondition.value = true
            }
        }
    }

    override suspend fun dispose() {
        commandSelected?.internalStop()
    }
}