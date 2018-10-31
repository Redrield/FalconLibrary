package org.ghrobotics.lib.commands

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ghrobotics.lib.utils.observabletype.ObservableVariable

class FalconCommandGroup(
        private val groupType: GroupType,
        private val commands: List<FalconCommand>
) : FalconCommand(commands.flatMap { it.requiredSubsystems }) {

    init {
        if (groupType == GroupType.PARALLEL) {
            // Check required subsystem overlap
            for (c1 in commands) {
                for (c2 in commands) {
                    if (c1 == c2) continue
                    if (c1.requiredSubsystems.any { rs -> c2.requiredSubsystems.contains(rs) }) {
                        throw IllegalArgumentException("Subsystem conflict! ${c1::class.simpleName} and ${c2::class.simpleName}")
                    }
                }
            }
        }
    }

    private val groupFinishCondition = ObservableVariable(false)

    init {
        executeFrequency = 0
        finishCondition += groupFinishCondition
    }

    private sealed class GroupEvent {
        class StartCommand(val falconCommand: FalconCommand) : GroupEvent()
        class StopCommand(val falconCommand: FalconCommand) : GroupEvent()
    }

    private val runningCommands = mutableListOf<FalconCommand>()
    private val commandsLeftToRun = mutableListOf<FalconCommand>()

    private var job: Job? = null
    private var channel: Channel<GroupEvent>? = null

    override suspend fun initialize() {
        commandsLeftToRun.clear()
        commandsLeftToRun += commands
        if (commandsLeftToRun.isEmpty()) {
            groupFinishCondition.value = true
        } else {
            groupFinishCondition.value = false
            val channel = Channel<GroupEvent>(Channel.UNLIMITED)
            this.channel = channel
            when (groupType) {
                GroupType.SEQUENTIAL -> channel.send(GroupEvent.StartCommand(commandsLeftToRun.removeAt(0)))
                GroupType.PARALLEL -> {
                    // Start all the commands
                    commandsLeftToRun.forEach { channel.send(GroupEvent.StartCommand(it)) }
                }
            }
            job = commandScope.launch {
                for (event in channel) {
                    when (event) {
                        is GroupEvent.StartCommand -> {
                            val command = event.falconCommand
                            runningCommands += command
                            command.internalStart {
                                channel.sendBlocking(GroupEvent.StopCommand(command))
                            }
                        }
                        is GroupEvent.StopCommand -> {
                            val command = event.falconCommand
                            if(runningCommands.contains(command)) {
                                command.internalStop()
                                runningCommands -= command
                                when (groupType) {
                                    GroupType.SEQUENTIAL -> {
                                        if (commandsLeftToRun.isEmpty()) {
                                            groupFinishCondition.value = true
                                        } else {
                                            channel.send(GroupEvent.StartCommand(commandsLeftToRun.removeAt(0)))
                                        }
                                    }
                                    GroupType.PARALLEL -> {
                                        groupFinishCondition.value = runningCommands.isEmpty()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun dispose() {
        channel?.close()
        job?.cancelAndJoin()
        channel = null
        job = null
        runningCommands.forEach {
            it.internalStop()
        }
    }

    enum class GroupType {
        PARALLEL,
        SEQUENTIAL
    }

}