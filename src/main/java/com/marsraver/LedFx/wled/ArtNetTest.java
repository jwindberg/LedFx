package com.marsraver.LedFx.wled;

import lombok.extern.log4j.Log4j2;

import java.awt.Color;
import java.net.*;

/**
 * Test class to verify Art-Net communication with WLED devices.
 * Tests individual grids separately to debug communication issues.
 */
@Log4j2
public class ArtNetTest {
    
    public static void main(String[] args) {
        System.out.println("Art-Net Test for WLED Devices");
        System.out.println("=============================\n");
        
        // Test Grid01 (Universe 0)
        testDevice("Grid01", "192.168.7.113", 256, 0);
        
        // Test Grid02 (Universe 1)
        testDevice("Grid02", "192.168.7.226", 256, 1);
    }
    
    private static void testDevice(String name, String ip, int ledCount, int universe) {
        System.out.println("Testing " + name + " (" + ip + ") on Universe " + universe);
        System.out.println("-----------------------------------");
        
        try {
            WledArtNetController controller = new WledArtNetController(ip, ledCount, universe);
            
            // Test 1: Send all black
            System.out.println("Test 1: Sending all black LEDs...");
            int[] blackColors = new int[ledCount * 3];
            controller.sendLedData(blackColors);
            Thread.sleep(100);
            System.out.println("  ✓ Sent black data");
            
            // Test 2: Send all red
            System.out.println("Test 2: Sending all red LEDs...");
            int[] redColors = new int[ledCount * 3];
            for (int i = 0; i < ledCount; i++) {
                redColors[i * 3] = 255;     // R
                redColors[i * 3 + 1] = 0;   // G
                redColors[i * 3 + 2] = 0;   // B
            }
            controller.sendLedData(redColors);
            Thread.sleep(100);
            System.out.println("  ✓ Sent red data");
            
            // Test 3: Send all green
            System.out.println("Test 3: Sending all green LEDs...");
            int[] greenColors = new int[ledCount * 3];
            for (int i = 0; i < ledCount; i++) {
                greenColors[i * 3] = 0;     // R
                greenColors[i * 3 + 1] = 255; // G
                greenColors[i * 3 + 2] = 0;   // B
            }
            controller.sendLedData(greenColors);
            Thread.sleep(100);
            System.out.println("  ✓ Sent green data");
            
            // Test 4: Send rainbow pattern
            System.out.println("Test 4: Sending rainbow pattern...");
            int[] rainbowColors = new int[ledCount * 3];
            for (int i = 0; i < ledCount; i++) {
                float hue = (i / (float) ledCount) * 360f;
                int rgb = Color.HSBtoRGB(hue / 360f, 1.0f, 1.0f);
                rainbowColors[i * 3] = (rgb >> 16) & 0xFF;     // R
                rainbowColors[i * 3 + 1] = (rgb >> 8) & 0xFF;  // G
                rainbowColors[i * 3 + 2] = rgb & 0xFF;          // B
            }
            controller.sendLedData(rainbowColors);
            Thread.sleep(500);
            System.out.println("  ✓ Sent rainbow data");
            
            // Test 5: Turn off
            System.out.println("Test 5: Turning off LEDs...");
            controller.turnOff();
            System.out.println("  ✓ Turned off");
            
            System.out.println(name + " test completed successfully!\n");
            
        } catch (Exception e) {
            log.error("❌ " + name + " test failed: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
        }
    }
}
