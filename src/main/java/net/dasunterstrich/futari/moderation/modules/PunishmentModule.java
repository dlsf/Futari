package net.dasunterstrich.futari.moderation.modules;

import net.dasunterstrich.futari.moderation.CommunicationResponse;
import net.dasunterstrich.futari.moderation.reports.EvidenceMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.util.concurrent.CompletableFuture;

public abstract class PunishmentModule {
    public abstract CompletableFuture<CommunicationResponse> apply(Guild guild, Member member, Member moderator, String reason, String duration, String comments, EvidenceMessage evidenceMessage);
    public abstract CompletableFuture<CommunicationResponse> revoke(Guild guild, User user, Member moderator, String reason, String comments, EvidenceMessage evidenceMessage);

    public CompletableFuture<CommunicationResponse> apply(Guild guild, Member member, Member moderator, String reason, String comments, EvidenceMessage evidenceMessage) {
        return apply(guild, member, moderator, reason, null, comments, evidenceMessage);
    }
}
