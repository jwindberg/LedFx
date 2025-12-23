package com.marsraver.LedFx.wled

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

object WledNetworkScanner {
    private val MAPPER = ObjectMapper()

    @Throws(Exception::class)
    fun discover(): MutableList<WledDevice?> {
        val subnet = detectLocalSubnet()
        if (subnet == null) {
            System.err.println("❌ Could not determine local subnet.")
            return mutableListOf<WledDevice?>()
        }

        println("📡 Detected subnet: " + subnet + ".x")
        val devices: MutableList<WledDevice?> = CopyOnWriteArrayList<WledDevice?>()
        val pool = Executors.newFixedThreadPool(64)

        for (i in 1..254) {
            val host = i
            pool.submit(Runnable {
                val ip = subnet + "." + host
                try {
                    val url = URL("http://" + ip + "/json/info")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.setConnectTimeout(400)
                    conn.setReadTimeout(800)
                    conn.setRequestMethod("GET")

                    if (conn.getResponseCode() == 200) {
                        conn.getInputStream().use { `in` ->
                            val json = MAPPER.readTree(`in`)
                            val dev = WledDevice()
                            dev.ip = ip
                            dev.name = json.path("name").asText("Unnamed")
                            dev.version = json.path("ver").asText("Unknown")
                            dev.ledCount = json.path("leds").path("count").asInt(-1)
                            dev.uptime = json.path("uptime").asLong(0)

                            devices.add(dev)
                            println("✅ Found " + dev)
                        }
                    }
                    conn.disconnect()
                } catch (ignored: Exception) {
                }
            })
        }

        pool.shutdown()
        pool.awaitTermination(25, TimeUnit.SECONDS)
        return devices
    }

    @Throws(SocketException::class)
    private fun detectLocalSubnet(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue

            for (addr in iface.getInterfaceAddresses()) {
                val inetAddr = addr.getAddress()
                if (inetAddr is Inet4Address && !inetAddr.isLoopbackAddress()) {
                    val bytes = inetAddr.getAddress()
                    return String.format(
                        "%d.%d.%d",
                        bytes[0].toInt() and 0xFF,
                        bytes[1].toInt() and 0xFF,
                        bytes[2].toInt() and 0xFF
                    )
                }
            }
        }
        return null
    }

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        println("🔍 Discovering WLED devices on local network...")
        val devices = discover()
        System.out.printf("%n=== %d WLED devices found ===%n", devices.size)
        devices.forEach(Consumer { x: WledDevice? -> println(x) })
    }
}

