package dev.amaro.bleh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice

data class SearchEvent(val eventType: String, val device: Device? = null) {

    constructor() : this(BluetoothAdapter.ACTION_DISCOVERY_STARTED) {}
    constructor(device: Device?) : this(BluetoothDevice.ACTION_FOUND, device) {}

}