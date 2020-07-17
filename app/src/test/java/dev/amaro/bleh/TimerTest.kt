package dev.amaro.bleh


import io.mockk.spyk
import io.mockk.verify
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(JUnit4::class)
class TimerTest {
    var barrier = CountDownLatch(1)
    val mInteger = AtomicInteger()


    private val callback = spyk(object : OnTimeoutListener {
        override fun onTimeout() {
            mInteger.set(mInteger.get() * 2)
            barrier.countDown()
        }
    })

    @Test
    @Throws(Exception::class)
    fun callListenerAfterTimeout() {
        val timer = Timer()
        timer.countForSeconds(1, callback)
        verify(timeout = 1200) { callback.onTimeout() }
    }

    @Test
    @Throws(Exception::class)
    fun waitSpecifiedTimeBeforeCallListener() {
        val timer = Timer()
        mInteger.set(1)
        timer.countForSeconds(1, callback)
        mInteger.set(mInteger.get() + 1)
        barrier.await()
        Assert.assertEquals(4, mInteger.get().toLong())
    }

    @Test
    @Throws(Exception::class)
    fun incrementTimerDuration() {
        val timer = Timer()
        val operation = timer.countForSeconds(1, callback)
        operation.incrementBy(1)
        try {
            verify(timeout = 1200) { callback.onTimeout() }
            Assert.fail()
        } catch (e: Throwable) {
            // Garante que não foi chamado antes
        }
        verify(timeout = 2200) { callback.onTimeout() }
    }

    @Test
    @Throws(Exception::class)
    fun resetTimerCounter() {
        val timer = Timer()
        val millis = System.currentTimeMillis()
        val latch = CountDownLatch(1)
        val operation = timer.countForSeconds(2, object : OnTimeoutListener {
            override fun onTimeout() {
                latch.countDown()
            }
        })
        try {
            verify(timeout = 2200) { callback.onTimeout() }
            Assert.fail()
        } catch (e: Throwable) {
            // Garante que não foi chamado antes
        }
        operation.resetTime()
        latch.await()
        val now = System.currentTimeMillis()
        assertThat((now - millis), isBiggerThan(2200))
    }

    @Test
    @Throws(Exception::class)
    fun stopAvoidListenerToBeCalled() {
        val timer = Timer()
        val operation = timer.countForSeconds(1, callback)
        timer.cancel(operation)
        barrier.await(2, TimeUnit.SECONDS)
        Assert.assertEquals(1, barrier.count)
    }

    @Test
    @Throws(Exception::class)
    fun whenTimerFinishYouCanRunAgain() {
        val timer = Timer()
        timer.countForSeconds(1, callback)
        barrier.await()
        barrier = CountDownLatch(1)
        timer.countForSeconds(1, callback)
        barrier.await()
    }

    private fun isBiggerThan(value: Long): Matcher<Long> {
        return object : BaseMatcher<Long>() {
            override fun describeTo(description: Description?) {
                description?.appendText(" is bigger than '$value'")
            }

            override fun matches(item: Any?): Boolean {
                return item != null && (item is Long || item is Int) && (item as Long) > value
            }

        }
    }
}