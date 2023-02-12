package net.dasunterstrich.futari.moderation.modules;

import net.dasunterstrich.futari.moderation.Punisher;
import net.dasunterstrich.futari.moderation.PunishmentResponse;
import net.dasunterstrich.futari.moderation.PunishmentType;
import net.dasunterstrich.futari.reports.EvidenceMessage;
import net.dasunterstrich.futari.reports.Report;
import net.dasunterstrich.futari.utils.DurationUtils;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.util.concurrent.TimeUnit;

public class BanModule extends PunishmentModule{
    private final Punisher punisher;

    public BanModule(Punisher punisher) {
        this.punisher = punisher;
    }

    @Override
    public PunishmentResponse apply(Guild guild, Member member, Member moderator, String reason, String duration, String comments, EvidenceMessage evidenceMessage) {
        if (!moderator.hasPermission(Permission.BAN_MEMBERS)) return PunishmentResponse.failed();

        // DM user
        punisher.contactUserAndThen(
                member.getUser(),
                EmbedUtils.custom(
                        "Banned from " + guild.getName(),
                        "**Duration**: " + DurationUtils.toReadableDuration(duration) + "\n**Reason**: " + reason,
                        Color.RED),
                throwable -> {}
        );

        // Ban user
        try {
            guild.ban(member, 3, TimeUnit.HOURS).reason(reason + "(" + duration + ")").queue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return PunishmentResponse.failed();
        }

        // Thread update
        var report = new Report(PunishmentType.BAN, member.getUser(), moderator.getUser(), reason, comments);
        report.setDuration(duration);
        report.setReportedMessage(evidenceMessage);
        punisher.reportManager.createReport(member.getUser(), report);

        // Database Update
        var success = punisher.addPunishmentToDatabase(report);
        return new PunishmentResponse(success, true);
    }

    @Override
    public PunishmentResponse revoke(Guild guild, User user, Member moderator, String reason, String comment, EvidenceMessage evidenceMessage) {
        if (!moderator.hasPermission(Permission.BAN_MEMBERS)) return PunishmentResponse.failed();

        guild.unban(user).reason(reason).queue();

        var report = new Report(PunishmentType.UNBAN, user, moderator.getUser(), reason, comment);
        report.setReportedMessage(evidenceMessage);
        punisher.reportManager.createReport(user, report);

        var success = punisher.addPunishmentToDatabase(report);
        return new PunishmentResponse(success, false);
    }
}
