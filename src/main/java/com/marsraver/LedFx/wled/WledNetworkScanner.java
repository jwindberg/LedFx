package com.marsraver.LedFx.wled;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class WledNetworkScanner {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static List<WledDevice> discover() throws Exception {
        String subnet = detectLocalSubnet();
        if (subnet == null) {
            System.err.println("❌ Could not determine local subnet.");
            return Collections.emptyList();
        }

        System.out.println("📡 Detected subnet: " + subnet + ".x");
        List<WledDevice> devices = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(64);

        for (int i = 1; i < 255; i++) {
            final int host = i;
            pool.submit(() -> {
                String ip = subnet + "." + host;
                try {
                    URL url = new URL("http://" + ip + "/json/info");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(400);
                    conn.setReadTimeout(800);
                    conn.setRequestMethod("GET");

                    if (conn.getResponseCode() == 200) {
                        try (InputStream in = conn.getInputStream()) {
                            JsonNode json = MAPPER.readTree(in);
                            WledDevice dev = new WledDevice();
                            dev.ip = ip;
                            dev.name = json.path("name").asText("Unnamed");
                            dev.version = json.path("ver").asText("Unknown");
                            dev.ledCount = json.path("leds").path("count").asInt(-1);
                            dev.uptime = json.path("uptime").asLong(0);

                            devices.add(dev);
                            System.out.println("✅ Found " + dev);
                        }
                    }
                    conn.disconnect();
                } catch (Exception ignored) {}
            });
        }

        pool.shutdown();
        pool.awaitTermination(25, TimeUnit.SECONDS);
        return devices;
    }

    private static String detectLocalSubnet() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;

            for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                InetAddress inetAddr = addr.getAddress();
                if (inetAddr instanceof Inet4Address && !inetAddr.isLoopbackAddress()) {
                    byte[] bytes = inetAddr.getAddress();
                    return String.format("%d.%d.%d",
                            bytes[0] & 0xFF,
                            bytes[1] & 0xFF,
                            bytes[2] & 0xFF);
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("🔍 Discovering WLED devices on local network...");
        List<WledDevice> devices = discover();
        System.out.printf("%n=== %d WLED devices found ===%n", devices.size());
        devices.forEach(System.out::println);
    }
}

