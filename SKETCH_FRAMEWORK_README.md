# LedFx Sketch Framework

A Processing-like framework for creating animated sketches in Java. This framework allows you to create interactive graphics and animations by implementing a simple `Sketch` interface.

## Features

- **Simple Interface**: Just implement `init()` and `draw()` methods
- **60 FPS Animation**: Smooth animations with automatic frame timing
- **Java Swing Integration**: Built on top of Java's standard GUI toolkit
- **Anti-aliased Graphics**: High-quality rendering with smooth edges
- **Keyboard Controls**: Built-in ESC key to exit sketches

## Quick Start

### 1. Create a Sketch

Create a new class that implements the `Sketch` interface:

```java
import com.marsraver.LedFx.Sketch;
import java.awt.*;

public class MySketch implements Sketch {
    
    @Override
    public void init(int width, int height) {
        // Called once when the sketch starts
        // Set up your initial configuration here
    }
    
    @Override
    public void draw(Graphics2D g, int width, int height) {
        // Called 60 times per second
        // Draw your graphics here
    }
}
```

### 2. Run Your Sketch

Use the `SketchLauncher` to run your sketch:

```java
import com.marsraver.LedFx.SketchLauncher;
import com.marsraver.LedFx.Sketch;

public class Main {
    public static void main(String[] args) {
        Sketch mySketch = new MySketch();
        SketchLauncher.runSketch(mySketch, 800, 600, "My Awesome Sketch");
    }
}
```

## Example: Bouncing Ball

The framework includes an example sketch that demonstrates a bouncing ball animation. To run it:

```bash
./gradlew run
```

Or run the `SketchLauncher` class directly.

## API Reference

### Sketch Interface

#### `void init(int width, int height)`
Called once when the sketch starts. Use this to:
- Set up initial variables
- Configure colors, fonts, etc.
- Perform one-time setup tasks

**Parameters:**
- `width`: The width of the drawing canvas
- `height`: The height of the drawing canvas

#### `void draw(Graphics2D g, int width, int height)`
Called continuously at 60 FPS. Use this to:
- Draw graphics
- Update animation state
- Handle user input (if needed)

**Parameters:**
- `g`: The Graphics2D object for drawing
- `width`: Current width of the canvas
- `height`: Current height of the canvas

### SketchRunner Class

The `SketchRunner` manages the window and animation loop:

```java
SketchRunner runner = new SketchRunner(sketch, width, height, title);
runner.start();  // Start animation
runner.stop();   // Stop animation
```

### SketchLauncher Class

Utility class for running sketches:

```java
// Run with default settings
SketchLauncher.runExampleSketch();

// Run a custom sketch
SketchLauncher.runSketch(mySketch, 800, 600, "My Sketch");
```

## Drawing Tips

### Basic Shapes
```java
// Rectangle
g.fillRect(x, y, width, height);
g.drawRect(x, y, width, height);

// Circle/Ellipse
g.fillOval(x, y, width, height);
g.drawOval(x, y, width, height);

// Line
g.drawLine(x1, y1, x2, y2);
```

### Colors
```java
// Set color
g.setColor(Color.RED);
g.setColor(new Color(255, 0, 0));
g.setColor(new Color(255, 0, 0, 128)); // With alpha

// Fill background
g.setColor(Color.BLACK);
g.fillRect(0, 0, width, height);
```

### Text
```java
g.setFont(new Font("Arial", Font.BOLD, 24));
g.setColor(Color.WHITE);
g.drawString("Hello World!", x, y);
```

### Animation
```java
// Use instance variables to store state
private float angle = 0;

public void draw(Graphics2D g, int width, int height) {
    // Update state
    angle += 0.1;
    
    // Draw based on state
    g.rotate(angle);
    g.fillRect(0, 0, 50, 50);
}
```

## Building and Running

### Using Gradle
```bash
# Run the example sketch
./gradlew run

# Build the project
./gradlew build

# Run tests
./gradlew test
```

### Using IDE
1. Import the project into your IDE
2. Run the `SketchLauncher` class
3. Or create your own main method that uses `SketchLauncher.runSketch()`

## Keyboard Controls

- **ESC**: Exit the sketch

## Requirements

- Java 17 or higher
- No additional dependencies (uses Java Swing)

## License

This framework is part of the LedFx project.

