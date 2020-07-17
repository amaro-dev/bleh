package dev.amaro.bleh

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Assert.assertNull
import java.util.concurrent.CountDownLatch
import kotlin.reflect.KClass

fun <T> Flow<T>.test(): TestFlow<T> {
    return TestFlow(this)
}

fun <T> Flow<T>.consume() {
    GlobalScope.async { collect() }
}

class TestFlow<T>(
    flow: Flow<T>,
    rule: (Flow<T>, CountDownLatch) -> Flow<T> = { f, _ -> f }
) {
    private val resultSignal = CountDownLatch(1)
    private var flow: Flow<T>

    init {
        val temp = flow
            .onCompletion { isCompleted = true }
            .onEach { values.add(it) }
            .catch {
                isFailed = true
                exception = it
            }
        this.flow = rule(temp, resultSignal)
    }

    var isCompleted: Boolean = false
        private set
    val values: MutableList<T> = mutableListOf()

    var isFailed: Boolean = false
        private set
    var exception: Throwable? = null
        private set

    fun unlockWithNrEvents(number: Int): TestFlow<T> = apply {
        flow = flow.onEach {
            if (values.size == number) resultSignal.countDown()
        }
    }

    fun unlockWhenComplete() = apply {
        flow = flow.onCompletion {
            resultSignal.countDown()
        }
    }

    fun unlockWhenFail() = apply {
        flow = flow.catch {
            resultSignal.countDown()
        }
    }

    fun waitResults() = apply { resultSignal.await() }

    fun consume() = apply {
        flow.consume()
    }

    fun assertCompleted() = apply {
        Assert.assertTrue(isCompleted)
    }

    fun assertNotCompleted() = apply {
        Assert.assertFalse(isCompleted)
    }

    fun assertValue(vararg events: T) = apply {
        Assert.assertThat(values, Matchers.containsInAnyOrder(*events))
    }

    fun assertEventCount(number: Int) = apply {
        Assert.assertThat(values.size, CoreMatchers.equalTo(number))
    }

    fun assertError(kClass: KClass<*>) {
        assertThat(exception, Matchers.instanceOf(kClass.java))
    }

    fun assertNoErrors() = apply {
        assertNull(exception)
    }

}