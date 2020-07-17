package dev.amaro.bleh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import dev.amaro.broadcastflow.FlowCaster
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference
import java.util.concurrent.Callable

internal class SearchApi(context: WeakReference<Context>, adapter: BluetoothAdapter) :
    BluetoothApi(context, adapter) {
    private var mStopRequested = false

    fun stop() {
        adapter.cancelDiscovery()
        mStopRequested = true
    }

    fun search(): Flow<SearchEvent> {
        mStopRequested = false
        val setup = FlowCaster.BroadcastSetup(
            actions = listOf(
                BluetoothAdapter.ACTION_DISCOVERY_STARTED,
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
                BluetoothDevice.ACTION_FOUND
            ),
            startCommand = startSearch(),
            exitCondition = detectEndOfSearch(),
            emitExitEvent = false
        )
        return (context.get()?.let { FlowCaster.listen(it, setup) } ?: emptyFlow())
            .onEach { Logger.print("Raw event: $it") }
            .map(extractEvent())
            .onEach { Logger.print("Handled event: $it") }
            .filterNotNull()
    }

    private fun startSearch(): Callable<Flow<Intent>> {
        return Callable {
            adapter.startDiscovery()
            emptyFlow<Intent>()
        }
    }

    private fun detectEndOfSearch(): FlowCaster.ExitCondition {
        return object : FlowCaster.ExitCondition {
            override fun shouldExit(intent: Intent, eventNumber: Int): Boolean {
                return BluetoothAdapter.ACTION_DISCOVERY_FINISHED == intent.action && mStopRequested
            }
        }
    }

    private fun extractEvent(): (suspend (Intent) -> SearchEvent?) {
        return { intent ->
            val action = intent.action
            when {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED == action -> SearchEvent()
                BluetoothDevice.ACTION_FOUND == action -> SearchEvent(Device(intent))
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action -> {
                    adapter.startDiscovery()
                    null
                }
                else -> null
            }
        }
    }
}