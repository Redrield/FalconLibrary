package org.ghrobotics.lib.subsystems.drive

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.DemandType
import org.ghrobotics.lib.commands.FalconCommand
import org.ghrobotics.lib.mathematics.twodim.geometry.Pose2dWithCurvature
import org.ghrobotics.lib.mathematics.twodim.trajectory.types.TimedTrajectory
import org.ghrobotics.lib.utils.Source
import org.ghrobotics.lib.utils.observabletype.ObservableVariable

class FollowTrajectoryCommand(
        val driveSubsystem: TankDriveSubsystem,
        val trajectorySource: Source<TimedTrajectory<Pose2dWithCurvature>>
) : FalconCommand(driveSubsystem) {

    constructor(
            driveSubsystem: TankDriveSubsystem,
            trajectory: TimedTrajectory<Pose2dWithCurvature>
    ) : this(driveSubsystem, Source(trajectory))


    private val trajectoryFinished = ObservableVariable(false)

    init {
        finishCondition += trajectoryFinished
    }

    private val trajectoryFollower = driveSubsystem.trajectoryFollower

    override suspend fun initialize() {
        trajectoryFollower.resetTrajectory(trajectorySource())
        trajectoryFinished.value = false
    }

    override suspend fun execute() {
        val robotPosition = driveSubsystem.localization.robotPosition

        val output = trajectoryFollower.getOutputFromDynamics(robotPosition)

        driveSubsystem.leftMaster.set(
                ControlMode.Velocity,
                output.lSetpoint,
                DemandType.ArbitraryFeedForward,
                output.lfVoltage.value / 12.0
        )

        driveSubsystem.rightMaster.set(
                ControlMode.Velocity,
                output.rSetpoint,
                DemandType.ArbitraryFeedForward,
                output.rfVoltage.value / 12.0
        )

        trajectoryFinished.value = trajectoryFollower.isFinished
    }

    override suspend fun dispose() {
        driveSubsystem.leftMaster.set(ControlMode.PercentOutput, 0.0)
        driveSubsystem.rightMaster.set(ControlMode.PercentOutput, 0.0)
    }

}