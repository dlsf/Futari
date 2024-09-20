package moe.das.futari.moderation.modules;

import moe.das.futari.moderation.CommunicationResponse;
import moe.das.futari.moderation.Punisher;
import moe.das.futari.moderation.PunishmentType;
import moe.das.futari.moderation.exceptions.DirectMessagesClosedException;
import moe.das.futari.moderation.exceptions.PunishmentFailedException;
import moe.das.futari.moderation.reports.EvidenceMessage;
import moe.das.futari.moderation.reports.Report;
import moe.das.futari.utils.DurationUtils;
import moe.das.futari.utils.EmbedUtils;
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

            var reportCreationResponse = punisher.reportManager.createReport(report);
            var modlogMessageID = punisher.modlogManager.createModlog(report);

            // Database Update
            var success = punisher.addPunishmentToDatabase(report, reportCreationResponse, modlogMessageID);
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

            var reportCreationResponse = punisher.reportManager.createReport(report);
            var modlogMessageID = punisher.modlogManager.createModlog(report);

            // Database Update
            var success = punisher.addPunishmentToDatabase(report, reportCreationResponse, modlogMessageID);
            if (!success) throw new PunishmentFailedException("Communication with database failed");

            return CommunicationResponse.NONE;
        });
    }
}
