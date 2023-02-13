package net.dasunterstrich.futari.reports;

import net.dasunterstrich.futari.database.DatabaseHandler;
import net.dasunterstrich.futari.utils.DurationUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ReportManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ExecutorService executorService = Executors.newScheduledThreadPool(3);
    private final DatabaseHandler databaseHandler;
    private final Map<Long, Long> reportThreads = new ConcurrentHashMap<>();
    private Guild guild;

    public ReportManager(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;

        try (var connection = databaseHandler.getConnection(); var statement = connection.createStatement()) {
            var resultSet = statement.executeQuery("SELECT * FROM ReportThreads");
            while (resultSet.next()) {
                var userId = resultSet.getLong("user_id");
                var threadId = resultSet.getLong("thread_id");
                reportThreads.put(userId, threadId);
            }
        } catch (Exception exception) {
            logger.error("Failed to load report threads", exception);
        }

        logger.info("Loaded report threads successfully");
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    public void createReport(User user, Report report) {
        if (!report.getReportType().isReportable()) return;

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

            // TODO: Reminders
            if (report.getReportedMessage().messageContent() == null && report.getReportedMessage().messageAttachments().isEmpty()) {
                thread.sendMessage(report.getModerator().getAsMention() + " Please post the evidence for this punishment").queue();
            }
        });
    }

    public boolean hasReportThread(User user) {
        return reportThreads.containsKey(user.getIdLong());
    }

    public long getReportThreadID(User user) {
        return reportThreads.get(user.getIdLong());
    }

    private void createReportThread(User user) {
        var forumPost = guild.getForumChannelById(1073212604090167357L).createForumPost(user.getAsTag() + " (" + user.getIdLong() + ")", MessageCreateData.fromContent("New Report"))
                .timeout(2, TimeUnit.SECONDS)
                .complete();

        var userId = user.getIdLong();
        var threadId = forumPost.getThreadChannel().getIdLong();

        reportThreads.put(userId, threadId);
        addThreadToDatabase(userId, threadId);
    }

    private void addThreadToDatabase(long userId, long threadId) {
        try (var connection = databaseHandler.getConnection()) {
            var statement = connection.prepareStatement("INSERT INTO ReportThreads (user_id, thread_id) VALUES (?, ?)");

            statement.setLong(1, userId);
            statement.setLong(2, threadId);

            statement.execute();
            statement.close();
        } catch (Exception exception) {
            logger.error("Error while inserting thread data", exception);
        }
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

        if (report.getReportedMessage().messageContent() != null) {
            stringBuilder.append("\n**Reported Message**: \n```").append(report.getReportedMessage().messageContent()).append("```");
        }

        return stringBuilder.toString();
    }

}
