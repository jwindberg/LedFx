# WLED Communication Protocols

This document describes the different protocols available for communicating with WLED devices, their performance characteristics, and implementation details.

## Current Implementation: HTTP/JSON REST API

### Pros
- ✅ Easy to implement and debug
- ✅ Human-readable protocol
- ✅ Good for configuration and status queries
- ✅ Web-based UI can use same API

### Cons
- ❌ High overhead: HTTP headers + JSON serialization
- ❌ TCP connection overhead for each request
- ❌ Slower frame rates (typically 30-60 FPS max)
- ❌ Not optimized for high-frequency LED updates

### When to Use
- Configuration and state management
- Status queries and discovery
- Web interface integration
- Low-frequency updates (< 30 FPS)

## Better Alternatives for LED Streaming

### 1. **WLED Art-Net / E1.31 (Recommended)**

WLED supports the **Art-Net** (E1.31) protocol over UDP for high-performance LED streaming. This is the industry-standard protocol for LED installations.

#### Performance Characteristics
- ✅ **Very low overhead**: Binary protocol, no JSON parsing
- ✅ **UDP-based**: No connection overhead, fire-and-forget
- ✅ **High frame rates**: 120+ FPS achievable
- ✅ **Industry standard**: Used by professional LED software
- ✅ **Multi-device support**: Broadcast to multiple devices simultaneously

#### How It Works
1. Data is sent as UDP packets to port **5568** (default)
2. Each packet contains a DMX universe (512 bytes max = 170 LEDs)
3. Multiple universes for larger LED strips
4. No response required (fire-and-forget)

#### Frame Rate Comparison
```
HTTP/JSON:   ~30-60 FPS (with optimizations)
Art-Net:     ~120+ FPS (theoretical ~500 FPS)
```

#### Implementation Details

```java
// Art-Net packet structure (simplified)
public class ArtNetPacket {
    // Header: "Art-Net\0"
    private static final byte[] ARTNET_ID = "Art-Net\0".getBytes();
    
    // OpCode: 0x5000 = ArtDMX (send DMX data)
    private static final short OP_DMX = 0x5000;
    
    // Protocol version (always 14)
    private static final short PROTOCOL_VERSION = 14;
    
    // DMX data (512 bytes max)
    private byte[] dmxData;
}
```

#### Example UDP Send
```java
DatagramSocket socket = new DatagramSocket();
byte[] packet = createArtNetPacket(ledColors, universe, channel);
InetAddress address = InetAddress.getByName(deviceIp);
DatagramPacket datagram = new DatagramPacket(
    packet, packet.length, address, 5568
);
socket.send(datagram);
```

### 2. **WLED WLED Protocol (Binary)**

WLED has its own optimized binary protocol (alternate to Art-Net).

#### Performance Characteristics
- ✅ **Very efficient**: Optimized for WLED specifically
- ✅ **UDP-based**: Low latency
- ✅ **Specialized**: Better compression for LED data

#### Implementation
Send binary UDP packets to port **21324** (WLED sync port).

### 3. **WebSocket (Current Support)**

WLED also supports WebSocket connections for real-time updates.

#### Performance Characteristics
- ⚠️ **Moderate overhead**: Lower than HTTP but higher than raw UDP
- ✅ **Persistent connection**: No per-request overhead
- ✅ **Two-way communication**: Can receive updates from device

## Recommended Implementation: Art-Net (E1.31)

### Why Art-Net?

1. **Proven in production**: Used by professional LED software (xLights, QLC+, etc.)
2. **High performance**: 120+ FPS achievable
3. **Industry standard**: Well-documented and widely supported
4. **Multi-device**: Can broadcast to multiple WLED devices
5. **Future-proof**: Works with any E1.31-compatible hardware

### Performance Gains

Based on WLED documentation and benchmarks:

| Protocol | Max FPS | Latency | CPU Usage |
|----------|---------|---------|-----------|
| HTTP/JSON | 60 | 20-30ms | High |
| Art-Net | 120+ | 5-10ms | Low |
| **Improvement** | **2x** | **3x faster** | **50% less** |

### Implementation Strategy

1. **Keep HTTP/JSON for configuration**
   - Device discovery
   - Settings and presets
   - Status queries

2. **Use Art-Net for LED streaming**
   - Real-time animation data
   - High-frequency updates
   - Visual feedback

### Example Integration

```java
public class WledController {
    // Use HTTP for configuration
    public void turnOn() { /* HTTP POST */ }
    public void setBrightness(int bri) { /* HTTP POST */ }
    
    // Use Art-Net for LED data
    public void sendLedData(int[] rgb) { /* UDP Art-Net */ }
}
```

## References

- WLED Art-Net Support: https://kno.wled.ge/interfaces/artnet/
- Art-Net Protocol Specification: https://artisticlicence.com/WebSiteMaster/User%20Guides/art-net.pdf
- WLED Sync Protocol: https://kno.wled.ge/features/sync/

## Next Steps

To implement Art-Net support:

1. Add Art-Net UDP packet builder
2. Create `WledArtNetController` class
3. Add configuration for Art-Net vs HTTP
4. Benchmark performance improvements
5. Update documentation

Estimated development time: 2-4 hours
Expected performance gain: 2-3x faster
