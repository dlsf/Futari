package net.dasunterstrich.moderation;

import net.dasunterstrich.utils.DurationUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportManager {

    private final ExecutorService executorService = Executors.newScheduledThreadPool(3);
    private final Map<Long, Long> reportThreads = new ConcurrentHashMap<>();
    private Guild guild;

    public ReportManager() {
        // TODO: Load from database
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    public void createReport(User user, Report report) {
        executorService.execute(() -> {
            if (!hasReportThread(user)) createReportThread(user);

            var thread = guild.getThreadChannelById(reportThreads.get(user.getIdLong()));
            var title = buildTitle(report);
            var description = buildDescriptionString(report);

            thread.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setTitle(title)
                            .setColor(report.getReportType().getColor())
                            .setDescription(description)
                            .setTimestamp(Instant.now())
                            .build()
            ).queue();

            // TODO: Database stuff, reminders
            //thread.sendMessage("Report Type: " + report.getReportType().getName() + "\nReason: " + report.getReason() + "\nComments: " + report.getComments()).queue();
        });
    }

    private boolean hasReportThread(User user) {
        return reportThreads.containsKey(user.getIdLong());
    }

    private void createReportThread(User user) {
        // TODO: Replace complete if possible or add timeout
        guild.getForumChannelById(1073212604090167357L).createForumPost(user.getAsTag() + " (" + user.getIdLong() + ")", MessageCreateData.fromContent("New Report"))
                .queue(forumPost -> {
                    reportThreads.put(user.getIdLong(), forumPost.getThreadChannel().getIdLong());

                    // TODO: Add to database
                });
    }

    private String buildTitle(Report report) {
        var stringBuilder = new StringBuilder();

        stringBuilder.append(report.getReportType().getName());

        if (report.getDuration() != null) {
            stringBuilder.append(" (");

            stringBuilder.append(DurationUtils.toReadableDuration(report.getDuration()));

            stringBuilder.append(")");
        }

        return stringBuilder.toString();
    }

    private String buildDescriptionString(Report report) {
        var stringBuilder = new StringBuilder();

        stringBuilder.append("**Reason**: ").append(report.getReason());
        stringBuilder.append("\n**Moderator**: ").append(report.getModerator().getAsMention());

        if (!report.getComments().isEmpty()) {
            stringBuilder.append("\n**Comments**: ").append(report.getComments());
        }

        return stringBuilder.toString();
    }

}
