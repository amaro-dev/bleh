package dev.amaro.bleh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.*
import java.util.*

class SearchRequest private constructor(
    private val timer: Timer,
    private val engine: SearchEngine,
    private val prefix: String?,
    private val signal: Short
) {
    private var mOperation: TimerOperation? = null
    private val mDevices: MutableList<Device> =
        ArrayList()
    private val mTimeoutListener: OnTimeoutListener = object : OnTimeoutListener {
        override fun onTimeout() {
            engine.stop()
        }
    }

    fun perform(): Flow<Device> {
        return engine.search()
            .onStart { mDevices.clear() }
            .onEach(detectStart())
            .map(extractDevice())
            .filterNotNull()
            .filter(discardByFilter())
            .onEach(collect())
    }

    private fun collect(): suspend (Device) -> Unit {
        return { device ->
            val index = mDevices.indexOfFirst { it.details.address == device.details.address }
            if (index == -1)
                mDevices.add(device)
            else
                mDevices[index] = device
        }
    }

    private fun discardByFilter(): suspend (Device) -> Boolean {
        return { device -> filterByPrefix(device) && filterBySignalStrength(device) }
    }

    private fun filterBySignalStrength(device: Device): Boolean {
        if (signal > 0) {
            val signal: Int = if (device.signal < 0) 100 + device.signal else device.signal.toInt()
            return signal.toShort() > this.signal
        }
        return true
    }

    private fun filterByPrefix(device: Device): Boolean {
        return if (prefix != null) {
            device.name.startsWith(prefix)
        } else true
    }


    private fun extractDevice(): suspend (SearchEvent) -> Device? {
        return { searchEvent ->
            if (BluetoothDevice.ACTION_FOUND == searchEvent.eventType) {
                searchEvent.device
            } else null
        }
    }

    private fun detectStart(): suspend (SearchEvent) -> Unit {
        return { searchEvent ->
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED == searchEvent.eventType) {
                mOperation = timer.countForSeconds(30, mTimeoutListener)
            }
        }
    }

    val devices: Array<Device>
        get() = mDevices.toTypedArray()

    fun stop() {
        mOperation?.run { timer.cancel(this) }
        engine.stop()
    }


    class Builder internal constructor(private val injector: Injector) {
        constructor(context: Context) : this(RealInjector(context))

        private var prefix: String? = null
        private var signal: Short = 0
        fun create(): SearchRequest {
            return SearchRequest(injector.timer(), injector.searchEngine(), prefix, signal)
        }

        fun filterByPrefix(prefix: String?): Builder {
            this.prefix = prefix
            return this
        }

        fun filterBySignal(signal: Short): Builder {
            this.signal = signal
            return this
        }
    }

}