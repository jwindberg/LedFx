package com.marsraver.LedFx.wled

import java.awt.Color
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import kotlin.math.min

/**
 * Client for controlling WLED devices via DDP (Distributed Display Protocol).
 * DDP is recommended for WLED as it has better performance and avoids Art-Net's
 * secondary color issues.
 */
class WledDdpClient @JvmOverloads constructor(val wledInfo: WledInfo, val port: Int = defaultDdpPort) {
    private var socket: DatagramSocket? = null
    private var debugLogged = false
    private var sequence = 0

    /**
     * Opens the UDP socket if it is not already open.
     */
    @Throws(SocketException::class)
    fun connect() {
        if (socket == null || socket!!.isClosed()) {
            socket = DatagramSocket()
        }
    }

    /**
     * Closes the UDP socket if open.
     */
    fun disconnect() {
        if (socket != null && !socket!!.isClosed()) {
            socket!!.close()
        }
    }

    /**
     * Sends raw RGB data to the device using DDP.
     *
     * @param rgbData array of RGB bytes (0-255) laid out as [R,G,B,R,G,B,...]
     * @param numLeds number of LEDs represented in rgbData
     * @return true if the data was sent successfully, false otherwise
     */
    fun sendRgb(rgbData: IntArray, numLeds: Int): Boolean {
        val activeSocket = socket
        check(!(activeSocket == null || activeSocket.isClosed())) { "Client not connected. Call connect() first." }

        require(rgbData.size >= numLeds * 3) { "RGB data array too small. Need at least " + (numLeds * 3) + " elements" }

        try {
            val address = InetAddress.getByName(wledInfo.ip)
            val ledsPerPacket: Int = maxLedsPerPacket
            val totalPackets = (numLeds + ledsPerPacket - 1) / ledsPerPacket

            for (packetNum in 0..<totalPackets) {
                val startLed = packetNum * ledsPerPacket
                val endLed = min(startLed + ledsPerPacket, numLeds)
                val ledsInPacket = endLed - startLed

                val packetRgbData = IntArray(ledsInPacket * 3)
                System.arraycopy(rgbData, startLed * 3, packetRgbData, 0, ledsInPacket * 3)

                // One-time debug logging of non-zero LEDs in the first packet
                if (!debugLogged && packetNum == 0) {
                    debugLogged = true
                    val sb = StringBuilder()
                    val maxDebugLeds = min(ledsInPacket, 20)
                    for (i in 0..<maxDebugLeds) {
                        val r = packetRgbData[i * 3]
                        val g = packetRgbData[i * 3 + 1]
                        val b = packetRgbData[i * 3 + 2]
                        if ((r or g or b) != 0) {
                            if (sb.length > 0) {
                                sb.append(", ")
                            }
                            sb.append("led=").append(startLed + i)
                                .append(" -> rgb(").append(r).append(",").append(g).append(",").append(b).append(")")
                        }
                    }
                    val name = wledInfo.name ?: wledInfo.ip ?: "unknown"
                    println(
                        ("DDP debug for " + name + " (packet " + packetNum + ", startLed=" + startLed
                                + "): " + (if (sb.length == 0) "<none>" else sb))
                    )
                }

                val packet = createDdpPacket(
                    packetRgbData, ledsInPacket, startLed,
                    packetNum == totalPackets - 1
                )
                val datagramPacket = DatagramPacket(packet, packet.size, address, port)
                activeSocket.send(datagramPacket)
            }

            // Increment sequence number for next frame
            sequence = (sequence + 1) and 0xFF
            return true
        } catch (e: IOException) {
            return false
        }
    }

    /**
     * Convenience API to send [Color] objects instead of raw RGB values.
     *
     * @param colors  array of Colors
     * @param numLeds number of LEDs to send
     * @return true if the data was sent successfully, false otherwise
     */
    fun sendColors(colors: Array<Color>, numLeds: Int): Boolean {
        require(colors.size >= numLeds) { "Color array too small. Need at least " + numLeds + " colors" }
        val rgbData = IntArray(numLeds * 3)
        for (i in 0..<numLeds) {
            val color = colors[i]
            rgbData[i * 3] = color.getRed()
            rgbData[i * 3 + 1] = color.getGreen()
            rgbData[i * 3 + 2] = color.getBlue()
        }
        return sendRgb(rgbData, numLeds)
    }

    /**
     * Sends a frame of all black (off) to the device.
     *
     * @param numLeds number of LEDs to turn off
     * @return true if the data was sent successfully, false otherwise
     */
    fun turnOff(numLeds: Int): Boolean {
        val black = IntArray(numLeds * 3)
        return sendRgb(black, numLeds)
    }

    /**
     * Builds a single DDP packet.
     *
     * DDP packet format (used by WLED):
     * Header (10 bytes):
     * - Flags (1 byte)
     * - Sequence (1 byte)
     * - Data type (1 byte) : 1 = RGB pixel data
     * - Destination ID (1 byte)
     * - Data offset (4 bytes, big-endian): starting byte offset
     * - Data length (2 bytes, big-endian): number of data bytes
     */
    private fun createDdpPacket(rgbData: IntArray, numLeds: Int, startLed: Int, isLast: Boolean): ByteArray {
        val flags = if (isLast) 0x40.toByte() else 0x00.toByte() // Push flag for last packet

        val dataOffset = startLed * 3 // DDP uses byte offset, not LED offset
        val dataLength = numLeds * 3 // Number of bytes of RGB data

        val packetLength = 10 + dataLength
        val packet = ByteArray(packetLength)
        var offset = 0

        // Header
        packet[offset++] = flags
        packet[offset++] = (sequence and 0xFF).toByte()
        packet[offset++] = 1 // Data type: RGB pixel data
        packet[offset++] = 1 // Destination ID: default

        // Data offset (4 bytes, big-endian)
        packet[offset++] = ((dataOffset shr 24) and 0xFF).toByte()
        packet[offset++] = ((dataOffset shr 16) and 0xFF).toByte()
        packet[offset++] = ((dataOffset shr 8) and 0xFF).toByte()
        packet[offset++] = (dataOffset and 0xFF).toByte()

        // Data length (2 bytes, big-endian)
        packet[offset++] = ((dataLength shr 8) and 0xFF).toByte()
        packet[offset++] = (dataLength and 0xFF).toByte()

        // RGB data in standard RGB order
        for (i in 0..<numLeds) {
            val r = rgbData[i * 3]
            val g = rgbData[i * 3 + 1]
            val b = rgbData[i * 3 + 2]

            packet[offset++] = (r and 0xFF).toByte()
            packet[offset++] = (g and 0xFF).toByte()
            packet[offset++] = (b and 0xFF).toByte()
        }

        return packet
    }

    val isConnected: Boolean
        get() = socket != null && !socket!!.isClosed()

    companion object {
        const val defaultDdpPort: Int = 4048

        /**
         * DDP max payload is 1440 bytes; 10 bytes header + 1440 bytes pixel data.
         * With 3 bytes per LED, this yields 480 LEDs per packet. We keep the name
         * from the original Kotlin snippet for clarity.
         */
        const val maxLedsPerPacket: Int = 480
    }
}


