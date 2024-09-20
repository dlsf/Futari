package moe.das.futari.listener;

import moe.das.futari.database.DatabaseHandler;
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
            var resultSet = statement.executeQuery("SELECT * FROM Punishments P LEFT JOIN TemporaryPunishments TP ON P.id = TP.report_id WHERE (type = 'MUTE' OR type = 'UNMUTE') AND user_id = " + memberID);
            var mute = false;

            // Only the last iteration matters, we want to know what mute/unmute happened last to this user
            while (resultSet.next()) {
                var type = resultSet.getString("type");
                switch (type) {
                    case "UNMUTE" -> mute = false;
                    case "MUTE" -> {
                        // Mute if permanent
                        if (resultSet.getString("duration").isEmpty()) {
                            mute = true;
                        } else if (resultSet.getInt("done") == 0) {
                            mute = true;
                        } else {
                            mute = false;
                        }
                    }
                }
            }

            if (mute) {
                addMutedRole(event.getGuild(), event.getMember());
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
