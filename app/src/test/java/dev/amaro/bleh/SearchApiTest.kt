package dev.amaro.bleh

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
class SearchApiTest {

    private var adapter: BluetoothAdapter = mockk(relaxed = true)
    private val context =
        WeakReference<Context>(ApplicationProvider.getApplicationContext<Application>())

    @Test
    fun checkBluetoothStatusOnAdapter() {
        val eventHandler = SearchApi(context, adapter)
        every { adapter.isEnabled } returns true
        assertEquals(true, eventHandler.isBluetoothOn)
    }

    @Test
    fun stopMethodMustCallCancelDiscovery() {
        val eventHandler = SearchApi(context, adapter)
        eventHandler.stop()
        verify { adapter.cancelDiscovery() }
    }

    @Test
    fun doNotStartNewSearchCycleAfterSearchFinishesIfStopRequestHasBeenSent() {
        val eventHandler = SearchApi(context, adapter)
        val observable: Flow<SearchEvent> = eventHandler.search()
        observable.test()
            .unlockWhenComplete()
            .consume()
            .apply {
                eventHandler.stop()
                sendSearchFinishedMessage()
            }
            .waitResults()
            .assertCompleted()
            .apply { verify(exactly = 1) { adapter.startDiscovery() } }
    }

    @Test
    fun keepSearchingAfterSearchFinishesIfNoStopRequestHasBeenSent() {
        val eventHandler = SearchApi(context, adapter)
        val observable: Flow<SearchEvent> = eventHandler.search()
        observable.consume()
        sendSearchFinishedMessage()
        verify(exactly = 2) { adapter.startDiscovery() }
    }

    @Test
    fun sendNotificationWhenSearchStart() {
        val eventHandler = SearchApi(context, adapter)
        val observable: Flow<SearchEvent> = eventHandler.search()
        observable.test()
            .unlockWithNrEvents(1).consume()
            .apply { sendSearchStartedMessage() }
            .waitResults()
            .assertValue(SearchEvent())
            .assertNotCompleted()
    }

    @Test
    fun sendNotificationWhenFindDevice() {
        val eventHandler = SearchApi(context, adapter)
        val device: BluetoothDevice = mockk()
        eventHandler.search().test()
            .unlockWithNrEvents(2)
            .consume()
            .apply {
                sendSearchStartedMessage()
                sendDeviceFoundMessage(device)
            }
            .waitResults()
            .assertEventCount(2)
            .assertNotCompleted()
            .apply { assertEquals(device, values[1].device!!.details) }
    }

    @Test
    fun retrieveDeviceNameAndSignal() {
        val eventHandler = SearchApi(context, adapter)
        eventHandler.search()
            .test()
            .unlockWithNrEvents(2)
            .consume()
            .apply {
                val device: BluetoothDevice = mockk(relaxed = true)
                sendSearchStartedMessage()
                sendDeviceFoundMessage(device, "Name", -34)
            }
            .waitResults()
            .apply {
                assertEquals("Name", values[1].device!!.name)
                assertEquals(-34, values[1].device!!.signal)
            }
    }

    @Test
    fun doNotSendNotificationWhenFinishSearchWithoutStopRequest() {
        val eventHandler = SearchApi(context, adapter)
        val observable: Flow<SearchEvent> = eventHandler.search()
        observable.test()
            .unlockWithNrEvents(1)
            .consume()
            .apply {
                sendSearchStartedMessage()
                sendSearchFinishedMessage()
            }
            .waitResults()
            .assertValue(SearchEvent())
            .assertNotCompleted()
    }

    @Test
    fun sendNotificationOnlyWhenFinishSearchAfterStopRequest() {
        val eventHandler = SearchApi(context, adapter)
        val observable: Flow<SearchEvent> = eventHandler.search()
        observable.test()
            .unlockWhenComplete()
            .consume()
            .apply {
                sendSearchStartedMessage()
                eventHandler.stop()
                sendSearchFinishedMessage()
            }
            .waitResults()
            .assertValue(SearchEvent())
            .assertCompleted()
    }

    @Test
    fun turnBluetoothOnMustCallItOnAdapter() = runBlockingTest {
        val eventHandler = SearchApi(context, adapter)
        val job = async { eventHandler.turnBluetoothOn() }
        sendBluetoothIsOnMessage()
        job.await()
        verify { adapter.enable() }
    }

    @Test
    fun turnBluetoothOffMustCallItOnAdapter() = runBlockingTest {
        val eventHandler = SearchApi(context, adapter)
        val job = async { eventHandler.turnBluetoothOff() }
        sendBluetoothIsOffMessage()
        job.await()
        verify { adapter.disable() }
    }


}