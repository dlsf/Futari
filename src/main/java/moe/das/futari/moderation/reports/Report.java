package moe.das.futari.moderation.reports;

import moe.das.futari.moderation.PunishmentType;
import net.dv8tion.jda.api.entities.User;

public class Report {

    private final PunishmentType punishmentType;
    private final User user;
    private final User moderator;
    private final String comments;
    private String reason;
    private String duration;
    private EvidenceMessage evidenceMessage;

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

    public void setReportedMessage(EvidenceMessage evidenceMessage) {
        this.evidenceMessage = evidenceMessage;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public EvidenceMessage getReportedMessage() {
        return evidenceMessage;
    }
}
