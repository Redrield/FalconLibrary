package org.ghrobotics.lib.wrappers

import edu.wpi.first.wpilibj.RobotBase
import edu.wpi.first.wpilibj.command.Scheduler
import edu.wpi.first.wpilibj.hal.HAL
import edu.wpi.first.wpilibj.livewindow.LiveWindow
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.ghrobotics.lib.commands.FalconCommandHandler
import org.ghrobotics.lib.commands.FalconSubsystem
import org.ghrobotics.lib.commands.SubsystemHandler
import org.ghrobotics.lib.utils.*
import org.ghrobotics.lib.utils.observabletype.ObservableValue
import org.ghrobotics.lib.utils.observabletype.ObservableVariable
import org.ghrobotics.lib.wrappers.hid.FalconHID

abstract class FalconRobotBase : RobotBase() {

    companion object {
        var DEBUG = true
            private set
        lateinit var INSTANCE: FalconRobotBase
            private set
    }

    init {
        @Suppress("LeakingThis")
        INSTANCE = this
        DEBUG = false
    }

    enum class Mode(private val any: Boolean = false) {
        NONE,
        ANY(true),
        DISABLED,
        AUTONOMOUS,
        TELEOP,
        TEST;

        val rawValues by lazy { if (any) enumValues<Mode>().toList() else listOf(this@Mode) }
    }

    // State Machine
    private val currentModeState = ObservableVariable(Mode.NONE)
    val currentModeStateValue: ObservableValue<Mode> = currentModeState
    val modeStateMachine: StateMachine<Mode> = StateMachine(currentModeState)

    fun onEnter(enterState: Mode, listener: SMEnterListener<Mode>) =
            modeStateMachine.onEnter(enterState.rawValues, listener)

    fun onLeave(leaveState: Mode, listener: SMLeaveListener<Mode>) =
            modeStateMachine.onLeave(leaveState.rawValues, listener)

    fun onTransition(fromState: Mode, toState: Mode, listener: SMTransitionListener<Mode>) =
            modeStateMachine.onTransition(fromState.rawValues, toState.rawValues, listener)

    fun onWhile(whileState: Mode, frequency: Int = 50, listener: SMWhileListener<Mode>) =
            modeStateMachine.onWhile(whileState.rawValues, frequency, listener)

    // Main Robot Code

    var initialized = false
        private set

    abstract suspend fun initialize()

    override fun startCompetition() = runBlocking {
        LiveWindow.setEnabled(false)
        // Disabled
        onWhile(Mode.DISABLED) { HAL.observeUserProgramDisabled() }
        // Autonomous
        onWhile(Mode.AUTONOMOUS) { HAL.observeUserProgramAutonomous() }
        // TeleOp
        onWhile(Mode.TELEOP) { HAL.observeUserProgramTeleop() }
        // Test
        onEnter(Mode.TEST) { LiveWindow.setEnabled(true) }
        onWhile(Mode.TEST) { HAL.observeUserProgramTest() }
        onLeave(Mode.TEST) { LiveWindow.setEnabled(false) }
        // Update Values
        onWhile(Mode.ANY) {
            SmartDashboard.updateValues()
            //            LiveWindow.updateValues()
        }

        onEnter(Mode.AUTONOMOUS) { SubsystemHandler.autoReset() }
        onEnter(Mode.TELEOP) { SubsystemHandler.teleopReset() }
        onEnter(Mode.DISABLED) { SubsystemHandler.zeroOutputs() }

        initialize()
        SubsystemHandler.lateInit()
        initialized = true
        FalconCommandHandler.internalStart()
        // Start up the default command
        println("[Robot] Initialized")
        // Update Commands
        onWhile(Mode.ANY) {
            Scheduler.getInstance().run()
        }

        // Tell the DS that the robot is ready to be enabled
        HAL.observeUserProgramStarting()

        while (isActive) {
            // Wait for new data to arrive
            m_ds.waitForData()

            val newMode = when {
                isDisabled -> Mode.DISABLED
                isAutonomous -> Mode.AUTONOMOUS
                isOperatorControl -> Mode.TELEOP
                isTest -> Mode.TEST
                else -> TODO("Robot in invalid mode!")
            }
            currentModeState.value = newMode
        }
    }

    // Helpers
    protected operator fun FalconSubsystem.unaryPlus() = SubsystemHandler.addSubsystem(this)

    protected suspend operator fun FalconHID<*>.unaryPlus() = onWhile(Mode.TELEOP) { update() }
}
