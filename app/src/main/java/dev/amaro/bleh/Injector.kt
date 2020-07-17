package dev.amaro.bleh

import android.bluetooth.BluetoothAdapter
import android.content.Context
import java.lang.ref.WeakReference

internal interface Injector {
    fun timer(): Timer
    fun pairEngine(): PairEngine
    fun pairApi(): PairApi
    fun pairSystem(): PairingSystem
    fun searchEngine(): SearchEngine
    fun searchApi(): SearchApi
    fun btAdapter(): BluetoothAdapter
}

internal class RealInjector(context: Context) : Injector {

    private val context = WeakReference(context)

    override fun timer(): Timer = Timer()

    override fun pairEngine(): PairEngine = PairEngine(pairApi())

    override fun pairApi(): PairApi = PairApi(context, btAdapter(), pairSystem())

    override fun pairSystem(): PairingSystem = PairingSystem()

    override fun searchEngine(): SearchEngine = SearchEngine(searchApi())

    override fun searchApi(): SearchApi = SearchApi(context, btAdapter())

    override fun btAdapter(): BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

}