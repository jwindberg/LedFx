package com.marsraver.LedFx

import com.marsraver.LedFx.wled.WledController

/**
 * Simple test class to send known patterns to WLED devices.
 * This helps verify that the WLED communication is working correctly.
 */
object WledTest {
    @JvmStatic
    fun main(args: Array<String>) {
        println("WLED Test - Sending known patterns to devices")


        // Test with the two WLED devices
        val deviceIps = arrayOf("192.168.7.113", "192.168.7.226")
        val ledCount = 256 // 16x16 grid

        for (deviceIp in deviceIps) {
            println("\nTesting device: " + deviceIp)
            val controller = WledController(deviceIp, ledCount)


            // Test 1: All red
            println("Test 1: All red LEDs")
            val redPattern = createSolidColorPattern(ledCount, 255, 0, 0) // All red
            val success1 = controller.sendLedData(redPattern)
            println("Red pattern sent: " + success1)


            // Wait 2 seconds
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
            }


            // Test 2: All green
            println("Test 2: All green LEDs")
            val greenPattern = createSolidColorPattern(ledCount, 0, 255, 0) // All green
            val success2 = controller.sendLedData(greenPattern)
            println("Green pattern sent: " + success2)


            // Wait 2 seconds
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
            }


            // Test 3: All blue
            println("Test 3: All blue LEDs")
            val bluePattern = createSolidColorPattern(ledCount, 0, 0, 255) // All blue
            val success3 = controller.sendLedData(bluePattern)
            println("Blue pattern sent: " + success3)


            // Wait 2 seconds
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
            }


            // Test 4: Rainbow pattern
            println("Test 4: Rainbow pattern")
            val rainbowPattern = createRainbowPattern(ledCount)
            val success4 = controller.sendLedData(rainbowPattern)
            println("Rainbow pattern sent: " + success4)


            // Wait 2 seconds
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
            }


            // Test 5: Turn off
            println("Test 5: Turn off all LEDs")
            val offPattern = createSolidColorPattern(ledCount, 0, 0, 0) // All black
            val success5 = controller.sendLedData(offPattern)
            println("Off pattern sent: " + success5)
        }

        println("\nWLED Test completed!")
    }

    /**
     * Creates a solid color pattern for all LEDs.
     */
    private fun createSolidColorPattern(ledCount: Int, r: Int, g: Int, b: Int): IntArray {
        val pattern = IntArray(ledCount * 3)
        for (i in 0..<ledCount) {
            pattern[i * 3] = r // Red
            pattern[i * 3 + 1] = g // Green
            pattern[i * 3 + 2] = b // Blue
        }
        return pattern
    }

    /**
     * Creates a rainbow pattern across all LEDs.
     */
    private fun createRainbowPattern(ledCount: Int): IntArray {
        val pattern = IntArray(ledCount * 3)
        for (i in 0..<ledCount) {
            // Create rainbow colors
            val hue = i.toFloat() / ledCount * 360.0f
            val rgb = hsvToRgb(hue, 1.0f, 1.0f)

            pattern[i * 3] = rgb[0] // Red
            pattern[i * 3 + 1] = rgb[1] // Green
            pattern[i * 3 + 2] = rgb[2] // Blue
        }
        return pattern
    }

    /**
     * Converts HSV to RGB.
     */
    private fun hsvToRgb(h: Float, s: Float, v: Float): IntArray {
        val rgb = IntArray(3)

        val i = (h / 60.0f).toInt()
        val f = h / 60.0f - i
        val p = v * (1.0f - s)
        val q = v * (1.0f - s * f)
        val t = v * (1.0f - s * (1.0f - f))

        when (i % 6) {
            0 -> {
                rgb[0] = (v * 255).toInt()
                rgb[1] = (t * 255).toInt()
                rgb[2] = (p * 255).toInt()
            }

            1 -> {
                rgb[0] = (q * 255).toInt()
                rgb[1] = (v * 255).toInt()
                rgb[2] = (p * 255).toInt()
            }

            2 -> {
                rgb[0] = (p * 255).toInt()
                rgb[1] = (v * 255).toInt()
                rgb[2] = (t * 255).toInt()
            }

            3 -> {
                rgb[0] = (p * 255).toInt()
                rgb[1] = (q * 255).toInt()
                rgb[2] = (v * 255).toInt()
            }

            4 -> {
                rgb[0] = (t * 255).toInt()
                rgb[1] = (p * 255).toInt()
                rgb[2] = (v * 255).toInt()
            }

            5 -> {
                rgb[0] = (v * 255).toInt()
                rgb[1] = (p * 255).toInt()
                rgb[2] = (q * 255).toInt()
            }
        }

        return rgb
    }
}




