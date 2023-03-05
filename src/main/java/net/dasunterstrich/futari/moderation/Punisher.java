package net.dasunterstrich.futari.moderation;

import net.dasunterstrich.futari.database.DatabaseHandler;
import net.dasunterstrich.futari.moderation.exceptions.DirectMessagesClosedException;
import net.dasunterstrich.futari.moderation.exceptions.PunishmentFailedException;
import net.dasunterstrich.futari.moderation.modlog.ModlogManager;
import net.dasunterstrich.futari.moderation.modules.*;
import net.dasunterstrich.futari.moderation.reports.EvidenceMessage;
import net.dasunterstrich.futari.moderation.reports.Report;
import net.dasunterstrich.futari.moderation.reports.ReportCreationResponse;
import net.dasunterstrich.futari.moderation.reports.ReportManager;
import net.dasunterstrich.futari.utils.DurationUtils;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Punisher {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final DatabaseHandler databaseHandler;
    public final ReportManager reportManager;
    public final ModlogManager modlogManager;

    private final BanModule banModule;
    private final PunishmentModule muteModule;
    private final PunishmentModule warnModule;
    private final PunishmentModule kickModule;

    public Punisher(DatabaseHandler databaseHandler, ReportManager reportManager, ModlogManager modlogManager) {
        this.databaseHandler = databaseHandler;
        this.reportManager = reportManager;
        this.modlogManager = modlogManager;

        this.banModule = new BanModule(this);
        this.muteModule = new MuteModule(this);
        this.warnModule = new WarnModule(this);
        this.kickModule = new KickModule(this);
    }

    public CompletableFuture<CommunicationResponse> ban(Guild guild, Member member, Member moderator, String reason, String duration, int deletionInterval, String comments, EvidenceMessage evidenceMessage) {
        return banModule.apply(guild, member, moderator, reason, duration, deletionInterval, comments, evidenceMessage);
    }

    public CompletableFuture<CommunicationResponse> unban(Guild guild, User user, Member moderator, String reason, String comment, EvidenceMessage evidenceMessage) {
        return banModule.revoke(guild, user, moderator, reason, comment, evidenceMessage);
    }

    public CompletableFuture<CommunicationResponse> mute(Guild guild, Member member, Member moderator, String reason, String duration, String comments, EvidenceMessage evidenceMessage) {
        return muteModule.apply(guild, member, moderator, reason, duration, comments, evidenceMessage);
    }

    public CompletableFuture<CommunicationResponse> unmute(Guild guild, Member member, Member moderator, String reason, String comment, EvidenceMessage evidenceMessage) {
        return muteModule.revoke(guild, member.getUser(), moderator, reason, comment, evidenceMessage);
    }

    public CompletableFuture<CommunicationResponse> warn(Guild guild, Member member, Member moderator, String reason, String comments, EvidenceMessage evidenceMessage) {
        return warnModule.apply(guild, member, moderator, reason, comments, evidenceMessage);
    }

    public CompletableFuture<CommunicationResponse> unwarn(Guild guild, int punishmentID, Member member, Member moderator, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            if (!moderator.hasPermission(Permission.BAN_MEMBERS)) throw new IllegalStateException("Not a moderator");

            // DM user
            try {
                contactUser(
                        member.getUser(),
                        EmbedUtils.custom(
                                "Your warn from " + guild.getName() + " got revoked",
                                "**Reason**: " + reason,
                                Color.GREEN)
                );
            } catch (DirectMessagesClosedException exception) {
                return CommunicationResponse.FAILURE;
            }

            return CommunicationResponse.SUCCESS;
        }).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) throw new PunishmentFailedException(throwable);

            // Remove from database
            try (var connection = databaseHandler.getConnection(); var statement = connection.createStatement()) {
                statement.execute("DELETE FROM Punishments WHERE id = " + punishmentID);
            } catch (Exception exception) {
                logger.error("Could not revoke warn", exception);
                throw new PunishmentFailedException("Communication with database failed");
            }

            var report = new Report(PunishmentType.UNWARN, member.getUser(), moderator.getUser(), reason, "");
            modlogManager.createModlog(report);

            return communicationResponse;
        });
    }

    public CompletableFuture<CommunicationResponse> kick(Guild guild, Member member, Member moderator, String reason, String comments, EvidenceMessage evidenceMessage) {
        return kickModule.apply(guild, member, moderator, reason, comments, evidenceMessage);
    }

    public void contactUser(User user, MessageEmbed embed) throws DirectMessagesClosedException {
        var result = user.openPrivateChannel()
                .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(embed))
                .mapToResult()
                .complete();

        if (result.isFailure()) {
            throw new DirectMessagesClosedException();
        }
    }

    public boolean addPunishmentToDatabase(Report report, ReportCreationResponse reportCreationResponse, long modlogMessageID) {
        if (report.getReason().equals("Auto")) return true;

        try (var connection = databaseHandler.getConnection()) {
            var statement = connection.prepareStatement("INSERT INTO Punishments (user_id, moderator_id, type, reason, comment, timestamp, duration, report_message_id, modlog_message_id, reminder_message_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            statement.setLong(1, report.getUser().getIdLong());
            statement.setLong(2, report.getModerator().getIdLong());
            statement.setString(3, report.getReportType().name());
            statement.setString(4, report.getReason());
            statement.setString(5, report.getComments());
            statement.setLong(6, Instant.now().getEpochSecond());

            if (report.getDuration() == null) {
                statement.setString(7, "");
            } else {
                statement.setString(7, report.getDuration());
            }

            statement.setLong(8, reportCreationResponse.reportMessageID());
            statement.setLong(9, modlogMessageID);
            statement.setLong(10, reportCreationResponse.reminderMessageID());

            statement.executeUpdate();
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

    public boolean addTemporaryPunishment(Connection connection, Report report, int punishmentID) throws SQLException {
        var statement = connection.prepareStatement("INSERT INTO TemporaryPunishments (report_id, timestamp) VALUES (?, ?)");
        statement.setInt(1, punishmentID);
        statement.setLong(2, System.currentTimeMillis() + DurationUtils.durationStringToMillis(report.getDuration()));

        statement.execute();
        statement.close();
        return true;
    }
}
