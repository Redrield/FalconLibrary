package org.ghrobotics.lib.commands.impl

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ghrobotics.lib.commands.FalconCommand
import org.ghrobotics.lib.commands.FalconCommandGroupBuilder
import org.ghrobotics.lib.commands.commandGroup
import org.ghrobotics.lib.mathematics.units.Time
import org.ghrobotics.lib.utils.Source
import org.ghrobotics.lib.utils.observabletype.ObservableVariable

fun FalconCommand.withTimeout(delay: Time) = TimeoutCommand(delay, this)
fun FalconCommand.withTimeout(delay: Source<Time>) = TimeoutCommand(delay, this)

fun FalconCommandGroupBuilder.withTimeout(delay: Time, block: FalconCommandGroupBuilder.() -> Unit) =
        TimeoutCommand(delay, commandGroup(type, block))

fun FalconCommandGroupBuilder.withTimeout(delay: Source<Time>, block: FalconCommandGroupBuilder.() -> Unit) =
        TimeoutCommand(delay, commandGroup(type, block))

class TimeoutCommand(
        private val timeoutSource: Source<Time>,
        private val command: FalconCommand
) : FalconCommand(command.requiredSubsystems) {
    constructor(timeout: Time, command: FalconCommand) : this(Source(timeout), command)

    private val commandFinishedCondition = ObservableVariable(false)

    init {
        executeFrequency = 0
        finishCondition += commandFinishedCondition
    }

    private lateinit var job: Job

    override suspend fun initialize() {
        commandFinishedCondition.value = false
        command.internalStart {
            commandFinishedCondition.value = true
        }
        job = commandScope.launch {
            delay(timeoutSource().millisecond.toLong())
            commandFinishedCondition.value = true
        }
    }

    override suspend fun dispose() {
        job.cancelAndJoin()
        command.internalStop()
    }
}