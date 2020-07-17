package dev.amaro.bleh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import dev.amaro.broadcastflow.FlowCaster
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference
import java.util.concurrent.Callable


internal class PairApi(
    context: WeakReference<Context>,
    adapter: BluetoothAdapter,
    val pairingSystem: PairingSystem
) : BluetoothApi(context, adapter) {

    fun sendTimeoutMessage(macAddress: String?) {
        val intent = Intent(ACTION_PAIRING_TIMEOUT)
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, adapter.getRemoteDevice(macAddress))
        context.get()?.sendBroadcast(intent)
    }

    fun sendErrorMessage(macAddress: String?) {
        val intent = Intent(ACTION_PAIRING_FAILED)
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, adapter.getRemoteDevice(macAddress))
        context.get()?.sendBroadcast(intent)
    }

    fun pair(macAddress: String): Flow<PairEvent> {
        val setup = FlowCaster.BroadcastSetup(
            actions = listOf(
                BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                ACTION_FAKE_PAIR_REQUEST,
                ACTION_PAIRING_FAILED,
                ACTION_PAIRING_TIMEOUT
            ),
            startCommand = startPairProcess(macAddress),
            exitCondition = detectPairCompleted(macAddress)
        )
        return context.get()?.let {
            FlowCaster.listen(it, setup)
                .filter(onlyEventsForThisDevice(macAddress))
                .map(detectError())
                .map(extractEvent())
                .filterNotNull()
        } ?: flowOf()
    }

    private fun startPairProcess(macAddress: String): Callable<Flow<Intent>> {
        return object : Callable<Flow<Intent>> {
            override fun call(): Flow<Intent> {
                return try {
                    // The method getRemoteDevice will always return a Device even if it doesn't exists
                    // https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html#getRemoteDevice
                    val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
                    if (device.bondState == BluetoothDevice.BOND_BONDED) {
                        val intent = Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                        intent.putExtra(
                            BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothDevice.BOND_BONDED
                        )
                        return flowOf(intent)
                    }
                    pairingSystem.pair(device)
                    val intent = Intent(ACTION_PAIRING_STARTED)
                    intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                    flowOf(intent)
                } catch (devicePairingFailed: DevicePairingFailed) {
                    flow { throw devicePairingFailed }
                }
            }
        }
    }

    private fun detectPairCompleted(macAddress: String): FlowCaster.ExitCondition {
        return object : FlowCaster.ExitCondition {
            override fun shouldExit(intent: Intent, eventNumber: Int): Boolean {
                val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                return device != null && macAddress == device.address && state == BluetoothDevice.BOND_BONDED
            }
        }
    }

    private fun onlyEventsForThisDevice(macAddress: String): suspend (Intent) -> Boolean {
        return { intent ->
            val device =
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            macAddress == device?.address
        }
    }

    private fun detectError(): suspend (Intent) -> Intent {
        return { intent ->
            val action = intent.action
            if (ACTION_PAIRING_FAILED == action || BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                throw DevicePairingFailed()
            } else if (ACTION_PAIRING_TIMEOUT == action) {
                context.get()?.run { pairingSystem.cancelPairRequest(this) }
                throw DevicePairingTimeout()
            } else {
                intent
            }
        }
    }

    private fun extractEvent(): suspend (Intent) -> PairEvent? {
        return { intent ->
            val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
            val action = intent.action
            val device =
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)!!
            when {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED == action &&
                        state == BluetoothDevice.BOND_BONDED ->
                    PairEvent(ACTION_PAIRING_SUCCEEDED, device)
                BluetoothDevice.ACTION_BOND_STATE_CHANGED == action &&
                        state == BluetoothDevice.BOND_NONE ->
                    PairEvent(ACTION_PAIRING_NOT_DONE, device)
                ACTION_PAIRING_STARTED == action -> PairEvent(ACTION_PAIRING_STARTED, device)
                ACTION_FAKE_PAIR_REQUEST == action || BluetoothDevice.ACTION_ACL_CONNECTED == action
                -> PairEvent(ACTION_PAIRING_ON_PROGRESS, device)
                else -> null
            }
        }
    }

    companion object {
        private const val TAG = "PairApi"
        const val ACTION_FAKE_PAIR_REQUEST =
            "android.bluetooth.device.action.PAIRING_REQUEST"
        const val ACTION_PAIRING_SUCCEEDED = "dev.amaro.bluetoothhelper.PAIRING_SUCCEEDED"
        const val ACTION_PAIRING_STARTED = "dev.amaro.bluetoothhelper.PAIRING_STARTED"
        const val ACTION_PAIRING_TIMEOUT = "dev.amaro.bluetoothhelper.PAIRING_TIMEOUT"
        const val ACTION_PAIRING_FAILED = "dev.amaro.bluetoothhelper.PAIRING_FAILED"
        const val ACTION_PAIRING_NOT_DONE = "dev.amaro.bluetoothhelper.PAIRING_NOT_DONE"
        const val ACTION_PAIRING_ON_PROGRESS =
            "android.bluetooth.device.action.PAIRING_ON_PROGRESS"
    }

}