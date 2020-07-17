package dev.amaro.bleh

import android.bluetooth.BluetoothDevice
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@RunWith(JUnit4::class)
class PairEngineTest {

    private val mPairApi: PairApi = mockk(relaxed = true)

    @Before
    fun setUp() {
        setSuccessWhenTurnOffBluetooth()
        setSuccessWhenTurnOnBluetooth()
    }

    @Test
    fun turnBluetoothOnBeforeStartPairing() {
        setBluetoothOff()
        val engine = PairEngine(mPairApi)
        engine.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()

        coVerify { mPairApi.turnBluetoothOn() }
    }

    @Test
    fun ifBluetoothIsOnTurnOffAndOnBeforeStartPairing() {
        setBluetoothOn()
        val engine = PairEngine(mPairApi)
        engine.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        coVerify {
            mPairApi.turnBluetoothOff()
            mPairApi.turnBluetoothOn()
        }
    }

    @Test
    fun returnPairedDeviceAfterSucceed() {
        val device: BluetoothDevice = mockk(relaxed = true)
        setPairRequestSucceeded(device)
        val engine = PairEngine(mPairApi!!)
        engine.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
            .assertValue(PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, device))
            .assertNoErrors()
    }

    @Test
    fun notifyTimeoutCallsSendTimeoutMessageOnApi() {
        val engine = PairEngine(mPairApi)
        engine.notifyTimeout(MAC_ADDRESS_1)
        verify { mPairApi.sendTimeoutMessage(eq(MAC_ADDRESS_1)) }
    }

    @Test
    fun notifyErrorCallsSendErrorMessageOnApi() {
        val engine = PairEngine(mPairApi)
        engine.notifyError(MAC_ADDRESS_1)
        verify { mPairApi.sendErrorMessage(eq(MAC_ADDRESS_1)) }
    }

    private fun setPairRequestSucceeded(device: BluetoothDevice) {
        coEvery { mPairApi.pair(any()) } returns flowOf(
            PairEvent(
                PairApi.ACTION_PAIRING_SUCCEEDED,
                device
            )
        )
    }

    private fun setBluetoothOn() {
        every { mPairApi.isBluetoothOn } returns true
    }

    private fun setBluetoothOff() {
        every { mPairApi.isBluetoothOn } returns false
    }

    private fun setSuccessWhenTurnOnBluetooth() {
        coEvery { mPairApi.turnBluetoothOn() } returns Unit
    }

    private fun setSuccessWhenTurnOffBluetooth() {
        coEvery { mPairApi.turnBluetoothOff() } returns Unit
    }

    companion object {
        private const val MAC_ADDRESS_1 = "00:11:22:33:44:55"
    }
}