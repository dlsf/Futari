package net.dasunterstrich.futari.moderation;

import java.awt.*;

public enum PunishmentType {

    BAN("Ban", Color.RED, true),
    MUTE("Mute", Color.GRAY, true),
    WARN("Warn", Color.YELLOW, true),
    UNBAN("Unban", Color.GREEN, false),
    UNMUTE("Unmute", Color.GREEN, false);

    private final String name;
    private final Color color;
    private final boolean reportable;

    PunishmentType(String name, Color color, boolean reportable) {
        this.name = name;
        this.color = color;
        this.reportable = reportable;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public boolean isReportable() {
        return reportable;
    }
}
