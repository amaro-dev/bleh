package dev.amaro.bleh

import android.bluetooth.BluetoothDevice
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.*
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(JUnit4::class)
class SearchRequestTest {

    companion object {
        const val MAC_ADDRESS_1 = "11:22:33:AA:BB:CC"
        const val MAC_ADDRESS_2 = "11:22:33:AA:BB:DD"
        const val MAC_ADDRESS_3 = "11:22:33:AA:BB:EE"
        const val MAC_ADDRESS_4 = "11:22:33:AA:BB:FF"
    }

    private val mTimer: Timer = mockk(relaxed = true)

    private val mOperation: TimerOperation = mockk(relaxed = true)

    private val mEngine: SearchEngine = mockk(relaxed = true)

    private val injector: Injector = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { mTimer.countForSeconds(any(), any()) } returns mOperation
        every { injector.searchEngine() } returns mEngine
        every { injector.timer() } returns mTimer
        every { injector.btAdapter() } returns mockk(relaxed = true)
        setupStartEventWhenSearch()
    }

    @Test
    @Throws(Exception::class)
    fun timerIsNotStartedIfOnStartIsNotCalled() {
        val device: Device = mockk(relaxed = true)
        setFoundDevicesOnEngine(device)
        val request: SearchRequest = SearchRequest.Builder(injector).create()
        request.perform().consume()
        verify(exactly = 0) { mTimer.countForSeconds(any(), any()) }
    }

    @Test
    @Throws(Exception::class)
    fun whenFindDeviceInsertOnList() {
        val device: Device = mockk(relaxed = true)
        setFoundDevicesOnEngine(device)
        val request: SearchRequest = SearchRequest.Builder(injector).create()
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        Assert.assertEquals(device, request.devices[0])
    }

    @Test
    @Throws(Exception::class)
    fun `Do not repeat devices`() {
        setFoundDevicesOnEngine(
            newDevice("MP", -56, MAC_ADDRESS_1),
            newDevice("PAX123", -43, MAC_ADDRESS_2),
            newDevice("MP", 51, MAC_ADDRESS_1),
            newDevice("PAX123", 12, MAC_ADDRESS_2),
            newDevice("MP", 70, MAC_ADDRESS_3)
        )
        val request: SearchRequest = SearchRequest.Builder(injector).create()
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        assertEquals(3, request.devices.size)
        assertEquals(51.toShort(), request.devices[0].signal)
        assertEquals(12.toShort(), request.devices[1].signal)
        assertEquals(70.toShort(), request.devices[2].signal)
    }

    @Test
    @Throws(Exception::class)
    fun whenStartSearchClearsTheDeviceList() {
        val device: Device = mockk(relaxed = true)
        setFoundDevicesOnEngine(device)
        val request: SearchRequest = SearchRequest.Builder(injector).create()
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        Assert.assertEquals(1, request.devices.size)
    }

    @Test
    @Throws(Exception::class)
    fun whenTimeEndStopSearch() {
        setInstantaneousFinishOnTimer()
        val request: SearchRequest = SearchRequest.Builder(injector).create()
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        verify { mEngine.stop() }
    }

    @Test
    @Throws(Exception::class)
    fun doNotStopSearchBeforeTimeEnds() {
        val request: SearchRequest = SearchRequest.Builder(injector).create()
        request.perform().consume()
        verify(exactly = 0) { mEngine.stop() }
    }

    @Test
    @Throws(Exception::class)
    fun ifRequestToStopMustCallStopOnEngine() {
        val request: SearchRequest = SearchRequest.Builder(injector).create()
        request.stop()
        verify { mEngine.stop() }
    }

    @Test
    @Throws(Exception::class)
    fun ifRequestToStopTurnOffTimer() {
        val request = SearchRequest.Builder(injector)
            .create()
        request
            .perform()
            .onCompletion { println("Completed") }
            .onEmpty { request.stop() }
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        verify { mTimer.cancel(mOperation) }
    }

    @Test
    @Throws(Exception::class)
    fun searchDefaultDurationMustBe30Seconds() {
        val request: SearchRequest = SearchRequest.Builder(injector).create()
        request.perform().consume()
        verify { mTimer.countForSeconds(eq(30), any()) }
    }

    @Test
    @Throws(Exception::class)
    fun filterByName() {
        setFoundDevicesOnEngine(
            newDevice("MP", -12, MAC_ADDRESS_1),
            newDevice("PAX123", -12, MAC_ADDRESS_2),
            newDevice("MP2", -12, MAC_ADDRESS_3),
            newDevice("PAX324", -12, MAC_ADDRESS_4)
        )
        SearchRequest.Builder(injector)
            .filterByPrefix("PAX")
            .create()
            .perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
            .assertEventCount(2)
    }

    @Test
    @Throws(Exception::class)
    fun capturedResultsWhenFilterByName() {
        setFoundDevicesOnEngine(
            newDevice("MP", -12, MAC_ADDRESS_1),
            newDevice("PAX123", -12, MAC_ADDRESS_2),
            newDevice("MP2", -12, MAC_ADDRESS_3),
            newDevice("PAX324", -12, MAC_ADDRESS_4)
        )
        val request: SearchRequest = SearchRequest.Builder(injector)
            .filterByPrefix("PAX").create()
        request.perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        Assert.assertEquals(2, request.devices.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun filterBySignal() {
        setFoundDevicesOnEngine(
            newDevice("MP", -56, MAC_ADDRESS_1),
            newDevice("PAX123", -43, MAC_ADDRESS_2),
            newDevice("MP2", 51, MAC_ADDRESS_3),
            newDevice("PAX324", 12, MAC_ADDRESS_4)
        )
        SearchRequest.Builder(injector)
            .filterBySignal(50)
            .create()
            .perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
            .assertEventCount(2)
    }

    @Test
    @Throws(Exception::class)
    fun filterBySignalAndName() {
        setFoundDevicesOnEngine(
            newDevice("MP", -56, MAC_ADDRESS_1),
            newDevice("PAX123", -43, MAC_ADDRESS_2),
            newDevice("MP2", 51, MAC_ADDRESS_3),
            newDevice("PAX324", 12, MAC_ADDRESS_4)
        )
        SearchRequest.Builder(injector)
            .filterBySignal(50)
            .filterByPrefix("PAX")
            .create()
            .perform()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
            .assertEventCount(1)
    }

    private fun newDevice(name: String, signal: Short, address: String = MAC_ADDRESS_1): Device {
        val device: Device = mockk(relaxed = true)
        val deviceBT: BluetoothDevice = mockk(relaxed = true)
        every { device.name } returns name
        every { device.signal } returns signal
        every { deviceBT.address } returns address
        every { device.details } returns deviceBT
        return device
    }

    private fun setFoundDevicesOnEngine(vararg devices: Device) {
        every { mEngine.search() } returns devices.asFlow().map { SearchEvent(it) }
    }

    private fun setupStartEventWhenSearch() {
        every { mEngine.search() } returns flowOf(SearchEvent())
    }

    private fun setInstantaneousFinishOnTimer() {
        every { mTimer.countForSeconds(any(), any()) } answers {
            (this.args[1] as OnTimeoutListener).onTimeout()
            mOperation
        }
    }
}