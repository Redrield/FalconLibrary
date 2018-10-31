package org.ghrobotics.lib.commands

import org.ghrobotics.lib.mathematics.units.Time
import org.ghrobotics.lib.mathematics.units.nanosecond

abstract class TimedFalconCommand(
        vararg requiredSubsystems: FalconSubsystem
) : FalconCommand(*requiredSubsystems) {

    private var lastExecute: Long = 0L

    override suspend fun internalInitialize() {
        super.internalInitialize()
        lastExecute = System.nanoTime()
    }

    final override suspend fun execute() {
        val newTime = System.nanoTime()
        timedExecute((newTime - lastExecute).nanosecond)
        lastExecute = newTime
    }

    protected open suspend fun timedExecute(deltaTime: Time) {}

}