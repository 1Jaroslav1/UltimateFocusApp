package com.ultimatefocus.app.util

class FakeBatteryProvider(
    private var _batteryLevel: Int = 75,
    private var _charging: Boolean = false,
) : BatteryProvider {
    override fun getBatteryLevel(): Int = _batteryLevel
    override fun isCharging(): Boolean = _charging

    fun setBatteryLevel(level: Int) { _batteryLevel = level }
    fun setCharging(charging: Boolean) { _charging = charging }
}
