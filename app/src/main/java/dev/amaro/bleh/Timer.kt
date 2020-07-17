package dev.amaro.bleh

import java.util.Timer

internal class Timer {
    private val mTimer = Timer()

    companion object {
        const val ONE_SEC: Long = 1000
    }

    fun countForSeconds(seconds: Int, listener: OnTimeoutListener): TimerOperation {
        val operation = TimerOperation(seconds, listener)
        mTimer.schedule(operation, ONE_SEC, ONE_SEC)
        return operation
    }

    fun cancel(operation: TimerOperation) {
        operation.cancel()
    }
}