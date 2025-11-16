package com.marsraver.LedFx.wled;

/**
 * Basic information about a WLED device used by the DDP client.
 * This is intentionally minimal – only fields actually needed by {@link WledDdpClient}.
 */
public class WledInfo {

    private String ip;
    private String name;

    public WledInfo(String ip) {
        this(ip, null);
    }

    public WledInfo(String ip, String name) {
        this.ip = ip;
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}


