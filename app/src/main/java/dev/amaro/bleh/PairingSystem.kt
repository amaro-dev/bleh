package dev.amaro.bleh

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.util.Log

class PairingSystem {
    @Throws(DevicePairingFailed::class)
    fun pair(device: BluetoothDevice) {
        pairBond(device)
    }

    @Throws(DevicePairingFailed::class)
    private fun pairBond(device: BluetoothDevice) {
        try {
            device.javaClass.getMethod("createBond").invoke(device)
            Log.d(TAG, "Pair call succeeded")
        } catch (e: Exception) {
            throw DevicePairingFailed(e)
        }
    }

    fun cancelPairRequest(context: Context) {
        context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }

    companion object {
        private const val TAG = "PairingSystem"
    }
}