package org.ghrobotics.lib.commands.impl

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ghrobotics.lib.commands.FalconCommand
import org.ghrobotics.lib.mathematics.units.Time
import org.ghrobotics.lib.utils.Source
import org.ghrobotics.lib.utils.observabletype.ObservableVariable

class DelayCommand(
        private val delaySource: Source<Time>
) : FalconCommand() {

    constructor(delay: Time) : this(Source(delay))

    private val delayCondition = ObservableVariable(false)

    init {
        executeFrequency = 0
        finishCondition += delayCondition
    }

    private lateinit var job: Job

    override suspend fun initialize() {
        delayCondition.value = false
        job = commandScope.launch {
            delay(delaySource().millisecond.toLong())
            delayCondition.value = true
        }
    }

    override suspend fun dispose() {
        job.cancelAndJoin()
    }
}