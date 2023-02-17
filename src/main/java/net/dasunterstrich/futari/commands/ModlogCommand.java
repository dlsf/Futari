package net.dasunterstrich.futari.commands;

import net.dasunterstrich.futari.commands.internal.BotCommand;
import net.dasunterstrich.futari.database.DatabaseHandler;
import net.dasunterstrich.futari.moderation.PunishmentType;
import net.dasunterstrich.futari.utils.DiscordUtils;
import net.dasunterstrich.futari.utils.DurationUtils;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModlogCommand extends BotCommand {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DatabaseHandler databaseHandler;

    public ModlogCommand(DatabaseHandler databaseHandler) {
        super("modlog", Permission.BAN_MEMBERS, "modlog <User>");

        this.databaseHandler = databaseHandler;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("modlog", "Display the modlog of a user")
                .addOption(OptionType.USER, "user", "The user to check", true);
    }

    @Override
    public void onTextCommand(MessageReceivedEvent event) {
        var words = event.getMessage().getContentRaw().split(" ");
        if (words.length == 0) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Please use `!modlog <User>`")).queue();
            return;
        }

        var targetUserOptional = DiscordUtils.parseStringAsUser(event.getJDA(), words[1]);
        if (targetUserOptional.isEmpty()) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Cannot check modlog, invalid user provided")).queue();
            return;
        }

        var targetUser = targetUserOptional.get();
        event.getChannel().sendMessageEmbeds(buildModlogEmbed(event.getGuild(), targetUser)).queue();
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        var user = event.getOption("user").getAsUser();

        event.replyEmbeds(buildModlogEmbed(event.getGuild(), user)).queue();
    }

    private MessageEmbed buildModlogEmbed(Guild guild, User user) {
        var stringBuilder = new StringBuilder();

        try (var connection = databaseHandler.getConnection(); var statement = connection.createStatement()) {
            var resultSet = statement.executeQuery("SELECT * FROM Punishments WHERE user_id = " + user.getIdLong() + " ORDER BY id DESC");
            if (!resultSet.next()) {
                return EmbedUtils.error("User doesn't have a modlog");
            }

            do {
                var id = resultSet.getInt("id");
                var moderatorId = resultSet.getLong("moderator_id");
                var type = PunishmentType.valueOf(resultSet.getString("type"));
                var reason = resultSet.getString("reason");
                var timestamp = resultSet.getLong("timestamp");
                var duration = resultSet.getString("duration");

                appendToModlog(guild.getJDA(), stringBuilder, id, moderatorId, type, reason, timestamp, duration);
            } while (resultSet.next());
        } catch (Exception exception) {
            logger.error("Failed to check modlog", exception);
            return EmbedUtils.error("Internal bot error, unable to check modlog");
        }

        var body = stringBuilder.toString();
        return EmbedUtils.success("Modlog for " + user.getAsTag(), body.substring(0, body.length() - 1));
    }

    private void appendToModlog(JDA jda, StringBuilder stringBuilder, int id, long moderatorID, PunishmentType punishmentType, String reason, long timestamp, String duration) {
        var moderator = jda.getUserById(moderatorID);
        var moderatorString = moderator == null ? "Unknown (" + moderatorID + ")" : moderator.getAsMention();

        stringBuilder.append("Case #").append(id).append(": **").append(punishmentType.getName()).append("**\n");
        stringBuilder.append("**Reason**: ").append(reason).append("\n");
        stringBuilder.append("**Moderator**: ").append(moderatorString).append("\n");

        if (!duration.isEmpty()) {
            stringBuilder.append("**Duration**: ").append(DurationUtils.toReadableDuration(duration)).append("\n");
        }

        stringBuilder.append("<t:").append(timestamp).append(">\n\n");
    }
}
