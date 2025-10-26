# Performance Improvements

This document describes the performance optimizations made to improve LED animation performance, making it comparable to the Processing-based Fadecandy examples.

## Problem Analysis

The original implementation had several performance bottlenecks compared to the Processing-based Fadecandy library:

1. **HTTP Overhead**: Sending JSON over HTTP for every frame is very slow
2. **Frame Rate**: Limited to 60 FPS by Swing's timer system
3. **JSON Serialization**: Creating and serializing JSON payloads on every frame
4. **No Frame Rate Limiting**: Sent updates even when network couldn't keep up
5. **Redundant Operations**: Repeated method calls and object creation in animation loops

## Optimizations Implemented

### 1. Frame Rate Limiting (WledController.java)
- Added `AtomicLong` to track last send time
- Skip frames if less than 16ms has passed (~60 FPS max network rate)
- Prevents overwhelming the network with redundant requests
- Reduces unnecessary JSON serialization

### 2. Connection Timeouts
- Reduced `connectTimeout` from default (5000ms) to 100ms
- Reduced `readTimeout` to 100ms
- Failed requests fail fast instead of blocking

### 3. Increased UI Frame Rate (AnimationSketchRunner.java)
- Increased from 60 FPS to 120 FPS for smoother animations
- UI updates more frequently while network is limited to ~60 FPS

### 4. Cached Values (BouncingBallAnimation.java)
- Cache `gridSize`, `pixelSize`, and `gridCount` to avoid repeated method calls
- Pre-compute glow color once per frame instead of creating new `Color` objects in loops
- Reduced object allocations in hot loops

### 5. Optimized HTTP Requests
- Pre-compute URL string (`baseUrl`)
- Serialize JSON once and store in byte array
- Reuse connection patterns

## Results

These optimizations should provide:
- **Smoother animations**: 120 FPS UI updates with 60 FPS network limit
- **Lower latency**: Failed requests fail fast instead of blocking
- **Better CPU efficiency**: Reduced object allocations and method calls
- **Stable frame rate**: Network won't be overwhelmed with redundant requests

## Comparison with Processing/Fadecandy

The Processing-based Fadecandy examples were faster because:
- Processing runs at native speed with highly optimized graphics
- Fadecandy uses binary Open Pixel Control (OPC) protocol over TCP, not HTTP JSON
- Processing uses efficient buffer operations

Our optimizations bring Java/Swing closer to Processing performance while maintaining the same HTTP-based architecture.

## Further Optimizations (Future Work)

For even better performance, consider:
1. **Art-Net Protocol**: Implement E1.31/Art-Net UDP protocol for 2-3x performance gain (see `WLED_PROTOCOLS.md`)
   - Switches from HTTP/JSON to binary UDP
   - Achieves 120+ FPS (vs 60 FPS with HTTP)
   - 3x lower latency (5-10ms vs 20-30ms)
   - Industry-standard protocol used by professional LED software
2. **Multi-threading**: Send to multiple devices in parallel
3. **Connection Pooling**: Reuse HTTP connections across frames

### Art-Net Implementation (Recommended Next Step)

See `WLED_PROTOCOLS.md` for detailed information about implementing Art-Net support. This would provide the biggest performance improvement:
- **2x faster frame rates** (120+ FPS achievable)
- **3x lower latency** (5-10ms vs 20-30ms)
- **50% less CPU usage**
- **Industry standard** protocol used by xLights, QLC+, and other professional LED software

The implementation would keep HTTP/JSON for device configuration and status, while using Art-Net UDP for high-performance LED streaming.
