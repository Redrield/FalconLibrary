package org.ghrobotics.lib.commands.impl

import org.ghrobotics.lib.commands.FalconCommand

abstract class InstantCommand : FalconCommand() {
    init {
        executeFrequency = 0
        finishCondition += { true }
    }
}

class InstantRunnableCommand(
        private val block: suspend () -> Unit
) : InstantCommand() {
    override suspend fun initialize() = block()
}