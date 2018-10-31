package org.ghrobotics.lib.commands

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.sendBlocking
import org.ghrobotics.lib.wrappers.FalconRobotBase

object FalconCommandHandler {

    private val allocationMap = mutableMapOf<FalconCommand, List<FalconSubsystem>>()

    private sealed class CommandEvent {
        object InitEvent : CommandEvent()
        class StartCommandEvent(val command: FalconCommand) : CommandEvent()
        class StopCommandEvent(val command: FalconCommand) : CommandEvent()
    }

    private val commandChannel = GlobalScope.actor<CommandEvent>(
            capacity = Channel.UNLIMITED,
            start = CoroutineStart.LAZY
    ) {
        internalStartDefaultCommands()
        for (event in channel) {
            // Process the event
            when (event) {
                is CommandEvent.StartCommandEvent -> internalStartCommand(event.command)
                is CommandEvent.StopCommandEvent -> internalStopCommand(event.command)
            }
            // Ensure all subsystems have commands running
            internalStartDefaultCommands()
        }
    }

    internal fun internalStart() {
        commandChannel.sendBlocking(CommandEvent.InitEvent)
    }

    fun start(command: FalconCommand) {
        require(FalconRobotBase.DEBUG || FalconRobotBase.INSTANCE.initialized) { "You cannot start commands during/before initialization!" }
        commandChannel.sendBlocking(CommandEvent.StartCommandEvent(command))
    }

    fun stop(command: FalconCommand) = commandChannel.sendBlocking(CommandEvent.StopCommandEvent(command))

    private suspend fun internalStartCommand(commandToStart: FalconCommand) {
        val commandNeeds = commandToStart.requiredSubsystems

        // Stop commands that have a subsystem in common
        allocationMap.filterValues { allocatedList ->
            allocatedList.any { commandNeeds.contains(it) }
        }.forEach { internalStopCommand(it.key) }

        // Start the command
        allocationMap[commandToStart] = commandNeeds
        commandToStart.internalStart {
            // feedback to stop the command when finished
            commandChannel.sendBlocking(CommandEvent.StopCommandEvent(commandToStart))
        }
    }

    private suspend fun internalStopCommand(commandToStop: FalconCommand) {
        allocationMap.remove(commandToStop)
        commandToStop.internalStop()
    }

    private suspend fun internalStartDefaultCommands() =
            SubsystemHandler.subsystems.asSequence()
                    .filterNot { subsystem -> allocationMap.any { it.value.contains(subsystem) } }
                    .forEach { internalStartCommand(it.defaultCommand) }

}