package net.dasunterstrich.futari.reports;

import net.dasunterstrich.futari.moderation.PunishmentType;
import net.dv8tion.jda.api.entities.User;

public class Report {

    private final PunishmentType punishmentType;
    private final User user;
    private final User moderator;
    private final String reason;
    private final String comments;
    private String duration;
    private ReportedMessage reportedMessage;

    public Report(PunishmentType punishmentType, User user, User moderator, String reason, String comments) {
        this.punishmentType = punishmentType;
        this.user = user;
        this.moderator = moderator;
        this.reason = reason;
        this.comments = comments;
    }

    public void setDuration(String durationString) {
        this.duration = durationString;
    }

    public void setReportedMessage(ReportedMessage reportedMessage) {
        this.reportedMessage = reportedMessage;
    }

    public PunishmentType getReportType() {
        return punishmentType;
    }

    public User getUser() {
        return user;
    }

    public User getModerator() {
        return moderator;
    }

    public String getReason() {
        return reason;
    }

    public String getComments() {
        return comments;
    }

    public String getDuration() {
        return duration;
    }

    public ReportedMessage getReportedMessage() {
        return reportedMessage;
    }
}
