package org.ghrobotics.lib.commands.impl

import org.ghrobotics.lib.commands.FalconCommand
import org.ghrobotics.lib.commands.FalconSubsystem

class EmptyCommand(
        vararg requiredSubsystems: FalconSubsystem
) : FalconCommand(requiredSubsystems.toList()) {
    init {
        executeFrequency = 0
    }
}