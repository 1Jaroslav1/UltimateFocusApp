package com.ultimatefocus.app.util

interface BatteryProvider {
    fun getBatteryLevel(): Int
    fun isCharging(): Boolean
}
