package net.dasunterstrich.futari.listener;

import net.dasunterstrich.futari.database.DatabaseHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuildMemberJoinListener extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DatabaseHandler databaseHandler;

    public GuildMemberJoinListener(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        var memberID = event.getMember().getIdLong();

        try (var connection = databaseHandler.getConnection(); var statement = connection.createStatement()) {
            var resultSet = statement.executeQuery("SELECT * FROM Punishments WHERE user_id = " + memberID);
            if (resultSet.next()) {
                addMutedRole(event.getGuild(), event.getMember());
                return;
            }

            resultSet = statement.executeQuery("SELECT * FROM TemporaryPunishments TP INNER JOIN Punishments P on TP.report_id = P.id WHERE TP.timestamp >= " + System.currentTimeMillis() + " AND P.user_id = " + memberID);
            if (resultSet.next()) {
                addMutedRole(event.getGuild(), event.getMember());
                return;
            }
        } catch (Exception exception) {
            logger.error("Failed to assign mute role to " + memberID, exception);
        }
    }

    private void addMutedRole(Guild guild, Member member) {
        guild.addRoleToMember(member.getUser(), guild.getRoleById(1073208950280953906L)).reason("Muted").queue();

        logger.info("Assigned muted role to " + member.getUser().getAsTag() + " after rejoin");
    }
}
