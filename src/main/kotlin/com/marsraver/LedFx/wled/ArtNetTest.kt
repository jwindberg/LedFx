package com.marsraver.LedFx.wled

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color

/**
 * Test class to verify Art-Net communication with WLED devices.
 * Tests individual grids separately to debug communication issues.
 */
object ArtNetTest {
    private val log: Logger = LogManager.getLogger(ArtNetTest::class.java)
    @JvmStatic
    fun main(args: Array<String>) {
        println("Art-Net Test for WLED Devices")
        println("=============================\n")


        // Test Grid01 (Universe 0)
        testDevice("Grid01", "192.168.7.113", 256, 0)


        // Test Grid02 (Universe 1)
        testDevice("Grid02", "192.168.7.226", 256, 1)
    }

    private fun testDevice(name: String?, ip: String?, ledCount: Int, universe: Int) {
        println("Testing " + name + " (" + ip + ") on Universe " + universe)
        println("-----------------------------------")

        try {
            val controller = WledArtNetController(ip, ledCount, universe)


            // Test 1: Send all black
            println("Test 1: Sending all black LEDs...")
            val blackColors = IntArray(ledCount * 3)
            controller.sendLedData(blackColors)
            Thread.sleep(100)
            println("  ✓ Sent black data")


            // Test 2: Send all red
            println("Test 2: Sending all red LEDs...")
            val redColors = IntArray(ledCount * 3)
            for (i in 0..<ledCount) {
                redColors[i * 3] = 255 // R
                redColors[i * 3 + 1] = 0 // G
                redColors[i * 3 + 2] = 0 // B
            }
            controller.sendLedData(redColors)
            Thread.sleep(100)
            println("  ✓ Sent red data")


            // Test 3: Send all green
            println("Test 3: Sending all green LEDs...")
            val greenColors = IntArray(ledCount * 3)
            for (i in 0..<ledCount) {
                greenColors[i * 3] = 0 // R
                greenColors[i * 3 + 1] = 255 // G
                greenColors[i * 3 + 2] = 0 // B
            }
            controller.sendLedData(greenColors)
            Thread.sleep(100)
            println("  ✓ Sent green data")


            // Test 4: Send rainbow pattern
            println("Test 4: Sending rainbow pattern...")
            val rainbowColors = IntArray(ledCount * 3)
            for (i in 0..<ledCount) {
                val hue = (i / ledCount.toFloat()) * 360f
                val rgb = Color.HSBtoRGB(hue / 360f, 1.0f, 1.0f)
                rainbowColors[i * 3] = (rgb shr 16) and 0xFF // R
                rainbowColors[i * 3 + 1] = (rgb shr 8) and 0xFF // G
                rainbowColors[i * 3 + 2] = rgb and 0xFF // B
            }
            controller.sendLedData(rainbowColors)
            Thread.sleep(500)
            println("  ✓ Sent rainbow data")


            // Test 5: Turn off
            println("Test 5: Turning off LEDs...")
            controller.turnOff()
            println("  ✓ Turned off")

            println(name + " test completed successfully!\n")
        } catch (e: Exception) {
            log.error("❌ {} test failed: {}", name, e.message, e)
            e.printStackTrace()
            println()
        }
    }
}
