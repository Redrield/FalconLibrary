package org.ghrobotics.lib.commands.impl

import kotlinx.coroutines.GlobalScope
import org.ghrobotics.lib.commands.FalconCommand
import org.ghrobotics.lib.commands.FalconCommandGroupBuilder
import org.ghrobotics.lib.commands.commandGroup
import org.ghrobotics.lib.utils.BooleanSource
import org.ghrobotics.lib.utils.observabletype.ObservableValue
import org.ghrobotics.lib.utils.observabletype.ObservableVariable
import org.ghrobotics.lib.utils.observabletype.or
import org.ghrobotics.lib.utils.observabletype.updatableValue

fun FalconCommand.withExit(condition: BooleanSource) = WithExitCommand(condition, this)
fun FalconCommand.withExit(condition: ObservableValue<Boolean>) = WithExitCommand(condition, this)

fun FalconCommandGroupBuilder.withExit(condition: BooleanSource, block: FalconCommandGroupBuilder.() -> Unit) =
        WithExitCommand(condition, commandGroup(type, block))

fun FalconCommandGroupBuilder.withExit(condition: ObservableValue<Boolean>, block: FalconCommandGroupBuilder.() -> Unit) =
        WithExitCommand(condition, commandGroup(type, block))

class WithExitCommand(
        condition: ObservableValue<Boolean>,
        private val command: FalconCommand
) : FalconCommand(command.requiredSubsystems) {
    constructor(condition: BooleanSource, command: FalconCommand) :
            this(GlobalScope.updatableValue(block = condition), command)

    private val commandFinishedCondition = ObservableVariable(false)

    init {
        executeFrequency = 0
        finishCondition += condition or commandFinishedCondition
    }

    override suspend fun initialize() {
        commandFinishedCondition.value = false
        command.internalStart {
            commandFinishedCondition.value = true
        }
    }

    override suspend fun dispose() {
        command.internalStop()
    }
}