/*
 * FRC Team 5190
 * Green Hope Falcons
 */

@file:Suppress("unused")

package org.ghrobotics.lib.commands

import org.ghrobotics.lib.commands.impl.ConditionalCommand
import org.ghrobotics.lib.utils.Source
import org.ghrobotics.lib.utils.withEquals

// External Extension Helpers

fun sequential(block: FalconCommandGroupBuilder.() -> Unit) =
        commandGroup(FalconCommandGroup.GroupType.SEQUENTIAL, block)

fun parallel(block: FalconCommandGroupBuilder.() -> Unit) =
        commandGroup(FalconCommandGroup.GroupType.PARALLEL, block)

fun commandGroup(type: FalconCommandGroup.GroupType, block: FalconCommandGroupBuilder.() -> Unit) =
        FalconCommandGroupBuilder(type).apply(block).build()

fun <T> stateCommandGroup(state: Source<T>, block: StateCommandGroupBuilder<T>.() -> Unit) =
        StateCommandGroupBuilder(state).apply(block).build()


// Builders

interface CommandGroupBuilder {
    fun build(): FalconCommandGroup
}

class FalconCommandGroupBuilder(val type: FalconCommandGroup.GroupType) :
        CommandGroupBuilder {
    private val commands = mutableListOf<FalconCommand>()

    operator fun FalconCommand.unaryPlus() = commands.add(this)

    override fun build() = FalconCommandGroup(type, commands)
}

class StateCommandGroupBuilder<T>(private val state: Source<T>) :
        CommandGroupBuilder {
    private val stateMap = mutableMapOf<T, FalconCommand>()

    fun state(vararg states: T, block: () -> FalconCommand) = state(states = *states, command = block())
    fun state(vararg states: T, command: FalconCommand) = states.forEach { state(it, command) }

    fun state(state: T, block: () -> FalconCommand) = state(state, block())
    fun state(state: T, command: FalconCommand) {
        if (stateMap.containsKey(state)) println("[StateCommandGroup] Warning: state $state was overwritten during building")
        stateMap[state] = command
    }

    override fun build() =
            FalconCommandGroup(FalconCommandGroup.GroupType.SEQUENTIAL,
                    stateMap.entries.map { (key, command) ->
                        ConditionalCommand(state.withEquals(key), command)
                    })
}


@Suppress("FunctionName", "UNUSED_PARAMETER")
infix fun FalconCommandGroup.S3ND(other: String) = this.start()
