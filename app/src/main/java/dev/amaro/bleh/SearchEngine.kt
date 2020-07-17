package dev.amaro.bleh

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

internal class SearchEngine(private val mSearchApi: SearchApi) {
    fun search(): Flow<SearchEvent> {
        return if (!mSearchApi.isBluetoothOn) {
            runBlocking {
                mSearchApi.turnBluetoothOn()
                mSearchApi.search()
            }
        } else {
            runBlocking {
                mSearchApi.turnBluetoothOff()
                mSearchApi.turnBluetoothOn()
                mSearchApi.search()
            }
        }
    }

    fun stop() {
        mSearchApi.stop()
    }

}