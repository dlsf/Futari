package moe.das.futari.moderation.reports;

import moe.das.futari.database.DatabaseHandler;
import moe.das.futari.moderation.PunishmentType;
import moe.das.futari.utils.DurationUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ReportManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());
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

    public Report getReportByID(long reportID) throws SQLException, NoSuchElementException {
        try (var connection = databaseHandler.getConnection(); var statement = connection.createStatement()) {
            var resultSet = statement.executeQuery("SELECT * FROM Punishments WHERE id = " + reportID);
            if (!resultSet.next()) throw new NoSuchElementException();

            var punishmentType = resultSet.getString("type");
            var userID = resultSet.getLong("user_id");
            var moderatorID = resultSet.getLong("moderator_id");
            var reason = resultSet.getString("reason");
            var comment = resultSet.getString("comment");
            var duration = resultSet.getString("duration");

            var user = guild.retrieveMemberById(userID).complete().getUser();
            var moderator = guild.retrieveMemberById(moderatorID).complete().getUser();

            var report = new Report(PunishmentType.valueOf(punishmentType), user, moderator, reason, comment);
            report.setDuration(duration);

            return report;
        }
    }

    public void updateReportMessage(Message message, Report report) {
        var reportThread = message.getChannel().asThreadChannel();
        if (reportThread.isArchived()) reportThread.getManager().setArchived(false).complete();

        var embed = message.getEmbeds().get(0);
        // TODO: Fix images
        var image = embed.getImage();

        var title = buildTitle(report);
        var description = buildDescriptionString(report);

        message.editMessageEmbeds(
                new EmbedBuilder()
                        .setTitle(title)
                        .setColor(report.getReportType().getColor())
                        .setDescription(description)
                        .setTimestamp(Instant.now())
                        .build()
        ).complete();
    }

    public ReportCreationResponse createReport(Report report) {
        if (!report.getReportType().isReportable()) return new ReportCreationResponse(-1, -1);

        var user = report.getUser();
        if (!hasReportThread(user)) createReportThread(user);

        // TODO: Handle deleted threads
        var thread = guild.getThreadChannelById(reportThreads.get(user.getIdLong()));
        var title = buildTitle(report);
        var description = buildDescriptionString(report);

        var reportMessageID = thread.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle(title)
                        .setColor(report.getReportType().getColor())
                        .setDescription(description)
                        .setTimestamp(Instant.now())
                        .build()
        ).complete().getIdLong();

        if (report.getReportedMessage().messageContent() == null && report.getReportedMessage().messageAttachments().isEmpty()) {
            var reminderMessageID = thread.sendMessage(report.getModerator().getAsMention() + " Please post the evidence for this punishment").complete().getIdLong();
            return new ReportCreationResponse(reportMessageID, reminderMessageID);
        }

        return new ReportCreationResponse(reportMessageID, -1);
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

            statement.executeUpdate();
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

        var reportedMessage = report.getReportedMessage();
        if (reportedMessage != null && reportedMessage.messageContent() != null) {
            stringBuilder.append("\n**Reported Message**: \n```").append(report.getReportedMessage().messageContent()).append("```");
        }

        return stringBuilder.toString();
    }
}
