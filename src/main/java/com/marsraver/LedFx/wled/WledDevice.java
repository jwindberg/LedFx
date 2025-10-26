package com.marsraver.LedFx.wled;

public class WledDevice {
    public String ip;
    public String name;
    public String version;
    public int ledCount;
    public long uptime;
    public boolean isOn;

    @Override
    public String toString() {
        String power = isOn ? "ON" : "OFF";
        return String.format("  • WLED '%s' (%s) – version %s, %d LEDs, uptime %ds, Power: %s",
                name, ip, version, ledCount, uptime, power);
    }
}

