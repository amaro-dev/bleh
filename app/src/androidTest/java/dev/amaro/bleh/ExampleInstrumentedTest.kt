package dev.amaro.bleh

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun searchTest() = runBlocking {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        Logger.setOn()
        val request = SearchRequest.Builder(appContext)
            .create()
        request.perform()
            .collect()
        request.devices.forEach {
            println(it)
        }
    }

    @Test
    fun pairTest() = runBlocking {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        Logger.setOn()
        val address = "04:D1:6E:5F:ED:12"
        val request = PairRequest(address, appContext)
        request.perform()
            .catch { println("Fail: ${it.message}") }
            .onEach { println("Event: $it") }
            .onCompletion { println("Success") }
            .collect()

    }

}