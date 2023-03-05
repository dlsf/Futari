package net.dasunterstrich.futari.moderation.modules;

import net.dasunterstrich.futari.moderation.CommunicationResponse;
import net.dasunterstrich.futari.moderation.Punisher;
import net.dasunterstrich.futari.moderation.PunishmentType;
import net.dasunterstrich.futari.moderation.exceptions.DirectMessagesClosedException;
import net.dasunterstrich.futari.moderation.exceptions.PunishmentFailedException;
import net.dasunterstrich.futari.moderation.reports.EvidenceMessage;
import net.dasunterstrich.futari.moderation.reports.Report;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class WarnModule extends PunishmentModule {
    private final Punisher punisher;

    public WarnModule(Punisher punisher) {
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
                                "Warned from " + guild.getName(),
                                "**Reason**: " + reason,
                                Color.YELLOW)
                );
            } catch (DirectMessagesClosedException exception) {
                return CommunicationResponse.FAILURE;
            }

            return CommunicationResponse.SUCCESS;
        }).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) throw new PunishmentFailedException(throwable);

            var report = new Report(PunishmentType.WARN, member.getUser(), moderator.getUser(), reason, comments);
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
    public CompletableFuture<CommunicationResponse> revoke(Guild guild, User user, Member moderator, String reason, String comments, EvidenceMessage evidenceMessage) {
        // Implemented directly in Punisher#unwarn() because of issues with the abstraction I can't be bothered to fix
        throw new IllegalStateException("Method not implemented");
    }
}
