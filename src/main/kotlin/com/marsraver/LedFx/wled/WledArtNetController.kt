package com.marsraver.LedFx.wled

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class WledArtNetController @JvmOverloads constructor(
    val deviceIp: String?,
    val ledCount: Int,
    val universe: Int,
    val colorMapping: ColorMapping = ColorMapping.GBR
) {
    private val lastSendTime = AtomicLong(0)
    private var socket: DatagramSocket? = null
    private var targetAddress: InetAddress? = null

    // Debug: Track packet send count
    private val sendCount = AtomicInteger(0)

    init {
        try {
            this.targetAddress = InetAddress.getByName(deviceIp)
            this.socket = DatagramSocket()
            this.socket!!.soTimeout = 1
            log.debug(
                "✓ WledArtNetController initialized for {} (Universe {}, {})",
                deviceIp,
                universe, colorMapping.description
            )
        } catch (e: Exception) {
            log.error("Failed to create Art-Net socket for {}: {}", deviceIp, e.message)
        }
    }

    fun sendLedData(ledColors: IntArray): Boolean {
        val now = System.currentTimeMillis()
        val lastSend = lastSendTime.get()
        if (now - lastSend < MIN_SEND_INTERVAL_MS) {
            return true
        }

        if (!lastSendTime.compareAndSet(lastSend, now)) {
            return true
        }

        try {
            val packet = createArtNetPacket(ledColors)
            val datagram = DatagramPacket(
                packet, packet.size, targetAddress, ARTNET_PORT
            )
            socket!!.send(datagram)


            // Debug: Log every 60 frames (~1 second at 60 FPS)
            val count = sendCount.incrementAndGet()
            if (count % 60 == 0) {
                log.debug(
                    "\uD83D\uDCE1 Sent {} packets to {} (Universe {})",
                    count,
                    deviceIp,
                    universe
                )
            }

            return true
        } catch (e: IOException) {
            log.error("Error sending Art-Net data to {} (Universe {}): {}", deviceIp, universe, e.message)
            return false
        }
    }

    private fun createArtNetPacket(ledColors: IntArray): ByteArray {
        // DMX data length: 1 byte start code + 3 bytes per LED
        val dataLength = 1 + ledCount * 3

        // Total packet size: 18 bytes header + data length
        val packetSize = 18 + dataLength
        val packet = ByteArray(packetSize)
        var offset = 0

        // Art-Net header
        System.arraycopy(ARTNET_ID, 0, packet, offset, 8)
        offset += 8

        // OpCode: 0x5000 (ArtDMX)
        packet[offset++] = 0x00
        packet[offset++] = 0x50

        // Protocol version: 14
        packet[offset++] = 0x00
        packet[offset++] = PROTOCOL_VERSION.toByte()

        // Sequence (0 = no sequence)
        packet[offset++] = 0
        // Physical input (0)
        packet[offset++] = 0

        // Universe (2 bytes, little endian)
        packet[offset++] = (universe and 0xFF).toByte()
        packet[offset++] = ((universe shr 8) and 0xFF).toByte()

        // Data length (2 bytes, big endian)
        packet[offset++] = ((dataLength shr 8) and 0xFF).toByte()
        packet[offset++] = (dataLength and 0xFF).toByte()

        // DMX start code (0)
        packet[offset++] = 0

        // Copy LED color data using the configured color mapping
        val maxLeds = min(ledCount, (packet.size - offset) / 3)
        var i = 0
        while (i < maxLeds && i * 3 + 2 < ledColors.size) {
            val colorIndex = i * 3
            packet[offset++] = colorMapping.mapChannel(colorIndex, 0, ledColors) // Channel 0
            packet[offset++] = colorMapping.mapChannel(colorIndex, 1, ledColors) // Channel 1
            packet[offset++] = colorMapping.mapChannel(colorIndex, 2, ledColors) // Channel 2
            i++
        }
        return packet
    }

    fun turnOff(): Boolean {
        try {
            // Send many black packets to ensure WLED receives and processes them
            // WLED may revert to a previous state if it stops receiving Art-Net data
            for (i in 0..4) {
                val blackColors = IntArray(ledCount * 3)
                val packet = createArtNetPacket(blackColors)
                val datagram = DatagramPacket(
                    packet, packet.size, targetAddress, ARTNET_PORT
                )
                socket!!.send(datagram)
                Thread.sleep(50) // Longer delay to ensure packets are processed
            }
            return true
        } catch (e: Exception) {
            log.error("Error turning off WLED device via Art-Net: {}", e.message)
            return false
        }
    }

    fun close() {
        if (socket != null && !socket!!.isClosed()) {
            socket!!.close()
        }
    }

    companion object {
        private const val MIN_SEND_INTERVAL_MS: Long = 8 // ~120 FPS max

        private const val ARTNET_PORT = 5568

        private val ARTNET_ID = "Art-Net\u0000".toByteArray()
        private const val OP_DMX: Short = 0x5000
        private const val PROTOCOL_VERSION: Short = 14
        private val log: Logger = LogManager.getLogger(WledArtNetController::class.java)
    }
}
