package dev.amaro.bleh

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking


internal class PairEngine(private val mPairApi: PairApi) {
    fun pair(macAddress: String): Flow<PairEvent> {
        runBlocking {
            if (mPairApi.isBluetoothOn)
                mPairApi.turnBluetoothOff()
            mPairApi.turnBluetoothOn()
        }
        return mPairApi.pair(macAddress)
    }


    fun notifyTimeout(macAddress: String?) {
        mPairApi.sendTimeoutMessage(macAddress)
    }

    fun notifyError(macAddress: String?) {
        mPairApi.sendErrorMessage(macAddress)
    }

}