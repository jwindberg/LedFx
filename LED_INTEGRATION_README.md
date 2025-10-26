# LedFx LED Integration

This document describes the LED integration features that allow sketches to control WLED devices in real-time.

## Overview

The LED integration extends the Processing-like sketch framework with the ability to:
- Map sketch pixels to a 16x16 LED grid
- Send color data to WLED devices in real-time
- Visualize the LED grid overlay in the sketch window
- Automatically handle LED communication at 60 FPS

## Components

### Core Classes

#### `WledController`
Handles communication with WLED devices via REST API:
- Sends LED color data to WLED device
- Manages device connection and error handling
- Supports both simple and advanced LED data formats

#### `LedGrid`
Represents a 16x16 LED matrix mapped to sketch pixels:
- Maps window pixels to LED positions
- Samples colors from sketch graphics
- Provides visual grid overlay
- Manages LED color data

#### `LedSketch` Interface
Extended sketch interface for LED support:
```java
public interface LedSketch extends Sketch {
    void init(int width, int height, LedGrid ledGrid);
    void draw(Graphics2D g, int width, int height, LedGrid ledGrid);
}
```

#### `LedSketchRunner`
Enhanced sketch runner with LED output:
- Manages both sketch window and LED communication
- Automatically sends LED data at 60 FPS
- Handles LED cleanup on exit

## Usage

### Basic LED Sketch

```java
public class MyLedSketch implements LedSketch {
    private LedGrid ledGrid;
    
    @Override
    public void init(int width, int height) {
        // Required by Sketch interface (not used in LED sketches)
    }
    
    @Override
    public void init(int width, int height, LedGrid ledGrid) {
        this.ledGrid = ledGrid;
        // Setup your sketch here
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height) {
        // Required by Sketch interface (not used in LED sketches)
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height, LedGrid ledGrid) {
        // Draw your graphics
        g.setColor(Color.RED);
        g.fillRect(100, 100, 50, 50);
        
        // Update LED colors based on your graphics
        updateLeds();
    }
    
    private void updateLeds() {
        // Set LED colors based on your sketch
        ledGrid.setLedColor(8, 8, Color.RED); // Center LED
        // LED data is automatically sent to WLED device
    }
}
```

### Running LED Sketches

#### Method 1: Using Gradle
```bash
# Run with default WLED device (192.168.7.226)
./gradlew run

# Run with custom WLED device
./gradlew runLed
```

#### Method 2: Using LedSketchLauncher
```java
// Run with specific WLED device
LedSketchLauncher.runLedSketch(
    new MyLedSketch(), 
    800, 600, 
    "My LED Sketch", 
    "192.168.7.226", 
    256
);

// Auto-discover WLED devices
LedSketchLauncher.runWithAutoDiscovery(
    new MyLedSketch(), 
    800, 600, 
    "My LED Sketch"
);
```

#### Method 3: Command Line
```bash
# Run with default settings
java -cp build/libs/LedFx-0.0.1-SNAPSHOT.jar com.marsraver.LedFx.LedSketchLauncher

# Run with custom WLED device
java -cp build/libs/LedFx-0.0.1-SNAPSHOT.jar com.marsraver.LedFx.LedSketchLauncher 192.168.7.226 256
```

## LED Grid Mapping

### Grid Layout
- **Size**: 16x16 LEDs (256 total)
- **Position**: Centered in sketch window, taking up ~50% of window size
- **Mapping**: Each LED corresponds to a grid of pixels in the window

### Pixel Mapping
```java
// Get grid information
int gridSize = ledGrid.getGridSize();        // 16
int pixelSize = ledGrid.getPixelSize();      // Size of each LED in pixels
int startX = ledGrid.getGridStartX();        // Grid start X position
int startY = ledGrid.getGridStartY();        // Grid start Y position

// Set LED color at specific grid position
ledGrid.setLedColor(x, y, color);           // x, y: 0-15
```

### Visual Grid Overlay
The LED grid is automatically drawn as an overlay in the sketch window:
- White grid lines show LED boundaries
- Yellow dots indicate LED positions
- Semi-transparent rectangle shows grid area

## WLED Device Communication

### Supported WLED API Endpoints
- `POST /json/state` - Send LED data and device state
- Automatic error handling and reconnection
- Support for both simple and advanced LED data formats

### LED Data Format
LEDs are sent as RGB values in the format expected by WLED:
```java
// Each LED is represented as 3 values: R, G, B
int[] ledColors = new int[256 * 3]; // 256 LEDs * 3 colors

// Set LED at position (x, y) to red
int ledIndex = (y * 16 + x) * 3;
ledColors[ledIndex] = 255;     // Red
ledColors[ledIndex + 1] = 0;   // Green
ledColors[ledIndex + 2] = 0;   // Blue
```

## Example: Bouncing Ball with LEDs

The included `LedExampleSketch` demonstrates:
- Bouncing ball animation
- Real-time LED mapping
- Color animation with HSV transitions
- Glow effects on nearby LEDs
- Visual grid overlay

### Key Features:
- Ball position maps to LED grid
- Smooth color transitions
- Glow effect on surrounding LEDs
- Real-time WLED device communication

## Configuration

### Default Settings
- **WLED Device IP**: 192.168.7.226
- **LED Count**: 256 (16x16)
- **Window Size**: 800x600
- **Animation Rate**: 60 FPS
- **Grid Size**: ~50% of window

### Customization
You can customize the LED grid by modifying the `LedGrid` constructor:
```java
// The grid automatically scales to fit in half the window
// and centers itself
LedGrid ledGrid = new LedGrid(windowWidth, windowHeight, wledController);
```

## Troubleshooting

### Common Issues

1. **WLED Device Not Found**
   - Check device IP address
   - Ensure device is on the same network
   - Verify WLED is running and accessible

2. **LEDs Not Updating**
   - Check network connection
   - Verify WLED device is responding
   - Check console for error messages

3. **Grid Not Visible**
   - Grid overlay is drawn automatically
   - Check if grid is outside window bounds
   - Verify window size is sufficient

### Debug Information
The framework provides console output for debugging:
```
LED Grid initialized:
  Grid size: 16x16
  Pixel size: 25x25
  Grid position: (200, 150)
  Total grid size: 400x400
LED Sketch initialized with size: 800x600
WLED Device: 192.168.7.226
```

## Performance Considerations

- LED data is sent at 60 FPS
- Network latency may affect real-time performance
- Consider reducing update frequency for slower networks
- LED sampling is optimized for performance

## Future Enhancements

- Support for different LED grid sizes
- Multiple WLED device support
- LED pattern libraries
- Advanced color effects
- Audio-reactive LED patterns

