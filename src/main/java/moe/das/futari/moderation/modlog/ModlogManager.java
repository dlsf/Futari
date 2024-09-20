package moe.das.futari.moderation.modlog;

import moe.das.futari.moderation.reports.Report;
import moe.das.futari.utils.DurationUtils;
import moe.das.futari.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Message;

public class ModlogManager {
    public long createModlog(Report report) {
        var jda = report.getUser().getJDA();
        var guild = jda.getGuildById(497092213034188806L);
        var modlogChannel = guild.getTextChannelById(1073722655628341429L);

        var title = buildTitle(report);
        var description = buildDescriptionString(report);
        var color =  report.getReportType().getColor();

        return modlogChannel.sendMessageEmbeds(EmbedUtils.customWithTimestamp(title, description, color)).complete().getIdLong();
    }

    public void updateModlogMessage(Message message, Report report) {
        var title = buildTitle(report);
        var description = buildDescriptionString(report);
        var color =  report.getReportType().getColor();

        message.editMessageEmbeds(EmbedUtils.customWithTimestamp(title, description, color)).complete();
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
