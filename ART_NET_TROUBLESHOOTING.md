# Art-Net Troubleshooting Guide

## Issue: Grid01 Not Receiving Art-Net Data

### Symptoms
- Grid02 works correctly with animations
- Grid01 (Universe 0) receives packets (confirmed by debug logs) but LEDs don't update
- Test patterns work on both grids (confirmed via `testArtNet`)

### Confirmed Facts
1. ✅ Art-Net packets are being sent to Grid01 (192.168.7.113) on Universe 0
2. ✅ Network connectivity is working (test patterns work)
3. ✅ Grid configuration is correct in `TwoGrids.xml`
4. ✅ `WledArtNetController` is initialized correctly for both grids

### Most Likely Cause
**WLED Art-Net Configuration**: Grid01's WLED device (192.168.7.113) may not be configured to receive Art-Net on Universe 0.

### Solution

#### Step 1: Check WLED Art-Net Settings
1. Open WLED web interface for Grid01: http://192.168.7.113
2. Go to **Config → Sync Settings**
3. Enable **E1.31 (Art-Net)**
4. Set **Port** to `5568` (default)
5. Set **Universe** to `0` (matching the code)
6. Enable **Accept broadcasts**
7. Click **Save**

#### Step 2: Verify Grid02 Settings
For comparison, check Grid02 (192.168.7.226):
- Universe should be `1`
- Port should be `5568`
- E1.31 should be **Enabled**

#### Step 3: Alternative Check
If the WLED interface doesn't show Universe settings:
1. Check if there's a separate segment configuration
2. Ensure the device is running a version that supports Art-Net
3. Try disabling and re-enabling E1.31

### Additional Debugging

#### Check Network Traffic
If you have network monitoring tools (Wireshark):
```bash
# Monitor UDP traffic on port 5568
tcpdump -i any -n udp port 5568
```

#### Check WLED Status
Verify the WLED device is responding:
```bash
curl http://192.168.7.113/json/state
```

#### Test Direct Art-Net
Run the Art-Net test to verify Grid01 responds:
```bash
./gradlew testArtNet
```

This test should show:
- Grid01 receiving red, green, blue, and rainbow patterns
- All tests passing

### Configuration Differences

| Setting | Grid01 | Grid02 | Notes |
|---------|--------|--------|-------|
| IP Address | 192.168.7.113 | 192.168.7.226 | Different |
| Art-Net Universe | 0 | 1 | Different |
| Port | 5568 | 5568 | Same |
| LED Count | 256 | 256 | Same |
| Grid Size | 16x16 | 16x16 | Same |

### Why This Issue Occurs

Art-Net requires **explicit configuration on the receiving device**. Each WLED device needs to:
1. Know which Art-Net universe to listen to
2. Have E1.31 enabled
3. Accept UDP packets on port 5568

Unlike HTTP/JSON which WLED always accepts, Art-Net must be explicitly enabled for performance and security reasons.

### Next Steps

1. **If Grid01 starts working**: Art-Net configuration was the issue. ✅
2. **If Grid01 still doesn't work**: 
   - Check WLED firmware version supports Art-Net
   - Try restarting Grid01's WLED device
   - Check for firewall issues
   - Verify network connectivity to Grid01

### References

- WLED Art-Net Documentation: https://kno.wled.ge/interfaces/artnet/
- Art-Net Protocol Specification: https://artisticlicence.com/WebSiteMaster/User%20Guides/art-net.pdf
- See `ART_NET_MIGRATION.md` for migration details
