package com.marsraver.LedFx.wled;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class WledColorRotator {

    private final List<WledDevice> devices;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, Boolean> devicePowerState = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private double hue = 0.0;

    public WledColorRotator(List<WledDevice> devices) {
        this.devices = devices;
    }

    public void start(double rotationSpeedDegreesPerStep, int intervalMillis) {
        if (running) return;
        running = true;

        // Print discovered device info
        System.out.println("\n✅ Found " + devices.size() + " WLED device(s):");
        for (var dev : devices) {
            System.out.printf(dev.toString());
        }

        System.out.println("\n🔍 Checking current power states...");
        updatePowerStates(true); // initial check with printout

        // Schedule power-state refreshes every 10 seconds
        scheduler.scheduleAtFixedRate(() -> updatePowerStates(false), 10, 10, TimeUnit.SECONDS);

        // Schedule color updates for "on" devices
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;

            Color c = Color.getHSBColor((float) (hue / 360.0), 1.0f, 1.0f);
            hue = (hue + rotationSpeedDegreesPerStep) % 360;

            for (var dev : devices) {
                Boolean isOn = devicePowerState.get(dev.ip);
                if (Boolean.TRUE.equals(isOn)) {
                    sendColor(dev, c);
                }
            }

        }, 0, intervalMillis, TimeUnit.MILLISECONDS);

        System.out.println("🎨 Started color rotation across " + devices.size() + " devices (power-aware).");
    }

    public void stop() {
        running = false;
        scheduler.shutdownNow();
        System.out.println("🛑 Stopped color rotation.");
    }

    private void updatePowerStates(boolean initial) {
        for (var dev : devices) {
            try {
                URL url = new URL("http://" + dev.ip + "/json/state");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(800);
                conn.setReadTimeout(1200);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    try (InputStream in = conn.getInputStream()) {
                        JsonNode state = mapper.readTree(in);
                        boolean isOn = state.path("on").asBoolean(true);
                        Boolean previous = devicePowerState.put(dev.ip, isOn);

                        if (initial) {
                            System.out.printf("%s Device %s (%s) is %s%n",
                                    isOn ? "💡" : "💤", dev.name, dev.ip, isOn ? "ON" : "OFF");
                        } else if (previous != null && previous != isOn) {
                            System.out.printf("🔄 Device %s (%s) changed power state: %s%n",
                                    dev.name, dev.ip, isOn ? "ON" : "OFF");
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                devicePowerState.put(dev.ip, false);
            }
        }
    }

    private void sendColor(WledDevice dev, Color color) {
        try {
            String urlStr = "http://" + dev.ip + "/json/state";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(400);
            conn.setReadTimeout(800);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();

            String payload = String.format("{\"seg\":[{\"col\":[[%d,%d,%d]]}]}", r, g, b);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload.getBytes());
                out.flush();
            }

            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
            System.err.println("⚠️ Failed to update " + dev.ip + ": " + e.getMessage());
        }
    }

    // Example usage
    public static void main(String[] args) throws Exception {
        System.out.println("🔍 Discovering WLED devices...");
        var devices = WledNetworkScanner.discover();

        if (devices.isEmpty()) {
            System.out.println("No WLED devices found.");
            return;
        }

        WledColorRotator rotator = new WledColorRotator(devices);
        rotator.start(3.0, 500);  // rotate hue by 3° every 500ms

        Thread.sleep(120000);     // run for 2 minutes
        rotator.stop();
    }
}

