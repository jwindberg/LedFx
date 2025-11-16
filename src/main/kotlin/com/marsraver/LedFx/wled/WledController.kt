package com.marsraver.LedFx.wled

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min

/**
 * Controller for sending LED data to a WLED device.
 * Handles communication with the WLED REST API.
 */
class WledController(deviceIp: String, ledCount: Int) {
    /**
     * Gets the device IP address.
     *
     * @return the device IP
     */
    val deviceIp: String?

    /**
     * Gets the number of LEDs on the device.
     *
     * @return the LED count
     */
    val ledCount: Int

    // Performance optimizations
    private val lastSendTime = AtomicLong(0)
    private val cachedConnection: HttpURLConnection? = null
    private val baseUrl: String

    init {
        this.deviceIp = deviceIp
        this.ledCount = ledCount
        this.baseUrl = "http://" + deviceIp + "/json/state"
    }

    /**
     * Sends LED color data to the WLED device with frame rate limiting.
     *
     * @param ledColors Array of RGB values (3 values per LED: R, G, B)
     * @return true if successful, false otherwise
     */
    fun sendLedData(ledColors: IntArray): Boolean {
        // Frame rate limiting: only send if enough time has passed
        val now = System.currentTimeMillis()
        val lastSend = lastSendTime.get()
        if (now - lastSend < MIN_SEND_INTERVAL_MS) {
            return true // Skip this frame to maintain frame rate
        }

        if (!lastSendTime.compareAndSet(lastSend, now)) {
            return true // Another thread is sending, skip this frame
        }

        try {
            val url = URL(baseUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 100 // Reduced from default
            conn.readTimeout = 100 // Reduced from default
            conn.doOutput = true


            // Create JSON payload
            val payload: ObjectNode = MAPPER.createObjectNode()
            payload.put("on", true)
            payload.put("bri", 255) // Full brightness


            // Convert LED colors to WLED format - try individual RGB values
            val wledData = Array<IntArray?>(ledCount) { IntArray(3) }
            var i = 0
            while (i < ledCount && i * 3 + 2 < ledColors.size) {
                val r = ledColors[i * 3]
                val g = ledColors[i * 3 + 1]
                val b = ledColors[i * 3 + 2]

                wledData[i]!![0] = r
                wledData[i]!![1] = g
                wledData[i]!![2] = b

                i++
            }

            payload.set<JsonNode?>(
                "seg", MAPPER.createArrayNode().add(
                    MAPPER.createObjectNode()
                        .put("id", 0)
                        .put("start", 0)
                        .put("stop", ledCount)
                        .set<JsonNode?>("i", MAPPER.valueToTree<JsonNode?>(wledData))
                )
            )


            // Send the request
            val jsonBytes: ByteArray = MAPPER.writeValueAsBytes(payload)
            conn.outputStream.use { os ->
                os.write(jsonBytes)
            }
            val responseCode = conn.responseCode
            conn.disconnect()

            return responseCode == 200
        } catch (e: Exception) {
            log.error("Error sending LED data to {}: {}", deviceIp, e.message)
            return false
        }
    }

    /**
     * Sends LED color data using the WLED segment API format.
     * This method uses the correct WLED API format with segments and individual LED colors.
     *
     * @param ledColors Array of RGB values (3 values per LED: R, G, B)
     * @return true if successful, false otherwise
     */
    fun sendLedDataSimple(ledColors: IntArray): Boolean {
        try {
            val url = URL("http://" + deviceIp + "/json/state")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true


            // Create JSON payload
            val payload: ObjectNode = MAPPER.createObjectNode()
            payload.put("on", true)
            payload.put("bri", 255) // Full brightness


            // Create segment with individual LED colors
            val segment: ObjectNode = MAPPER.createObjectNode()
            segment.put("id", 0)
            segment.put("start", 0)
            segment.put("stop", ledCount)


            // Create array of hex color strings for each LED
            val hexColors = arrayOfNulls<String>(ledCount)
            var i = 0
            while (i < ledCount && i * 3 + 2 < ledColors.size) {
                val r = max(0, min(255, ledColors[i * 3]))
                val g = max(0, min(255, ledColors[i * 3 + 1]))
                val b = max(0, min(255, ledColors[i * 3 + 2]))


                // Format as hex string (e.g., "FF0000" for red)
                hexColors[i] = String.format("%02X%02X%02X", r, g, b)
                i++
            }

            segment.set<JsonNode?>("i", MAPPER.valueToTree<JsonNode?>(hexColors))


            // Add segment to segments array
            payload.set<JsonNode?>("seg", MAPPER.createArrayNode().add(segment))

            conn.outputStream.use { os ->
                os.write(MAPPER.writeValueAsBytes(payload))
            }
            val responseCode = conn.responseCode
            conn.disconnect()


            return responseCode == 200
        } catch (e: Exception) {
            log.error("Error sending LED data to {}: {}", deviceIp, e.message)
            return false
        }
    }

    /**
     * Turns off all LEDs on the device.
     *
     * @return true if successful, false otherwise
     */
    fun turnOff(): Boolean {
        try {
            val url = URL("http://" + deviceIp + "/json/state")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload: ObjectNode = MAPPER.createObjectNode()
            payload.put("on", false)

            conn.outputStream.use { os ->
                os.write(MAPPER.writeValueAsBytes(payload))
            }
            val responseCode = conn.responseCode
            conn.disconnect()

            return responseCode == 200
        } catch (e: Exception) {
            log.error("Error turning off WLED device {}: {}", deviceIp, e.message)
            return false
        }
    }

    companion object {
        private val MAPPER = ObjectMapper()
        private const val MIN_SEND_INTERVAL_MS: Long = 16 // ~60 FPS max
        private val log: Logger = LogManager.getLogger(WledController::class.java)
    }
}
