package net.dasunterstrich.futari.moderation;

import net.dasunterstrich.futari.database.DatabaseHandler;
import net.dasunterstrich.futari.reports.Report;
import net.dasunterstrich.futari.reports.ReportManager;
import net.dasunterstrich.futari.reports.ReportedMessage;
import net.dasunterstrich.futari.utils.DurationUtils;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Punisher {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final DatabaseHandler databaseHandler;
    private final ReportManager reportManager;

    public Punisher(DatabaseHandler databaseHandler, ReportManager reportManager) {
        this.databaseHandler = databaseHandler;
        this.reportManager = reportManager;
    }

    public PunishmentResponse ban(Guild guild, Member member, Member moderator, String reason, String duration, String comments, ReportedMessage reportedMessage) {
        if (!moderator.hasPermission(Permission.BAN_MEMBERS)) return PunishmentResponse.failed();

        // DM user
        contactUserAndThen(
                member.getUser(),
                EmbedUtils.custom(
                        "Banned from " + guild.getName(),
                        "**Duration**: " + DurationUtils.toReadableDuration(duration) + "\n**Reason**: " + reason,
                        Color.RED),
                throwable -> {}
        );

        // Ban user
        try {
            guild.ban(member, 3, TimeUnit.HOURS).reason(reason + "(" + duration + ")").queue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return PunishmentResponse.failed();
        }

        // Thread update
        var report = new Report(PunishmentType.BAN, member.getUser(), moderator.getUser(), reason, comments);
        report.setDuration(duration);
        report.setReportedMessage(reportedMessage);
        reportManager.createReport(member.getUser(), report);

        // Database Update
        var success = addReportToDatabase(report);
        return new PunishmentResponse(success, true);
    }

    public PunishmentResponse unban(Guild guild, User user, Member moderator, String reason, String comment, ReportedMessage reportedMessage) {
        if (!moderator.hasPermission(Permission.BAN_MEMBERS)) return PunishmentResponse.failed();

        guild.unban(user).reason(reason).queue();

        var report = new Report(PunishmentType.UNBAN, user, moderator.getUser(), reason, comment);
        report.setReportedMessage(reportedMessage);
        reportManager.createReport(user, report);

        var success = addReportToDatabase(report);
        return new PunishmentResponse(success, false);
    }

    public PunishmentResponse mute(Guild guild, Member member, Member moderator, String reason, String duration, String comments, ReportedMessage reportedMessage) {
        if (!moderator.hasPermission(Permission.BAN_MEMBERS)) return PunishmentResponse.failed();

        // DM user
        contactUserAndThen(
                member.getUser(),
                EmbedUtils.custom(
                        "Muted from " + guild.getName(),
                        "**Duration**: " + DurationUtils.toReadableDuration(duration) + "\n**Reason**: " + reason,
                        Color.GRAY),
                throwable -> {}
        );

        guild.addRoleToMember(member, guild.getRoleById(1073208950280953906L)).reason(reason).queue();

        // Thread update
        var report = new Report(PunishmentType.MUTE, member.getUser(), moderator.getUser(), reason, comments);
        report.setDuration(duration);
        report.setReportedMessage(reportedMessage);
        reportManager.createReport(member.getUser(), report);

        // Database Update
        var success = addReportToDatabase(report);
        return new PunishmentResponse(success, true);
    }

    public PunishmentResponse unmute(Guild guild, Member member, Member moderator, String reason, String comment, ReportedMessage reportedMessage) {
        if (!moderator.hasPermission(Permission.BAN_MEMBERS)) return PunishmentResponse.failed();

        // TODO: Null / guild check?
        guild.removeRoleFromMember(member, guild.getRoleById(1073208950280953906L)).reason(reason).queue();

        // DM user
        contactUserAndThen(
                member.getUser(),
                EmbedUtils.custom(
                        "You got unmuted from " + guild.getName(),
                        "**Reason**: " + reason,
                        Color.GREEN),
                throwable -> {}
        );

        var report = new Report(PunishmentType.UNMUTE, member.getUser(), moderator.getUser(), reason, comment);
        report.setReportedMessage(reportedMessage);
        reportManager.createReport(member.getUser(), report);

        // Database update
        var success = addReportToDatabase(report);
        return new PunishmentResponse(success, true);
    }

    public PunishmentResponse warn(Guild guild, Member member, Member moderator, String reason, String comments, ReportedMessage reportedMessage) {
        if (!moderator.hasPermission(Permission.BAN_MEMBERS)) return PunishmentResponse.failed();

        // DM user
        contactUserAndThen(
                member.getUser(),
                EmbedUtils.custom(
                        "Warned from " + guild.getName(),
                        "**Reason**: " + reason,
                        Color.YELLOW),
                throwable -> {}
        );

        // Thread update
        var report = new Report(PunishmentType.WARN, member.getUser(), moderator.getUser(), reason, comments);
        report.setReportedMessage(reportedMessage);
        reportManager.createReport(member.getUser(), report);

        // Database Update
        var success = addReportToDatabase(report);
        return new PunishmentResponse(success, true);
    }

    public PunishmentResponse unwarn(Guild guild, int punishmentID, Member member, Member moderator, String reason) {
        if (!moderator.hasPermission(Permission.BAN_MEMBERS)) return PunishmentResponse.failed();

        // DM user
        contactUserAndThen(
                member.getUser(),
                EmbedUtils.custom(
                        "Your warn from " + guild.getName() + " got revoked",
                        "**Reason**: " + reason,
                        Color.GREEN),
                throwable -> {}
        );

        // Remove from database
        try (var connection = databaseHandler.getConnection(); var statement = connection.createStatement()) {
            statement.execute("DELETE FROM Punishments WHERE id = " + punishmentID);
        } catch (Exception exception) {
            logger.error("Could not revoke warn", exception);
            return new PunishmentResponse(false, true);
        }

        return new PunishmentResponse(true, true);
    }

    public PunishmentResponse kick(Guild guild, Member member, Member moderator, String reason, String comments, ReportedMessage reportedMessage) {
        if (!moderator.hasPermission(Permission.KICK_MEMBERS)) return PunishmentResponse.failed();

        // DM user
        contactUserAndThen(
                member.getUser(),
                EmbedUtils.custom(
                        "Kicked from " + guild.getName(),
                        "**Reason**: " + reason,
                        Color.YELLOW),
                throwable -> {
                    member.kick().reason(reason).queue();

                    // Thread update
                    var report = new Report(PunishmentType.KICK, member.getUser(), moderator.getUser(), reason, comments);
                    report.setReportedMessage(reportedMessage);
                    reportManager.createReport(member.getUser(), report);

                    // Database Update
                    var success = addReportToDatabase(report);
                    System.out.println(throwable.isFailure());
                }
        );

        // TODO: Edit
        return new PunishmentResponse(true, true);
    }

    private void contactUserAndThen(User user, MessageEmbed embed, Consumer<? super Result<Message>> action) {
        user.openPrivateChannel()
                .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(embed))
                .mapToResult()
                .queue(action);

    }

    private boolean addReportToDatabase(Report report) {
        try(var connection = databaseHandler.getConnection()) {
            var statement = connection.prepareStatement("INSERT INTO Punishments (user_id, moderator_id, type, reason, comment, duration) VALUES (?, ?, ?, ?, ?, ?)");
            statement.setLong(1, report.getUser().getIdLong());
            statement.setLong(2, report.getModerator().getIdLong());
            statement.setString(3, report.getReportType().name());
            statement.setString(4, report.getReason());
            statement.setString(5, report.getComments());

            if (report.getDuration() == null) {
                statement.setString(6, "");
            } else {
                statement.setString(6, report.getDuration());
            }

            statement.execute();
            statement.close();

            if (report.getDuration() != null && !report.getDuration().isEmpty()) {
                return addTemporaryPunishment(connection, report, statement.getGeneratedKeys().getInt(1));
            }
        } catch (Exception exception) {
            logger.error("Could not add report to database", exception);
            return false;
        }

        return true;
    }

    private boolean addTemporaryPunishment(Connection connection, Report report, int punishmentID) throws SQLException {
        var statement = connection.prepareStatement("INSERT INTO TemporaryPunishments (report_id, timestamp) VALUES (?, ?)");
        statement.setInt(1, punishmentID);
        statement.setLong(2, System.currentTimeMillis() + DurationUtils.durationStringToMillis(report.getDuration()));

        statement.execute();
        statement.close();
        return true;
    }
}
