package com.ultimatefocus.app.util

class FakeBatteryProvider(
    var batteryLevel: Int = 75,
    var charging: Boolean = false,
) : BatteryProvider {
    override fun getBatteryLevel(): Int = batteryLevel
    override fun isCharging(): Boolean = charging
}
