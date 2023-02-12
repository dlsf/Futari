package net.dasunterstrich.futari.moderation.modules;

import net.dasunterstrich.futari.moderation.PunishmentResponse;
import net.dasunterstrich.futari.reports.EvidenceMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public abstract class PunishmentModule {
    public abstract PunishmentResponse apply(Guild guild, Member member, Member moderator, String reason, String duration, String comments, EvidenceMessage evidenceMessage);
    public abstract PunishmentResponse revoke(Guild guild, User user, Member moderator, String reason, String comments, EvidenceMessage evidenceMessage);

    public PunishmentResponse apply(Guild guild, Member member, Member moderator, String reason, String comments, EvidenceMessage evidenceMessage) {
        return apply(guild, member, moderator, reason, null, comments, evidenceMessage);
    }
}
