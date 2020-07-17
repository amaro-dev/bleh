package dev.amaro.bleh

import java.util.*

internal class TimerOperation(
    private var mDuration: Int,
    private var listener: OnTimeoutListener?
) : TimerTask() {
    private var mElapsed = 0
    private val mLock = Any()
    override fun run() {
        synchronized(mLock) {
            mElapsed++
            if (mElapsed >= mDuration) {
                cancel()
                listener?.onTimeout()
                listener = null
            }
        }
    }

    fun incrementBy(seconds: Int) {
        synchronized(mLock) { mDuration += seconds }
    }

    fun resetTime() {
        synchronized(mLock) { mElapsed = 0 }
    }

    companion object {
        private const val TAG = "TimerOperation"
    }

}