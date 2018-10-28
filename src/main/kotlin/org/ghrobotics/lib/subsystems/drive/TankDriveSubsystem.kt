package org.ghrobotics.lib.subsystems.drive

import org.ghrobotics.lib.commands.FalconSubsystem
import org.ghrobotics.lib.mathematics.twodim.control.TrajectoryFollower
import org.ghrobotics.lib.mathematics.twodim.geometry.Pose2dWithCurvature
import org.ghrobotics.lib.mathematics.twodim.trajectory.types.TimedTrajectory
import org.ghrobotics.lib.mathematics.twodim.trajectory.types.mirror
import org.ghrobotics.lib.mathematics.units.Length
import org.ghrobotics.lib.sensors.AHRSSensor
import org.ghrobotics.lib.wrappers.FalconSRX

abstract class TankDriveSubsystem : FalconSubsystem("Drive Subsystem") {
    abstract val leftMaster: FalconSRX<Length>
    abstract val rightMaster: FalconSRX<Length>

    abstract val ahrsSensor: AHRSSensor
    abstract val trajectoryFollower: TrajectoryFollower

    @Suppress("LeakingThis")
    val localization = TankDriveLocalization(this)

    fun followTrajectory(
        trajectory: TimedTrajectory<Pose2dWithCurvature>
    ) = FollowTrajectoryCommand(this, trajectory)

    fun followTrajectory(
        trajectory: TimedTrajectory<Pose2dWithCurvature>,
        pathMirrored: Boolean
    ) = followTrajectory(trajectory.let {
        if (pathMirrored) it.mirror() else it
    })

}