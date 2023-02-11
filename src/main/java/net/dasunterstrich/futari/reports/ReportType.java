package net.dasunterstrich.futari.reports;

import java.awt.*;

public enum ReportType {

    BAN("Ban", Color.RED),
    MUTE("Mute", Color.GRAY),
    WARN("Warn", Color.YELLOW),
    UNBAN("Unban", Color.GREEN),
    UNMUTE("Unmute", Color.GREEN);

    private final String name;
    private final Color color;

    ReportType(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }
}
