package net.dasunterstrich.futari.moderation.modules;

import net.dasunterstrich.futari.moderation.CommunicationResponse;
import net.dasunterstrich.futari.moderation.Punisher;
import net.dasunterstrich.futari.moderation.PunishmentType;
import net.dasunterstrich.futari.moderation.exceptions.DirectMessagesClosedException;
import net.dasunterstrich.futari.moderation.exceptions.PunishmentFailedException;
import net.dasunterstrich.futari.moderation.reports.EvidenceMessage;
import net.dasunterstrich.futari.moderation.reports.Report;
import net.dasunterstrich.futari.utils.DurationUtils;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BanModule extends PunishmentModule{
    private final Punisher punisher;

    public BanModule(Punisher punisher) {
        this.punisher = punisher;
    }

    @Override
    public CompletableFuture<CommunicationResponse> apply(Guild guild, Member member, Member moderator, String reason, String duration, String comments, EvidenceMessage evidenceMessage) {
        return apply(guild, member, moderator, reason, duration, 1, comments, evidenceMessage);
    }

    public CompletableFuture<CommunicationResponse> apply(Guild guild, Member member, Member moderator, String reason, String duration, int deletionInterval, String comments, EvidenceMessage evidenceMessage) {
        return CompletableFuture.supplyAsync(() -> {
            if (!moderator.hasPermission(Permission.BAN_MEMBERS)) throw new IllegalStateException("Not a moderator");

            // DM user
            try {
                punisher.contactUser(
                        member.getUser(),
                        EmbedUtils.custom(
                                "Banned from " + guild.getName(),
                                "**Duration**: " + DurationUtils.toReadableDuration(duration) + "\n**Reason**: " + reason,
                                Color.RED)
                );
            } catch (DirectMessagesClosedException exception) {
                return CommunicationResponse.FAILURE;
            }

            return CommunicationResponse.SUCCESS;
        }).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) throw new PunishmentFailedException(throwable);

            var result = guild.ban(member, deletionInterval, TimeUnit.HOURS).reason(reason + "(" + duration + ")").mapToResult().complete();
            if (result.isFailure()) throw new PunishmentFailedException("Failed to ban user");

            var report = new Report(PunishmentType.BAN, member.getUser(), moderator.getUser(), reason, comments);
            report.setDuration(duration);
            report.setReportedMessage(evidenceMessage);

            punisher.reportManager.createReport(report);
            punisher.modlogManager.createModlog(report);

            // Database Update
            var success = punisher.addPunishmentToDatabase(report);
            if (!success) throw new PunishmentFailedException("Communication with database failed");

            return communicationResponse;
        });
    }

    @Override
    public CompletableFuture<CommunicationResponse> revoke(Guild guild, User user, Member moderator, String reason, String comment, EvidenceMessage evidenceMessage) {
        return CompletableFuture.supplyAsync(() -> {
            if (!moderator.hasPermission(Permission.BAN_MEMBERS)) throw new PunishmentFailedException("Not a moderator");

            var result = guild.unban(user).reason(reason).mapToResult().complete();
            if (result.isFailure()) throw new PunishmentFailedException("Could not unban user");

            var report = new Report(PunishmentType.UNBAN, user, moderator.getUser(), reason, comment);
            report.setReportedMessage(evidenceMessage);

            punisher.reportManager.createReport(report);
            punisher.modlogManager.createModlog(report);

            var success = punisher.addPunishmentToDatabase(report);
            if (!success) throw new PunishmentFailedException("Communication with database failed");

            return CommunicationResponse.NONE;
        });
    }
}
