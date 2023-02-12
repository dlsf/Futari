package net.dasunterstrich.futari.moderation;

import net.dasunterstrich.futari.database.DatabaseHandler;
import net.dasunterstrich.futari.moderation.modules.*;
import net.dasunterstrich.futari.reports.EvidenceMessage;
import net.dasunterstrich.futari.reports.Report;
import net.dasunterstrich.futari.reports.ReportManager;
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
import java.util.function.Consumer;

public class Punisher {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final DatabaseHandler databaseHandler;
    public final ReportManager reportManager;

    private final PunishmentModule banModule;
    private final PunishmentModule muteModule;
    private final PunishmentModule warnModule;
    private final PunishmentModule kickModule;

    public Punisher(DatabaseHandler databaseHandler, ReportManager reportManager) {
        this.databaseHandler = databaseHandler;
        this.reportManager = reportManager;

        this.banModule = new BanModule(this);
        this.muteModule = new MuteModule(this);
        this.warnModule = new WarnModule(this);
        this.kickModule = new KickModule(this);
    }

    public PunishmentResponse ban(Guild guild, Member member, Member moderator, String reason, String duration, String comments, EvidenceMessage evidenceMessage) {
        return banModule.apply(guild, member, moderator, reason, duration, comments, evidenceMessage);
    }

    public PunishmentResponse unban(Guild guild, User user, Member moderator, String reason, String comment, EvidenceMessage evidenceMessage) {
        return banModule.revoke(guild, user, moderator, reason, comment, evidenceMessage);
    }

    public PunishmentResponse mute(Guild guild, Member member, Member moderator, String reason, String duration, String comments, EvidenceMessage evidenceMessage) {
        return muteModule.apply(guild, member, moderator, reason, duration, comments, evidenceMessage);
    }

    public PunishmentResponse unmute(Guild guild, Member member, Member moderator, String reason, String comment, EvidenceMessage evidenceMessage) {
        return muteModule.revoke(guild, member.getUser(), moderator, reason, comment, evidenceMessage);
    }

    public PunishmentResponse warn(Guild guild, Member member, Member moderator, String reason, String comments, EvidenceMessage evidenceMessage) {
        return warnModule.apply(guild, member, moderator, reason, comments, evidenceMessage);
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
                result -> {}
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

    public PunishmentResponse kick(Guild guild, Member member, Member moderator, String reason, String comments, EvidenceMessage evidenceMessage) {
        return kickModule.apply(guild, member, moderator, reason, comments, evidenceMessage);
    }

    public void contactUserAndThen(User user, MessageEmbed embed, Consumer<? super Result<Message>> action) {
        user.openPrivateChannel()
                .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(embed))
                .mapToResult()
                .queue(action);

    }

    public boolean addPunishmentToDatabase(Report report) {
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

    public boolean addTemporaryPunishment(Connection connection, Report report, int punishmentID) throws SQLException {
        var statement = connection.prepareStatement("INSERT INTO TemporaryPunishments (report_id, timestamp) VALUES (?, ?)");
        statement.setInt(1, punishmentID);
        statement.setLong(2, System.currentTimeMillis() + DurationUtils.durationStringToMillis(report.getDuration()));

        statement.execute();
        statement.close();
        return true;
    }
}
