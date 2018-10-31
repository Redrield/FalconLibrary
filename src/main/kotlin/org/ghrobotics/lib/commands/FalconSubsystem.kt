package org.ghrobotics.lib.commands

import org.ghrobotics.lib.commands.impl.EmptyCommand
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

object SubsystemHandler {

    private val _subsystems = CopyOnWriteArrayList<FalconSubsystem>()
    val subsystems: List<FalconSubsystem> = _subsystems

    private var alreadyStarted = false

    fun addSubsystem(subsystem: FalconSubsystem) {
        if (alreadyStarted) throw IllegalStateException("You cannot add a subsystem after the initialize stage")
        _subsystems.add(subsystem)
        println("[FalconSubsystem Handler] Added ${subsystem.javaClass.simpleName}")
    }

    fun lateInit() = _subsystems.forEach { it.lateInit() }

    fun autoReset() = _subsystems.forEach { it.autoReset() }

    fun teleopReset() = _subsystems.forEach { it.teleopReset() }

    // https://www.chiefdelphi.com/forums/showthread.php?t=166814
    fun zeroOutputs() = _subsystems.forEach { it.zeroOutputs() }
}

abstract class FalconSubsystem(name: String? = null) {
    companion object {
        private val subsystemId = AtomicLong()
    }

    val name = name ?: "FalconSubsystem ${subsystemId.incrementAndGet()}"

    @Suppress("LeakingThis")
    var defaultCommand: FalconCommand = EmptyCommand(this)

    open fun lateInit() {}
    open fun autoReset() {}
    open fun teleopReset() {}
    open fun zeroOutputs() {}
}