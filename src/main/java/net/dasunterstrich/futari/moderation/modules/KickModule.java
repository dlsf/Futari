package net.dasunterstrich.futari.moderation.modules;

import net.dasunterstrich.futari.moderation.Punisher;
import net.dasunterstrich.futari.moderation.PunishmentResponse;
import net.dasunterstrich.futari.moderation.PunishmentType;
import net.dasunterstrich.futari.reports.EvidenceMessage;
import net.dasunterstrich.futari.reports.Report;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;

public class KickModule extends PunishmentModule {
    private final Punisher punisher;

    public KickModule(Punisher punisher) {
        this.punisher = punisher;
    }

    @Override
    public PunishmentResponse apply(Guild guild, Member member, Member moderator, String reason, String duration, String comments, EvidenceMessage evidenceMessage) {
        if (!moderator.hasPermission(Permission.KICK_MEMBERS)) return PunishmentResponse.failed();

        // DM user
        punisher.contactUserAndThen(
                member.getUser(),
                EmbedUtils.custom(
                        "Kicked from " + guild.getName(),
                        "**Reason**: " + reason,
                        Color.YELLOW),
                throwable -> {
                    member.kick().reason(reason).queue();

                    // Thread update
                    var report = new Report(PunishmentType.KICK, member.getUser(), moderator.getUser(), reason, comments);
                    report.setReportedMessage(evidenceMessage);
                    punisher.reportManager.createReport(member.getUser(), report);

                    // Database Update
                    var success = punisher.addPunishmentToDatabase(report);
                    System.out.println(throwable.isFailure());
                }
        );

        // TODO: Edit
        return new PunishmentResponse(true, true);
    }

    @Override
    public PunishmentResponse revoke(Guild guild, User user, Member moderator, String reason, String comments, EvidenceMessage evidenceMessage) {
        return null;
    }
}
