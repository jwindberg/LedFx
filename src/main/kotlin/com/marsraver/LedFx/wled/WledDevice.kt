package com.marsraver.LedFx.wled

class WledDevice {
    var ip: String? = null
    var name: String? = null
    var version: String? = null
    var ledCount: Int = 0
    var uptime: Long = 0
    var isOn: Boolean = false

    override fun toString(): String {
        val power = if (isOn) "ON" else "OFF"
        return String.format(
            "  • WLED '%s' (%s) – version %s, %d LEDs, uptime %ds, Power: %s",
            name, ip, version, ledCount, uptime, power
        )
    }
}

