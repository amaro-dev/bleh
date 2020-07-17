package dev.amaro.bleh

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Parcelable

data class Device(val details: BluetoothDevice, val name: String, val signal: Short) {

    constructor(intent: Intent) : this(
        intent.getParcelableExtra<Parcelable>(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice,
        intent.getStringExtra(BluetoothDevice.EXTRA_NAME) ?: "",
        intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, -99)
    )
}