package net.dasunterstrich.futari.moderation.modlog;

import net.dasunterstrich.futari.moderation.reports.Report;
import net.dasunterstrich.futari.utils.DurationUtils;
import net.dasunterstrich.futari.utils.EmbedUtils;

public class ModlogManager {
    public void createModlog(Report report) {
        var jda = report.getUser().getJDA();
        var guild = jda.getGuildById(497092213034188806L);
        var modlogChannel = guild.getTextChannelById(1073722655628341429L);

        var title = buildTitle(report);
        var description = buildDescriptionString(report);
        var color =  report.getReportType().getColor();

        modlogChannel.sendMessageEmbeds(EmbedUtils.custom(title, description, color)).queue();
    }

    private String buildTitle(Report report) {
        var stringBuilder = new StringBuilder();

        stringBuilder.append(report.getReportType().getName());

        if (report.getDuration() != null) {
            stringBuilder.append(" (");
            stringBuilder.append(DurationUtils.toReadableDuration(report.getDuration()));
            stringBuilder.append(")");
        }

        stringBuilder.append(" | ");
        stringBuilder.append(report.getUser().getAsTag());

        return stringBuilder.toString();
    }

    private String buildDescriptionString(Report report) {
        return  "**Reason**: " + report.getReason() +
                "\n**User**: " + report.getUser().getAsMention() +
                "\n**Moderator**: " + report.getModerator().getAsMention();
    }
}
