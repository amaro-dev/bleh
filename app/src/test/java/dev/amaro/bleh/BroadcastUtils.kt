package dev.amaro.bleh

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.mockk.every

fun sendDeviceFoundMessage(device: BluetoothDevice) {
    val intent = Intent(BluetoothDevice.ACTION_FOUND)
    intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
    context.sendBroadcast(intent)
}

fun sendDeviceFoundMessage(device: BluetoothDevice, name: String, signal: Int) {
    val intent = Intent(BluetoothDevice.ACTION_FOUND)
    intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
    intent.putExtra(BluetoothDevice.EXTRA_NAME, name)
    intent.putExtra(BluetoothDevice.EXTRA_RSSI, signal)
    context.sendBroadcast(intent)
}

fun sendSearchStartedMessage() {
    val intent = Intent(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
    context.sendBroadcast(intent)
}

fun sendSearchFinishedMessage() {
    val intent = Intent(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    context.sendBroadcast(intent)
}

fun sendBluetoothIsOffMessage() {
    val intent = Intent(BluetoothAdapter.ACTION_STATE_CHANGED)
    intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
    context.sendBroadcast(intent)
}

fun sendBluetoothIsOnMessage() {
    val intent = Intent(BluetoothAdapter.ACTION_STATE_CHANGED)
    intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON)
    context.sendBroadcast(intent)
}

fun sendConnectedMessage(device: BluetoothDevice, macAddress: String) {
    sendBroadcastMessage(device, "", macAddress, -1, BluetoothDevice.ACTION_ACL_CONNECTED)
}

fun sendPairRequestMessage(device: BluetoothDevice, macAddress: String) {
    sendBroadcastMessage(device, "", macAddress, -1, PairApi.ACTION_FAKE_PAIR_REQUEST)
}

fun sendDisconnectMessage(device: BluetoothDevice, macAddress: String) {
    sendBroadcastMessage(device, "", macAddress, -1, BluetoothDevice.ACTION_ACL_DISCONNECTED)
}
// TODO: Parar possível busca em andamento antes de parear (Talvez não seja aqui)
/**
 * O código abaixo parece fornecer uma solução para o problema do pareamento (API 19+)
 *
 *
 * Intent pairingIntent = new Intent(BluetoothDevice.ACTION_PAIRING_REQUEST);
 * pairingIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
 * pairingIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
 * BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION);
 * pairingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 * startActivityForResult(pairingIntent, REQUEST_BT_PAIRING);
 */
fun sendDeviceBounded(device: BluetoothDevice, name: String, macAddress: String) {
    sendBroadcastMessage(
        device,
        name,
        macAddress,
        BluetoothDevice.BOND_BONDED,
        BluetoothDevice.ACTION_BOND_STATE_CHANGED
    )
}

fun sendDeviceNoState(device: BluetoothDevice, name: String, macAddress: String) {
    sendBroadcastMessage(
        device,
        name,
        macAddress,
        -1,
        BluetoothDevice.ACTION_BOND_STATE_CHANGED
    )
}

fun sendDeviceNotBounded(device: BluetoothDevice, name: String, macAddress: String) {
    sendBroadcastMessage(
        device,
        name,
        macAddress,
        BluetoothDevice.BOND_NONE,
        BluetoothDevice.ACTION_BOND_STATE_CHANGED
    )
}

fun sendPairingFailed(device: BluetoothDevice, macAddress: String) {
    sendBroadcastMessage(device, "", macAddress, -1, PairApi.ACTION_PAIRING_FAILED)
}

fun sendTimeout(device: BluetoothDevice, macAddress: String) {
    sendBroadcastMessage(device, "", macAddress, -1, PairApi.ACTION_PAIRING_TIMEOUT)
}

fun sendBroadcastMessage(
    device: BluetoothDevice, name: String, macAddress: String,
    state: Int, action: String
) {
    val intent = Intent(action)
    every { device.address } returns macAddress
    intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
    intent.putExtra(BluetoothDevice.EXTRA_NAME, name)
    intent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, state)
    context.sendBroadcast(intent)
}


private val context = ApplicationProvider.getApplicationContext<Application>()