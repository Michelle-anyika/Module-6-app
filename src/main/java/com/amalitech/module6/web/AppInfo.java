package com.amalitech.module6.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

/** Identifies the running task and image version - handy to watch blue/green traffic shift. */
@Component
public class AppInfo {

    private final String version;
    private final String host;

    public AppInfo(@Value("${app.version:local}") String version) {
        this.version = version;
        String h;
        try {
            h = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            h = "unknown";
        }
        this.host = h;
    }

    public String getVersion() { return version; }
    public String getShortVersion() { return version.length() > 7 ? version.substring(0, 7) : version; }
    public String getHost() { return host; }
}
