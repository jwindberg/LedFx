package com.marsraver.LedFx;

import com.marsraver.LedFx.wled.WledController;

/**
 * Simple test class to send known patterns to WLED devices.
 * This helps verify that the WLED communication is working correctly.
 */
public class WledTest {
    
    public static void main(String[] args) {
        System.out.println("WLED Test - Sending known patterns to devices");
        
        // Test with the two WLED devices
        String[] deviceIps = {"192.168.7.113", "192.168.7.226"};
        int ledCount = 256; // 16x16 grid
        
        for (String deviceIp : deviceIps) {
            System.out.println("\nTesting device: " + deviceIp);
            WledController controller = new WledController(deviceIp, ledCount);
            
            // Test 1: All red
            System.out.println("Test 1: All red LEDs");
            int[] redPattern = createSolidColorPattern(ledCount, 255, 0, 0); // All red
            boolean success1 = controller.sendLedData(redPattern);
            System.out.println("Red pattern sent: " + success1);
            
            // Wait 2 seconds
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            
            // Test 2: All green
            System.out.println("Test 2: All green LEDs");
            int[] greenPattern = createSolidColorPattern(ledCount, 0, 255, 0); // All green
            boolean success2 = controller.sendLedData(greenPattern);
            System.out.println("Green pattern sent: " + success2);
            
            // Wait 2 seconds
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            
            // Test 3: All blue
            System.out.println("Test 3: All blue LEDs");
            int[] bluePattern = createSolidColorPattern(ledCount, 0, 0, 255); // All blue
            boolean success3 = controller.sendLedData(bluePattern);
            System.out.println("Blue pattern sent: " + success3);
            
            // Wait 2 seconds
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            
            // Test 4: Rainbow pattern
            System.out.println("Test 4: Rainbow pattern");
            int[] rainbowPattern = createRainbowPattern(ledCount);
            boolean success4 = controller.sendLedData(rainbowPattern);
            System.out.println("Rainbow pattern sent: " + success4);
            
            // Wait 2 seconds
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            
            // Test 5: Turn off
            System.out.println("Test 5: Turn off all LEDs");
            int[] offPattern = createSolidColorPattern(ledCount, 0, 0, 0); // All black
            boolean success5 = controller.sendLedData(offPattern);
            System.out.println("Off pattern sent: " + success5);
        }
        
        System.out.println("\nWLED Test completed!");
    }
    
    /**
     * Creates a solid color pattern for all LEDs.
     */
    private static int[] createSolidColorPattern(int ledCount, int r, int g, int b) {
        int[] pattern = new int[ledCount * 3];
        for (int i = 0; i < ledCount; i++) {
            pattern[i * 3] = r;     // Red
            pattern[i * 3 + 1] = g; // Green
            pattern[i * 3 + 2] = b; // Blue
        }
        return pattern;
    }
    
    /**
     * Creates a rainbow pattern across all LEDs.
     */
    private static int[] createRainbowPattern(int ledCount) {
        int[] pattern = new int[ledCount * 3];
        for (int i = 0; i < ledCount; i++) {
            // Create rainbow colors
            float hue = (float) i / ledCount * 360.0f;
            int[] rgb = hsvToRgb(hue, 1.0f, 1.0f);
            
            pattern[i * 3] = rgb[0];     // Red
            pattern[i * 3 + 1] = rgb[1];  // Green
            pattern[i * 3 + 2] = rgb[2];  // Blue
        }
        return pattern;
    }
    
    /**
     * Converts HSV to RGB.
     */
    private static int[] hsvToRgb(float h, float s, float v) {
        int[] rgb = new int[3];
        
        int i = (int) (h / 60.0f);
        float f = h / 60.0f - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - s * f);
        float t = v * (1.0f - s * (1.0f - f));
        
        switch (i % 6) {
            case 0: rgb[0] = (int)(v * 255); rgb[1] = (int)(t * 255); rgb[2] = (int)(p * 255); break;
            case 1: rgb[0] = (int)(q * 255); rgb[1] = (int)(v * 255); rgb[2] = (int)(p * 255); break;
            case 2: rgb[0] = (int)(p * 255); rgb[1] = (int)(v * 255); rgb[2] = (int)(t * 255); break;
            case 3: rgb[0] = (int)(p * 255); rgb[1] = (int)(q * 255); rgb[2] = (int)(v * 255); break;
            case 4: rgb[0] = (int)(t * 255); rgb[1] = (int)(p * 255); rgb[2] = (int)(v * 255); break;
            case 5: rgb[0] = (int)(v * 255); rgb[1] = (int)(p * 255); rgb[2] = (int)(q * 255); break;
        }
        
        return rgb;
    }
}




