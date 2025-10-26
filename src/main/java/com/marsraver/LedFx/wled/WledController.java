package com.marsraver.LedFx.wled;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controller for sending LED data to a WLED device.
 * Handles communication with the WLED REST API.
 */
public class WledController {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String deviceIp;
    private final int ledCount;
    
    // Performance optimizations
    private final AtomicLong lastSendTime = new AtomicLong(0);
    private static final long MIN_SEND_INTERVAL_MS = 16; // ~60 FPS max
    private HttpURLConnection cachedConnection;
    private String baseUrl;
    
    public WledController(String deviceIp, int ledCount) {
        this.deviceIp = deviceIp;
        this.ledCount = ledCount;
        this.baseUrl = "http://" + deviceIp + "/json/state";
    }
    
    /**
     * Sends LED color data to the WLED device with frame rate limiting.
     * 
     * @param ledColors Array of RGB values (3 values per LED: R, G, B)
     * @return true if successful, false otherwise
     */
    public boolean sendLedData(int[] ledColors) {
        // Frame rate limiting: only send if enough time has passed
        long now = System.currentTimeMillis();
        long lastSend = lastSendTime.get();
        if (now - lastSend < MIN_SEND_INTERVAL_MS) {
            return true; // Skip this frame to maintain frame rate
        }
        
        if (!lastSendTime.compareAndSet(lastSend, now)) {
            return true; // Another thread is sending, skip this frame
        }
        
        try {
            URL url = new URL(baseUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(100); // Reduced from default
            conn.setReadTimeout(100); // Reduced from default
            conn.setDoOutput(true);
            
            // Create JSON payload
            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("on", true);
            payload.put("bri", 255); // Full brightness
            
            // Convert LED colors to WLED format - try individual RGB values
            int[][] wledData = new int[ledCount][3];
            for (int i = 0; i < ledCount && i * 3 + 2 < ledColors.length; i++) {
                int r = ledColors[i * 3];
                int g = ledColors[i * 3 + 1];
                int b = ledColors[i * 3 + 2];
                
                wledData[i][0] = r;
                wledData[i][1] = g;
                wledData[i][2] = b;
                
            }
            
            payload.set("seg", MAPPER.createArrayNode().add(
                MAPPER.createObjectNode()
                    .put("id", 0)
                    .put("start", 0)
                    .put("stop", ledCount)
                    .set("i", MAPPER.valueToTree(wledData))
            ));
            
            // Send the request
            byte[] jsonBytes = MAPPER.writeValueAsBytes(payload);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBytes);
            }
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            return responseCode == 200;
            
        } catch (Exception e) {
            System.err.println("Error sending LED data to " + deviceIp + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Sends LED color data using the WLED segment API format.
     * This method uses the correct WLED API format with segments and individual LED colors.
     * 
     * @param ledColors Array of RGB values (3 values per LED: R, G, B)
     * @return true if successful, false otherwise
     */
    public boolean sendLedDataSimple(int[] ledColors) {
        try {
            URL url = new URL("http://" + deviceIp + "/json/state");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            // Create JSON payload
            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("on", true);
            payload.put("bri", 255); // Full brightness
            
            // Create segment with individual LED colors
            ObjectNode segment = MAPPER.createObjectNode();
            segment.put("id", 0);
            segment.put("start", 0);
            segment.put("stop", ledCount);
            
            // Create array of hex color strings for each LED
            String[] hexColors = new String[ledCount];
            for (int i = 0; i < ledCount && i * 3 + 2 < ledColors.length; i++) {
                int r = Math.max(0, Math.min(255, ledColors[i * 3]));
                int g = Math.max(0, Math.min(255, ledColors[i * 3 + 1]));
                int b = Math.max(0, Math.min(255, ledColors[i * 3 + 2]));
                
                // Format as hex string (e.g., "FF0000" for red)
                hexColors[i] = String.format("%02X%02X%02X", r, g, b);
            }
            
            segment.set("i", MAPPER.valueToTree(hexColors));
            
            // Add segment to segments array
            payload.set("seg", MAPPER.createArrayNode().add(segment));
            
            // Send the request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(MAPPER.writeValueAsBytes(payload));
            }
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            
            return responseCode == 200;
            
        } catch (Exception e) {
            System.err.println("Error sending LED data to " + deviceIp + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Turns off all LEDs on the device.
     * 
     * @return true if successful, false otherwise
     */
    public boolean turnOff() {
        try {
            URL url = new URL("http://" + deviceIp + "/json/state");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("on", false);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(MAPPER.writeValueAsBytes(payload));
            }
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            return responseCode == 200;
            
        } catch (Exception e) {
            System.err.println("Error turning off WLED device: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the device IP address.
     * 
     * @return the device IP
     */
    public String getDeviceIp() {
        return deviceIp;
    }
    
    /**
     * Gets the number of LEDs on the device.
     * 
     * @return the LED count
     */
    public int getLedCount() {
        return ledCount;
    }
}
