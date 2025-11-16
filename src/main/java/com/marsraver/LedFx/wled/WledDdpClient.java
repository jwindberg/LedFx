package com.marsraver.LedFx.wled;

import java.awt.Color;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Client for controlling WLED devices via DDP (Distributed Display Protocol).
 * DDP is recommended for WLED as it has better performance and avoids Art-Net's
 * secondary color issues.
 */
public class WledDdpClient {

    private final WledInfo wledInfo;
    private final int port;

    private DatagramSocket socket;
    private boolean debugLogged = false;
    private int sequence = 0;

    public WledDdpClient(WledInfo wledInfo) {
        this(wledInfo, DDP_PORT);
    }

    public WledDdpClient(WledInfo wledInfo, int port) {
        this.wledInfo = wledInfo;
        this.port = port;
    }

    /**
     * Opens the UDP socket if it is not already open.
     */
    public void connect() throws SocketException {
        if (socket == null || socket.isClosed()) {
            socket = new DatagramSocket();
        }
    }

    /**
     * Closes the UDP socket if open.
     */
    public void disconnect() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Sends raw RGB data to the device using DDP.
     *
     * @param rgbData array of RGB bytes (0-255) laid out as [R,G,B,R,G,B,...]
     * @param numLeds number of LEDs represented in rgbData
     * @return true if the data was sent successfully, false otherwise
     */
    public boolean sendRgb(int[] rgbData, int numLeds) {
        DatagramSocket activeSocket = socket;
        if (activeSocket == null || activeSocket.isClosed()) {
            throw new IllegalStateException("Client not connected. Call connect() first.");
        }

        if (rgbData.length < numLeds * 3) {
            throw new IllegalArgumentException("RGB data array too small. Need at least " + (numLeds * 3) + " elements");
        }

        try {
            InetAddress address = InetAddress.getByName(wledInfo.getIp());
            int ledsPerPacket = LEDS_PER_PACKET;
            int totalPackets = (numLeds + ledsPerPacket - 1) / ledsPerPacket;

            for (int packetNum = 0; packetNum < totalPackets; packetNum++) {
                int startLed = packetNum * ledsPerPacket;
                int endLed = Math.min(startLed + ledsPerPacket, numLeds);
                int ledsInPacket = endLed - startLed;

                int[] packetRgbData = new int[ledsInPacket * 3];
                System.arraycopy(rgbData, startLed * 3, packetRgbData, 0, ledsInPacket * 3);

                // One-time debug logging of non-zero LEDs in the first packet
                if (!debugLogged && packetNum == 0) {
                    debugLogged = true;
                    StringBuilder sb = new StringBuilder();
                    int maxDebugLeds = Math.min(ledsInPacket, 20);
                    for (int i = 0; i < maxDebugLeds; i++) {
                        int r = packetRgbData[i * 3];
                        int g = packetRgbData[i * 3 + 1];
                        int b = packetRgbData[i * 3 + 2];
                        if ((r | g | b) != 0) {
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append("led=").append(startLed + i)
                              .append(" -> rgb(").append(r).append(",").append(g).append(",").append(b).append(")");
                        }
                    }
                    String name = wledInfo.getName() != null ? wledInfo.getName()
                            : (wledInfo.getIp() != null ? wledInfo.getIp() : "unknown");
                    System.out.println("DDP debug for " + name + " (packet " + packetNum + ", startLed=" + startLed
                            + "): " + (sb.length() == 0 ? "<none>" : sb));
                }

                byte[] packet = createDdpPacket(packetRgbData, ledsInPacket, startLed,
                        packetNum == totalPackets - 1);
                DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, address, port);
                activeSocket.send(datagramPacket);
            }

            // Increment sequence number for next frame
            sequence = (sequence + 1) & 0xFF;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Convenience API to send {@link Color} objects instead of raw RGB values.
     *
     * @param colors  array of Colors
     * @param numLeds number of LEDs to send
     * @return true if the data was sent successfully, false otherwise
     */
    public boolean sendColors(Color[] colors, int numLeds) {
        if (colors.length < numLeds) {
            throw new IllegalArgumentException("Color array too small. Need at least " + numLeds + " colors");
        }
        int[] rgbData = new int[numLeds * 3];
        for (int i = 0; i < numLeds; i++) {
            Color color = colors[i];
            rgbData[i * 3] = color.getRed();
            rgbData[i * 3 + 1] = color.getGreen();
            rgbData[i * 3 + 2] = color.getBlue();
        }
        return sendRgb(rgbData, numLeds);
    }

    /**
     * Sends a frame of all black (off) to the device.
     *
     * @param numLeds number of LEDs to turn off
     * @return true if the data was sent successfully, false otherwise
     */
    public boolean turnOff(int numLeds) {
        int[] black = new int[numLeds * 3];
        return sendRgb(black, numLeds);
    }

    /**
     * Builds a single DDP packet.
     *
     * DDP packet format (used by WLED):
     * Header (10 bytes):
     *  - Flags (1 byte)
     *  - Sequence (1 byte)
     *  - Data type (1 byte) : 1 = RGB pixel data
     *  - Destination ID (1 byte)
     *  - Data offset (4 bytes, big-endian): starting byte offset
     *  - Data length (2 bytes, big-endian): number of data bytes
     */
    private byte[] createDdpPacket(int[] rgbData, int numLeds, int startLed, boolean isLast) {
        byte flags = isLast ? (byte) 0x40 : (byte) 0x00; // Push flag for last packet

        int dataOffset = startLed * 3; // DDP uses byte offset, not LED offset
        int dataLength = numLeds * 3;  // Number of bytes of RGB data

        int packetLength = 10 + dataLength;
        byte[] packet = new byte[packetLength];
        int offset = 0;

        // Header
        packet[offset++] = flags;
        packet[offset++] = (byte) (sequence & 0xFF);
        packet[offset++] = 1; // Data type: RGB pixel data
        packet[offset++] = 1; // Destination ID: default

        // Data offset (4 bytes, big-endian)
        packet[offset++] = (byte) ((dataOffset >> 24) & 0xFF);
        packet[offset++] = (byte) ((dataOffset >> 16) & 0xFF);
        packet[offset++] = (byte) ((dataOffset >> 8) & 0xFF);
        packet[offset++] = (byte) (dataOffset & 0xFF);

        // Data length (2 bytes, big-endian)
        packet[offset++] = (byte) ((dataLength >> 8) & 0xFF);
        packet[offset++] = (byte) (dataLength & 0xFF);

        // RGB data in standard RGB order
        for (int i = 0; i < numLeds; i++) {
            int r = rgbData[i * 3];
            int g = rgbData[i * 3 + 1];
            int b = rgbData[i * 3 + 2];

            packet[offset++] = (byte) (r & 0xFF);
            packet[offset++] = (byte) (g & 0xFF);
            packet[offset++] = (byte) (b & 0xFF);
        }

        return packet;
    }

    public WledInfo getWledInfo() {
        return wledInfo;
    }

    public int getPort() {
        return port;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    public static int getDefaultDdpPort() {
        return DDP_PORT;
    }

    public static int getMaxLedsPerPacket() {
        return LEDS_PER_PACKET;
    }

    private static final int DDP_PORT = 4048;
    /**
     * DDP max payload is 1440 bytes; 10 bytes header + 1440 bytes pixel data.
     * With 3 bytes per LED, this yields 480 LEDs per packet. We keep the name
     * from the original Kotlin snippet for clarity.
     */
    private static final int LEDS_PER_PACKET = 480;
}


