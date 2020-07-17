package dev.amaro.bleh

class DevicePairingTimeout : Exception()
class DevicePairingFailed : Exception {
    constructor() : super() {}
    constructor(e: Exception?) : super(e) {}
}