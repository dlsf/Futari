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

public class MuteModule extends PunishmentModule {
    private final Punisher punisher;

    public MuteModule(Punisher punisher) {
        this.punisher = punisher;
    }

    @Override
    public CompletableFuture<CommunicationResponse> apply(Guild guild, Member member, Member moderator, String reason, String duration, String comments, EvidenceMessage evidenceMessage) {
        return CompletableFuture.supplyAsync(() -> {
            if (!moderator.hasPermission(Permission.BAN_MEMBERS)) throw new IllegalStateException("Not a moderator");

            // DM user
            try {
                punisher.contactUser(
                        member.getUser(),
                        EmbedUtils.custom(
                                "Muted from " + guild.getName(),
                                "**Duration**: " + DurationUtils.toReadableDuration(duration) + "\n**Reason**: " + reason,
                                Color.GRAY)
                );
            } catch (DirectMessagesClosedException exception) {
                return CommunicationResponse.FAILURE;
            }

            return CommunicationResponse.SUCCESS;
        }).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) throw new PunishmentFailedException(throwable);

            var result = guild.addRoleToMember(member, guild.getRoleById(1073208950280953906L)).reason(reason).mapToResult().complete();
            if (result.isFailure()) throw new PunishmentFailedException("Could not assign role");

            var report = new Report(PunishmentType.MUTE, member.getUser(), moderator.getUser(), reason, comments);
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
    public CompletableFuture<CommunicationResponse> revoke(Guild guild, User user, Member moderator, String reason, String comments, EvidenceMessage evidenceMessage) {
        return CompletableFuture.supplyAsync(() -> {
            if (!moderator.hasPermission(Permission.BAN_MEMBERS)) throw new IllegalStateException("Not a moderator");

            // DM user
            try {
                punisher.contactUser(
                        user,
                        EmbedUtils.custom(
                                "You got unmuted from " + guild.getName(),
                                "**Reason**: " + reason,
                                Color.GREEN)
                );
            } catch (DirectMessagesClosedException exception) {
                return CommunicationResponse.FAILURE;
            }

            return CommunicationResponse.SUCCESS;
        }).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) throw new PunishmentFailedException(throwable);

            var member = guild.retrieveMember(user).complete();
            var result = guild.removeRoleFromMember(member, guild.getRoleById(1073208950280953906L)).reason(reason).mapToResult().complete();
            if (result.isFailure()) throw new PunishmentFailedException("Could not remove mute role");

            var report = new Report(PunishmentType.UNMUTE, user, moderator.getUser(), reason, comments);
            report.setReportedMessage(evidenceMessage);

            punisher.reportManager.createReport(report);
            punisher.modlogManager.createModlog(report);

            // Database update
            var success = punisher.addPunishmentToDatabase(report);
            if (!success) throw new PunishmentFailedException("Communication with database failed");

            return communicationResponse;
        });
    }
}
