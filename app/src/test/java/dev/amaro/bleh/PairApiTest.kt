package dev.amaro.bleh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.amaro.bleh.PairApi.Companion.ACTION_PAIRING_ON_PROGRESS
import dev.amaro.bleh.PairApi.Companion.ACTION_PAIRING_STARTED
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference


/**
 * Durante o pareamento aguardamos o evento BOND_STATE_CHANGE para valor 12 (Sucesso)
 * Caso recebamos o valor 10 esperamos 3 segundos para enviar um erro
 * Se dentro destes 3 segundos recebermos um valor 11 aguardamos até o recebimento de outro 10 para retornar o erro
 */
@RunWith(RobolectricTestRunner::class)
class PairApiTest {

    private val mAdapter: BluetoothAdapter = mockk(relaxed = true)

    private val mPairingSystem: PairingSystem = mockk(relaxed = true)

    private val mDevice: BluetoothDevice = mockk(relaxed = true)

    private val context = WeakReference<Context>(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        every { mDevice.bondState } returns BluetoothDevice.BOND_NONE
        every { mDevice.address } returns MAC_ADDRESS_1
        every { mAdapter.getRemoteDevice(MAC_ADDRESS_1) } returns mDevice
    }

    @Test
    @Throws(Exception::class)
    fun pairMustCallDevicePairSystem() {
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .apply { sendDeviceBounded(mDevice, "PAX-12345678", MAC_ADDRESS_1) }
            .waitResults()
        verify { mPairingSystem.pair(mDevice) }
    }

    @Test
    @Throws(Exception::class)
    fun deviceIsSuccessfullyPaired() {
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .apply { sendDeviceBounded(mDevice, "PAX-12345678", MAC_ADDRESS_1) }
            .waitResults()
            .assertValue(
                PairEvent(ACTION_PAIRING_STARTED, mDevice),
                PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, mDevice)
            )
            .assertCompleted()
    }

    @Test
    @Throws(Exception::class)
    fun pairingSuccessAfterReceiveDeviceNotBounded() {
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .apply {
                sendDeviceNotBounded(mDevice, "PAX-12345678", MAC_ADDRESS_1)
                sendDeviceBounded(mDevice, "PAX-12345678", MAC_ADDRESS_1)
            }
            .waitResults()
            .assertValue(
                PairEvent(ACTION_PAIRING_STARTED, mDevice),
                PairEvent(PairApi.ACTION_PAIRING_NOT_DONE, mDevice),
                PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, mDevice)
            )
            .assertCompleted()
    }

    @Test
    @Throws(Exception::class)
    fun anotherDeviceIsPaired() {
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWithNrEvents(1)
            .consume()
            .apply { sendDeviceBounded(mDevice, "PAX-12345678", MAC_ADDRESS_2) }
            .waitResults()
            .assertValue(PairEvent(ACTION_PAIRING_STARTED, mDevice))
            .assertNotCompleted()
    }

    @Test
    @Throws(Exception::class)
    fun returnSuccessWhenDeviceAlreadyPaired() {
        every { mDevice.bondState } returns BluetoothDevice.BOND_BONDED
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
            .assertValue(PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, mDevice))
        verify(exactly = 0) { mPairingSystem.pair(mDevice) }
    }

    @Test
    @Throws(Exception::class)
    fun returnMessageInformingPairProcessHasStarted() {
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWithNrEvents(1)
            .consume()
            .waitResults()
            .assertValue(PairEvent(ACTION_PAIRING_STARTED, mDevice))
    }

    @Test
    @Throws(Exception::class)
    fun pairingFailed() {
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .apply { sendPairingFailed(mockk(relaxed = true), MAC_ADDRESS_1) }
            .waitResults()
            .assertValue(PairEvent(ACTION_PAIRING_STARTED, mDevice))
            .assertError(DevicePairingFailed::class)

    }

    @Test
    @Throws(Exception::class)
    fun cancelPairOperationSameAsTimeout() {
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .apply { sendTimeout(mockk(relaxed = true), MAC_ADDRESS_1) }
            .waitResults()
            .assertValue(PairEvent(ACTION_PAIRING_STARTED, mDevice))
            .assertError(DevicePairingTimeout::class)
    }

    @Test
    @Throws(Exception::class)
    fun pairingFailedWhenTryToPerform() {
        every { mPairingSystem.pair(any()) } throws DevicePairingFailed()
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
            .assertEventCount(0)
            .assertError(DevicePairingFailed::class)
    }

    @Test
    @Throws(Exception::class)
    fun sendBroadcastMessageForTimeout() {
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .apply { api.sendTimeoutMessage(MAC_ADDRESS_1) }
            .waitResults()
            .assertError(DevicePairingTimeout::class)
    }

    @Test
    @Throws(Exception::class)
    fun sendBroadcastMessageForError() {
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .apply { api.sendErrorMessage(MAC_ADDRESS_1) }
            .waitResults()
            .assertError(DevicePairingFailed::class)
    }

    @Test
    @Throws(Exception::class)
    fun translateDisconnectMessageAsError() {
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWhenComplete()
            .consume()
            .apply { sendDisconnectMessage(mockk(relaxed = true), MAC_ADDRESS_1) }
            .waitResults()
            .assertValue(PairEvent(ACTION_PAIRING_STARTED, mDevice))
            .assertError(DevicePairingFailed::class)
    }

    @Test
    @Throws(Exception::class)
    fun translatePairingRequestMessageAsOnProgress() {
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWithNrEvents(2)
            .consume()
            .apply { sendPairRequestMessage(mDevice, MAC_ADDRESS_1) }
            .waitResults()
            .assertValue(
                PairEvent(ACTION_PAIRING_STARTED, mDevice),
                PairEvent(ACTION_PAIRING_ON_PROGRESS, mDevice)
            )
    }

    @Test
    @Throws(Exception::class)
    fun translateConnectedMessageAsOnProgress() {
        val api = PairApi(context, mAdapter, mPairingSystem)
        api.pair(MAC_ADDRESS_1)
            .test()
            .unlockWithNrEvents(2)
            .consume()
            .apply { sendConnectedMessage(mDevice, MAC_ADDRESS_1) }
            .waitResults()
            .assertValue(
                PairEvent(ACTION_PAIRING_STARTED, mDevice),
                PairEvent(ACTION_PAIRING_ON_PROGRESS, mDevice)
            )
    }


    companion object {
        private const val MAC_ADDRESS_1 = "00:11:22:33:44:55"
        private const val MAC_ADDRESS_2 = "00:11:22:33:44:66"
    }
}