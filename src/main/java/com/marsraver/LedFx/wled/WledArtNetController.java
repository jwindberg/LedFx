package com.marsraver.LedFx.wled;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Log4j2
public class WledArtNetController {

    private final String deviceIp;
    private final int ledCount;
    private final int universe;
    private final ColorMapping colorMapping;

    private final AtomicLong lastSendTime = new AtomicLong(0);
    private static final long MIN_SEND_INTERVAL_MS = 8; // ~120 FPS max

    private DatagramSocket socket;
    private InetAddress targetAddress;
    private static final int ARTNET_PORT = 5568;

    private static final byte[] ARTNET_ID = "Art-Net\0".getBytes();
    private static final short OP_DMX = 0x5000;
    private static final short PROTOCOL_VERSION = 14;
    
    // Debug: Track packet send count
    private final AtomicInteger sendCount = new AtomicInteger(0);

    public WledArtNetController(String deviceIp, int ledCount, int universe) {
        this(deviceIp, ledCount, universe, ColorMapping.GBR);
    }

    public WledArtNetController(String deviceIp, int ledCount, int universe, ColorMapping colorMapping) {
        this.deviceIp = deviceIp;
        this.ledCount = ledCount;
        this.universe = universe;
        this.colorMapping = colorMapping;

        try {
            this.targetAddress = InetAddress.getByName(deviceIp);
            this.socket = new DatagramSocket();
            this.socket.setSoTimeout(1);
            log.debug("✓ WledArtNetController initialized for {} (Universe {}, {})", deviceIp, universe, colorMapping.getDescription());
        } catch (Exception e) {
            log.error("Failed to create Art-Net socket for {}: {}", deviceIp, e.getMessage());
        }
    }

    public boolean sendLedData(int[] ledColors) {
        long now = System.currentTimeMillis();
        long lastSend = lastSendTime.get();
        if (now - lastSend < MIN_SEND_INTERVAL_MS) {
            return true;
        }

        if (!lastSendTime.compareAndSet(lastSend, now)) {
            return true;
        }

        try {
            byte[] packet = createArtNetPacket(ledColors);
            DatagramPacket datagram = new DatagramPacket(
                packet, packet.length, targetAddress, ARTNET_PORT
            );
            socket.send(datagram);
            
            // Debug: Log every 60 frames (~1 second at 60 FPS)
            int count = sendCount.incrementAndGet();
            if (count % 60 == 0) {
                log.debug("\uD83D\uDCE1 Sent {} packets to {} (Universe {})", count, deviceIp, universe);
            }
            
            return true;
        } catch (IOException e) {
            log.error("Error sending Art-Net data to " + deviceIp + " (Universe " + universe + "): " + e.getMessage());
            return false;
        }
    }

    private byte[] createArtNetPacket(int[] ledColors) {
        // DMX data length: 1 byte start code + 3 bytes per LED
        int dataLength = 1 + ledCount * 3;

        // Total packet size: 18 bytes header + data length
        int packetSize = 18 + dataLength;
        byte[] packet = new byte[packetSize];
        int offset = 0;

        // Art-Net header
        System.arraycopy(ARTNET_ID, 0, packet, offset, 8);
        offset += 8;

        // OpCode: 0x5000 (ArtDMX)
        packet[offset++] = 0x00;
        packet[offset++] = 0x50;

        // Protocol version: 14
        packet[offset++] = 0x00;
        packet[offset++] = PROTOCOL_VERSION;

        // Sequence (0 = no sequence)
        packet[offset++] = 0;
        // Physical input (0)
        packet[offset++] = 0;

        // Universe (2 bytes, little endian)
        packet[offset++] = (byte) (universe & 0xFF);
        packet[offset++] = (byte) ((universe >> 8) & 0xFF);

        // Data length (2 bytes, big endian)
        packet[offset++] = (byte) ((dataLength >> 8) & 0xFF);
        packet[offset++] = (byte) (dataLength & 0xFF);

        // DMX start code (0)
        packet[offset++] = 0;

        // Copy LED color data using the configured color mapping
        int maxLeds = Math.min(ledCount, (packet.length - offset) / 3);
        for (int i = 0; i < maxLeds && i * 3 + 2 < ledColors.length; i++) {
            int colorIndex = i * 3;
            packet[offset++] = colorMapping.mapChannel(colorIndex, 0, ledColors); // Channel 0
            packet[offset++] = colorMapping.mapChannel(colorIndex, 1, ledColors); // Channel 1
            packet[offset++] = colorMapping.mapChannel(colorIndex, 2, ledColors); // Channel 2
        }
        return packet;
    }

    public boolean turnOff() {
        try {
            // Send many black packets to ensure WLED receives and processes them
            // WLED may revert to a previous state if it stops receiving Art-Net data
            for (int i = 0; i < 5; i++) {
                int[] blackColors = new int[ledCount * 3];
                byte[] packet = createArtNetPacket(blackColors);
                DatagramPacket datagram = new DatagramPacket(
                    packet, packet.length, targetAddress, ARTNET_PORT
                );
                socket.send(datagram);
                Thread.sleep(50); // Longer delay to ensure packets are processed
            }
            return true;
        } catch (Exception e) {
            log.error("Error turning off WLED device via Art-Net: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public int getLedCount() {
        return ledCount;
    }

    public int getUniverse() {
        return universe;
    }

    public ColorMapping getColorMapping() {
        return colorMapping;
    }
}
