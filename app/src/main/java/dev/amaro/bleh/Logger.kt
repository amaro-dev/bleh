package dev.amaro.bleh

import android.util.Log

internal object Logger {
    private var isActive = false

    fun setOn() {
        isActive = true
    }

    fun setOff() {
        isActive = false
    }

    fun print(value: Any) {
        if (isActive)
            Log.d("BLEH_LOG", value.toString())
    }

}
