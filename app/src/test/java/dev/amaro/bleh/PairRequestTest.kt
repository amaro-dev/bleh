package dev.amaro.bleh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PairRequestTest {


    private val mPairEngine: PairEngine = mockk(relaxed = true)

    private val mTimer: Timer = mockk(relaxed = true)

    private val mDevice: BluetoothDevice = mockk(relaxed = true)

    private val mAdapter: BluetoothAdapter = mockk(relaxed = true)

    private val mOperation: TimerOperation = mockk(relaxed = true)

    private val injector: Injector = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { injector.pairEngine() } returns mPairEngine
        every { injector.timer() } returns mTimer
        every { injector.btAdapter() } returns mAdapter
        every { mTimer.countForSeconds(any(), any()) } returns mOperation
    }

    @Test
    @Throws(Exception::class)
    fun pairRequestSuccessfullyCompleted() {
        val request = PairRequest(MAC_ADDRESS_1, injector)
        setDevicePairedMessage(mDevice)
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
            .assertNoErrors()
            .assertValue(mDevice)
    }

    @Test
    @Throws(Exception::class)
    fun sendTimeoutMessageWhenTimerTimeout() {
        val request = PairRequest(MAC_ADDRESS_1, injector)
        setLongWaitToDeliverMessage(mDevice)
        setTimerEndImmediately()
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        verify { mPairEngine.notifyTimeout(eq(MAC_ADDRESS_1)) }
    }

    @Test
    fun detectErrorCaseWhenPairingRequestWasReceivedButTheStateReceivedAfterWasNotBound() {
        val request = PairRequest(MAC_ADDRESS_1, injector)
        setPairNotDoneAfterRequest(mDevice)
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        verify { mPairEngine.notifyError(MAC_ADDRESS_1) }
    }

    @Test
    fun doNotThreatAsErrorWhenReceiveNotBondMessageBeforePairingRequestMessage() {
        val request = PairRequest(MAC_ADDRESS_1, injector)
        setDevicePairedMessage(mDevice)
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        verify(exactly = 0) { mPairEngine.notifyError(any()) }
    }

    @Test
    fun stopTimerWhenComplete() {
        val request = PairRequest(MAC_ADDRESS_1, injector)
        setPairNotDoneBeforeRequest(mDevice)
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        verify { mTimer.cancel(eq(mOperation)) }
    }

    @Test
    fun whenReceiveMessageInformingAboutPairOnProgressIncrementTimerBy5Seconds() {
        val request = PairRequest(MAC_ADDRESS_1, injector)
        setPairNotDoneAfterRequest(mDevice)
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        verify { mOperation.resetTime() }
    }

    @Test
    fun doNotStartTimerBeforeReceiveStartEvent() {
        val request = PairRequest(MAC_ADDRESS_1, injector)
        setNoStartMessage()
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        verify(exactly = 0) { mTimer.countForSeconds(any(), any()) }
    }

    private fun setTimerEndImmediately() {
        every { mTimer.countForSeconds(any(), any()) } answers {
            (args[1] as OnTimeoutListener).onTimeout()
            mOperation
        }
    }

    private fun setDevicePairedMessage(device: BluetoothDevice) {
        every { mPairEngine.pair(any()) } returns
                flowOf(PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, device))
    }

    private fun setNoStartMessage() {
        every { mPairEngine.pair(any()) } returns emptyFlow()
    }

    private fun setLongWaitToDeliverMessage(device: BluetoothDevice) {
        every { mPairEngine.pair(any()) } returns
                flowOf(PairEvent(PairApi.ACTION_PAIRING_STARTED, device))
                    .onEach { delay(500) }

    }

    private fun setPairNotDoneBeforeRequest(device: BluetoothDevice) {
        every { mPairEngine.pair(any()) } returns flowOf(
            PairEvent(PairApi.ACTION_PAIRING_STARTED, device),
            PairEvent(PairApi.ACTION_PAIRING_NOT_DONE, device),
            PairEvent(PairApi.ACTION_PAIRING_ON_PROGRESS, device)
        )

    }

    private fun setPairNotDoneAfterRequest(device: BluetoothDevice) {
        every { mPairEngine.pair(any()) } returns flowOf(
            PairEvent(PairApi.ACTION_PAIRING_STARTED, device),
            PairEvent(PairApi.ACTION_PAIRING_ON_PROGRESS, device),
            PairEvent(PairApi.ACTION_PAIRING_NOT_DONE, device)
        )
    }

    companion object {
        private const val MAC_ADDRESS_1 = "00:11:22:33:44:55"
    }
}