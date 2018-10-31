package org.ghrobotics.lib.commands

import kotlinx.coroutines.*
import org.ghrobotics.lib.utils.BooleanSource
import org.ghrobotics.lib.utils.loopFrequency
import org.ghrobotics.lib.utils.observabletype.*
import org.ghrobotics.lib.wrappers.FalconRobotBase

abstract class FalconCommand(
        val requiredSubsystems: List<FalconSubsystem>
) {
    constructor(vararg requiredSubsystems: FalconSubsystem) : this(requiredSubsystems.toList())

    init {
        if (!FalconRobotBase.DEBUG && FalconRobotBase.INSTANCE.initialized)
            println("[FalconCommand} [WARNING] It is not recommended to create commands after the robot has initialized!")
    }

    protected val finishCondition = FinishCondition()
    protected var executeFrequency = DEFAULT_FREQUENCY

    private lateinit var job: Job
    private var handle: ObservableHandle? = null

    internal fun internalStart(commandFinish: () -> Unit) {
        job = commandScope.launch {
            internalInitialize()
            if (finishCondition.value) {
                // Command ended, no need to start loop
                commandFinish()
            } else {
                handle = finishCondition.invokeOnceWhenTrue { commandFinish() }
                val frequency = executeFrequency
                if (frequency != 0) loopFrequency(frequency) {
                    internalExecute()
                }
            }
        }
    }

    internal suspend fun internalStop() {
        handle?.dispose()
        handle = null
        job.cancelAndJoin()
        internalDispose()
    }

    internal open suspend fun internalInitialize() = initialize()
    internal open suspend fun internalExecute() = execute()
    internal open suspend fun internalDispose() = dispose()

    protected open suspend fun initialize() {}
    protected open suspend fun execute() {}
    protected open suspend fun dispose() {}

    fun start() = FalconCommandHandler.start(this)
    fun stop() = FalconCommandHandler.stop(this)

    protected class FinishCondition private constructor(
            private val varReference: ObservableValueReference<Boolean>
    ) : ObservableValue<Boolean> by varReference {
        constructor() : this(ObservableValueReference(ObservableValue(false)))

        operator fun plusAssign(other: ObservableValue<Boolean>) {
            varReference.reference = varReference.reference or other
        }

        operator fun plusAssign(other: BooleanSource) = plusAssign(GlobalScope.updatableValue(block = other))

        fun set(other: ObservableValue<Boolean>) {
            varReference.reference = other
        }

        override fun toString() = "FINISH($varReference)[$value]"
    }

    companion object {
        const val DEFAULT_FREQUENCY = 50

        val commandScope = CoroutineScope(newFixedThreadPoolContext(2, "Command"))
    }

}



