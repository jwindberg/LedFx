package com.marsraver.LedFx.wled

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.Array
import kotlin.Double
import kotlin.Exception
import kotlin.Int
import kotlin.String
import kotlin.Throws
import kotlin.concurrent.Volatile

class WledColorRotator(private val devices: MutableList<WledDevice>) {
    private val mapper = ObjectMapper()
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    private val devicePowerState: MutableMap<String, Boolean> = ConcurrentHashMap()

    @Volatile
    private var running = false
    private var hue = 0.0

    fun start(rotationSpeedDegreesPerStep: Double, intervalMillis: Int) {
        if (running) return
        running = true

        // Print discovered device info
        log.debug("\n✅ Found " + devices.size + " WLED device(s):")
        for (dev in devices) {
            System.out.printf(dev.toString())
        }

        log.debug("\n🔍 Checking current power states...")
        updatePowerStates(true) // initial check with printout

        // Schedule power-state refreshes every 10 seconds
        scheduler.scheduleAtFixedRate({ updatePowerStates(false) }, 10, 10, TimeUnit.SECONDS)

        // Schedule color updates for "on" devices
        scheduler.scheduleAtFixedRate({
            if (!running) return@scheduleAtFixedRate
            val c = Color.getHSBColor((hue / 360.0).toFloat(), 1.0f, 1.0f)
            hue = (hue + rotationSpeedDegreesPerStep) % 360
            for (dev in devices) {
                val isOn = devicePowerState[dev.ip]
                if (isOn == true) {
                    sendColor(dev, c)
                }
            }
        }, 0, intervalMillis.toLong(), TimeUnit.MILLISECONDS)

        log.debug("🎨 Started color rotation across " + devices.size + " devices (power-aware).")
    }

    fun stop() {
        running = false
        scheduler.shutdownNow()
        log.debug("🛑 Stopped color rotation.")
    }

    private fun updatePowerStates(initial: Boolean) {
        for (dev in devices) {
            val ip = dev.ip ?: continue
            try {
                val url = URL("http://$ip/json/state")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 800
                conn.readTimeout = 1200
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    conn.inputStream.use { `in` ->
                        val state = mapper.readTree(`in`)
                        val isOn = state.path("on").asBoolean(true)
                        val previous = devicePowerState.put(ip, isOn)
                        if (initial) {
                            System.out.printf(
                                "%s Device %s (%s) is %s%n",
                                if (isOn) "💡" else "💤", dev.name, ip, if (isOn) "ON" else "OFF"
                            )
                        }
                        if (!initial && previous != null && previous != isOn) {
                            System.out.printf(
                                "🔄 Device %s (%s) changed power state: %s%n",
                                dev.name, ip, if (isOn) "ON" else "OFF"
                            )
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                devicePowerState[ip] = false
            }
        }
    }

    private fun sendColor(dev: WledDevice, color: Color) {
        try {
            val urlStr = "http://" + dev.ip + "/json/state"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.setConnectTimeout(400)
            conn.setReadTimeout(800)
            conn.setDoOutput(true)
            conn.setRequestMethod("POST")
            conn.setRequestProperty("Content-Type", "application/json")

            val r = color.getRed()
            val g = color.getGreen()
            val b = color.getBlue()

            val payload = String.format("{\"seg\":[{\"col\":[[%d,%d,%d]]}]}", r, g, b)

            conn.getOutputStream().use { out ->
                out.write(payload.toByteArray())
                out.flush()
            }
            conn.getResponseCode()
            conn.disconnect()
        } catch (e: Exception) {
            log.error("⚠️ Failed to update " + dev.ip + ": " + e.message)
        }
    }

    companion object {
        private val log: Logger = LogManager.getLogger(WledColorRotator::class.java)
        // Example usage
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            log.debug("🔍 Discovering WLED devices...")
            val devices = WledNetworkScanner.discover().filterNotNull().toMutableList()

            if (devices.isEmpty()) {
                log.debug("No WLED devices found.")
                return
            }

            val rotator = WledColorRotator(devices)
            rotator.start(3.0, 500) // rotate hue by 3° every 500ms

            Thread.sleep(120000) // run for 2 minutes
            rotator.stop()
        }
    }
}

