package net.dasunterstrich.futari.moderation;

import net.dasunterstrich.futari.database.DatabaseHandler;
import net.dasunterstrich.futari.reports.ReportType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimedPunishmentHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final DatabaseHandler databaseHandler;
    private final Punisher punisher;
    private final JDA jda;
    private final Guild guild;

    public TimedPunishmentHandler(DatabaseHandler databaseHandler, Punisher punisher, JDA jda, Guild guild) {
        this.databaseHandler = databaseHandler;
        this.punisher = punisher;
        this.jda = jda;
        this.guild = guild;

        executorService.scheduleAtFixedRate(this::checkForExpiredPunishments, 0, 5, TimeUnit.SECONDS);
    }

    private void checkForExpiredPunishments() {
        logger.debug("Checking for expired punishments");

        try (var connection = databaseHandler.getConnection(); var statement = connection.createStatement()) {
            var resultSet = statement.executeQuery("SELECT report_id, user_id, type FROM TemporaryPunishments INNER JOIN Punishments P on TemporaryPunishments.report_id = P.id WHERE TemporaryPunishments.timestamp <= " + System.currentTimeMillis());

            var revokedPunishments = new HashSet<Integer>();
            while (resultSet.next()) {
                var user = resultSet.getLong("user_id");
                var type = resultSet.getString("type");
                revokePunishments(user, ReportType.valueOf(type));

                revokedPunishments.add(resultSet.getInt("report_id"));
            }

            revokedPunishments.forEach(reportID -> {
                try {
                    statement.execute("DELETE FROM TemporaryPunishments WHERE report_id = " + reportID);

                    logger.debug("Revoked punishment with report ID " +  reportID);
                } catch (SQLException exception) {
                    logger.error("Could not remove expired punishment " + reportID, exception);
                }
            });
        } catch (Exception exception) {
            logger.error("Could not check for expired punishments", exception);
        }
    }

    private void revokePunishments(long userID, ReportType reportType) throws SQLException {
        switch (reportType) {
            case BAN -> {
                jda.retrieveUserById(userID).queue(user -> {
                    punisher.unban(guild, user, guild.getSelfMember(), "Auto");
                });
            }
            case MUTE -> {
                guild.retrieveMemberById(userID).queue(member -> {
                    punisher.unmute(guild, member, guild.getSelfMember(), "Auto");
                });
            }
            default -> {
                logger.warn("Encountered unexpected argument while automatically revoking punishment: " + reportType.name());
            }
        }
    }
}
