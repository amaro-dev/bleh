package dev.amaro.bleh

import android.bluetooth.BluetoothDevice
import android.content.Context
import dev.amaro.bleh.PairApi.Companion.ACTION_PAIRING_NOT_DONE
import dev.amaro.bleh.PairApi.Companion.ACTION_PAIRING_ON_PROGRESS
import dev.amaro.bleh.PairApi.Companion.ACTION_PAIRING_STARTED
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicReference

class PairRequest internal constructor(
    private val address: String,
    private val injector: Injector
) {
    constructor(address: String, context: Context) : this(address, RealInjector(context))

    private val mEngine: PairEngine = injector.pairEngine()
    private val mTimer: Timer = injector.timer()


    private val mTimeoutListener: OnTimeoutListener = object : OnTimeoutListener {
        override fun onTimeout() {
            mEngine.notifyTimeout(address)
        }
    }

    fun perform(): Flow<BluetoothDevice> {
        val opReference = AtomicReference<TimerOperation>()
        return mEngine.pair(address)
            .onEach(detectStart(opReference))
            .filter(detectError(opReference))
            .map(extractDevice())
            .onCompletion(stopTimer(opReference))
    }

    private fun detectStart(opReference: AtomicReference<TimerOperation>): suspend (PairEvent) -> Unit {
        return { pairEvent ->
            if (ACTION_PAIRING_STARTED == pairEvent.event) {
                opReference.set(mTimer.countForSeconds(20, mTimeoutListener))
            }
        }
    }

    private fun <T> stopTimer(opReference: AtomicReference<TimerOperation>):
            suspend FlowCollector<T>.(Throwable?) -> Unit {
        return { opReference.get()?.run { mTimer.cancel(this) } }
    }

    private fun detectError(operation: AtomicReference<TimerOperation>): suspend (PairEvent) -> Boolean {
        var mReceivedPairingRequest = false
        return { pairEvent ->
            if (mReceivedPairingRequest && ACTION_PAIRING_NOT_DONE == pairEvent.event) {
                mEngine.notifyError(address)
                false
            } else if (ACTION_PAIRING_ON_PROGRESS == pairEvent.event) {
                operation.get()?.resetTime()
                mReceivedPairingRequest = true
                true
            } else {
                true
            }
        }

    }

    private fun extractDevice(): suspend (PairEvent) -> BluetoothDevice {
        return { pairEvent -> pairEvent.device }
    }

}