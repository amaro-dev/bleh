package dev.amaro.bleh

import android.bluetooth.BluetoothDevice

data class PairEvent(val event: String, val device: BluetoothDevice)