package dev.amaro.bleh

import io.mockk.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@RunWith(JUnit4::class)
class SearchEngineTest {

    private val mSearchApi: SearchApi = mockk(relaxed = true)

    @Before
    fun setUp() {
        setSuccessfulSearchStart()
        setSuccessWhenTurnOnBluetooth()
        setSuccessWhenTurnOffBluetooth()
        setBluetoothOn()
    }

    @Test
    @Throws(Exception::class)
    fun whenAskToStopMustCallStopOnApi() {
        val engine = SearchEngine(mSearchApi)
        engine.stop()
        verify { mSearchApi.stop() }
    }

    @Test
    @Throws(Exception::class)
    fun whenSearchFindDeviceMustNotify() {
        val device: Device = mockk(relaxed = true)
        setDeviceToDeliverAsFound(device)
        val engine = SearchEngine(mSearchApi)
        engine.search()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
            .apply { assertEquals(device, values[0].device) }
    }

    @Test
    @Throws(Exception::class)
    fun whenSearchFinishMustNotify() {
        setSuccessfulSearchStop()
        val engine = SearchEngine(mSearchApi)
        engine.search()
            .test()
            .consume()
            .assertEventCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun whenSearchStartMustNotify() {
        val engine = SearchEngine(mSearchApi)
        engine.search()
            .onEach { println(it) }
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
            .assertValue(SearchEvent())
    }

    @Test
    @Throws(Exception::class)
    fun ifBluetoothIsOffMustTurnItOn() {
        setBluetoothOff()
        val engine = SearchEngine(mSearchApi)
        engine.search()
            .test()
            .consume()
        coVerify { mSearchApi.turnBluetoothOn() }
    }

    @Test
    @Throws(Exception::class)
    fun searchMustStartAfterBluetoothIsTurnedOn() {
        setBluetoothOff()
        val engine = SearchEngine(mSearchApi)
        engine.search()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
            .assertValue(SearchEvent())
    }

    @Test
    @Throws(Exception::class)
    fun searchMustResetBluetoothIfItsOn() {
        setBluetoothOn()
        val engine = SearchEngine(mSearchApi)
        engine.search()
            .test()
            .unlockWhenComplete()
            .consume()
            .waitResults()
        coVerify {
            mSearchApi.turnBluetoothOff()
            mSearchApi.turnBluetoothOn()
        }
    }

    private fun setDeviceToDeliverAsFound(device: Device) {
        every { mSearchApi.search() } returns flowOf(SearchEvent(device))
    }

    private fun setSuccessWhenTurnOnBluetooth() {
        coEvery { mSearchApi.turnBluetoothOn() } returns Unit
    }

    private fun setSuccessWhenTurnOffBluetooth() {
        coEvery { mSearchApi.turnBluetoothOff() } returns Unit
    }

    private fun setSuccessfulSearchStart() {
        every { mSearchApi.search() } returns flowOf(SearchEvent())
    }

    private fun setSuccessfulSearchStop() {
        every { mSearchApi.search() } returns emptyFlow()
    }

    private fun setBluetoothOn() {
        every { mSearchApi.isBluetoothOn } returns true
    }

    private fun setBluetoothOff() {
        every { mSearchApi.isBluetoothOn } returns false
    }
}