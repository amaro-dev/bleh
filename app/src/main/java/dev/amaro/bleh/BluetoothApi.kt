package dev.amaro.bleh

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import dev.amaro.broadcastflow.FlowCaster
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import java.lang.ref.WeakReference
import java.util.concurrent.Callable

internal open class BluetoothApi(
    protected val context: WeakReference<Context>,
    protected val adapter: BluetoothAdapter
) {

    suspend fun turnBluetoothOn() {
        val setup = FlowCaster.BroadcastSetup(
            actions = listOf(BluetoothAdapter.ACTION_STATE_CHANGED),
            startCommand = turnOn(),
            exitCondition = filterStateIs(BluetoothAdapter.STATE_ON),
            emitExitEvent = false
        )
        context.get()?.let { FlowCaster.listen(it, setup).collect() }
    }

    suspend fun turnBluetoothOff() {
        val setup = FlowCaster.BroadcastSetup(
            actions = listOf(BluetoothAdapter.ACTION_STATE_CHANGED),
            startCommand = turnOff(),
            exitCondition = filterStateIs(BluetoothAdapter.STATE_OFF),
            emitExitEvent = false
        )
        context.get()?.let { FlowCaster.listen(it, setup).collect() }
    }

    val isBluetoothOn: Boolean
        get() = adapter.isEnabled

    private fun turnOn(): Callable<Flow<Intent>> {
        return Callable {
            adapter.enable()
            flowOf<Intent>()
        }
    }

    private fun turnOff(): Callable<Flow<Intent>> {
        return Callable {
            adapter.disable()
            emptyFlow<Intent>()
        }
    }

    private fun filterStateIs(state: Int): FlowCaster.ExitCondition {
        return object : FlowCaster.ExitCondition {
            override fun shouldExit(intent: Intent, eventNumber: Int): Boolean {
                return intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == state
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothApi"
    }

}
