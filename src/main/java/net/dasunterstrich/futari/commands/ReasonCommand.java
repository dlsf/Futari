package net.dasunterstrich.futari.commands;

import net.dasunterstrich.futari.commands.internal.BotCommand;
import net.dasunterstrich.futari.database.DatabaseHandler;
import net.dasunterstrich.futari.moderation.modlog.ModlogManager;
import net.dasunterstrich.futari.moderation.reports.ReportManager;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dasunterstrich.futari.utils.ExceptionUtils;
import net.dasunterstrich.futari.utils.StringUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class ReasonCommand extends BotCommand {
    private final DatabaseHandler databaseHandler;
    private final ReportManager reportManager;
    private final ModlogManager modlogManager;

    public ReasonCommand(DatabaseHandler databaseHandler, ReportManager reportManager, ModlogManager modlogManager) {
        super("reason", Permission.BAN_MEMBERS, "reason <CaseID> <Reason>");

        this.databaseHandler = databaseHandler;
        this.reportManager = reportManager;
        this.modlogManager = modlogManager;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("reason", "Updates the reason of a case")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                .addOption(OptionType.INTEGER, "case_id", "The ID of the case", true)
                .addOption(OptionType.STRING, "reason", "The new reason for this case", true);
    }

    @Override
    public void onTextCommand(MessageReceivedEvent event) {
        var words = event.getMessage().getContentRaw().split(" ");
        if (words.length < 3) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Please use `!reason `<CaseID> <Reason>`")).queue();
            return;
        }

        if (!StringUtils.isInteger(words[1])) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Invalid case ID. Please use `!reason `<CaseID> <Reason>`")).queue();
            return;
        }

        var caseID = Integer.parseInt(words[1]);
        var reason = String.join(" ", Arrays.copyOfRange(words, 2, words.length));
        updateReason(event.getGuild(), caseID, reason).handleAsync((success, throwable) -> {
            if (throwable != null) {
                event.getChannel().sendMessageEmbeds(EmbedUtils.error("An unexpected error occurred: " + ExceptionUtils.stringify(throwable))).queue();
            } else if (!success) {
                event.getChannel().sendMessageEmbeds(EmbedUtils.error("Could not change reason, the case ID might be invalid")).queue();
            } else {
                event.getChannel().sendMessageEmbeds(EmbedUtils.success("Reason for Case #" + caseID + " updated", "**New Reason**: " + reason)).queue();
            }

            return null;
        });
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        var caseID = event.getOption("case_id").getAsInt();
        var reason = event.getOption("reason").getAsString();

        updateReason(event.getGuild(), caseID, reason).handleAsync((success, throwable) -> {
            if (throwable != null) {
                event.getHook().editOriginalEmbeds(EmbedUtils.error("An unexpected error occurred: " + ExceptionUtils.stringify(throwable))).queue();
            } else if (!success) {
                event.getHook().editOriginalEmbeds(EmbedUtils.error("Could not change reason, the case ID might be invalid")).queue();
            } else {
                event.getHook().editOriginalEmbeds(EmbedUtils.success("Reason for Case #" + caseID + " updated", "**New Reason**: " + reason)).queue();
            }

            return null;
        });
    }

    private CompletableFuture<Boolean> updateReason(Guild guild, int caseID, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try (var connection = databaseHandler.getConnection(); var statement = connection.createStatement()) {
                var resultSet = statement.executeQuery("SELECT * FROM Punishments WHERE id = " + caseID);
                if (!resultSet.next()) return false;

                var reportID = resultSet.getLong("id");
                var userID = resultSet.getLong("user_id");
                var reportMessageID = resultSet.getLong("report_message_id");
                var modlogMessageID = resultSet.getLong("modlog_message_id");

                var user = guild.getJDA().retrieveUserById(userID).complete();
                var reportThreadID = reportManager.getReportThreadID(user);
                var reportThread = findReportThreadByID(guild, reportThreadID);
                var reportMessage = reportThread.retrieveMessageById(reportMessageID).complete();

                var modlogChannel = guild.getTextChannelById(1073722655628341429L);
                var modlogMessage = modlogChannel.retrieveMessageById(modlogMessageID).complete();

                var report = reportManager.getReportByID(reportID);
                report.setReason(reason);

                reportManager.updateReportMessage(reportMessage, report);
                modlogManager.updateModlogMessage(modlogMessage, report);

                statement.close();

                var preparedStatement = connection.prepareStatement("UPDATE Punishments SET reason = ? WHERE id = " + caseID);
                preparedStatement.setString(1, reason);
                preparedStatement.execute();
                preparedStatement.close();
                return true;
            } catch (Exception exception) {
                exception.printStackTrace();
                return false;
            }
        });
    }

    private ThreadChannel findReportThreadByID(Guild guild, long reportThreadID) {
        var reportThread = guild.getThreadChannelById(reportThreadID);

        // Try to fetch archived channels if it's not in the cache
        if (reportThread == null) {
            guild.getForumChannelById(1073212604090167357L).retrieveArchivedPublicThreadChannels().complete();
            reportThread = guild.getThreadChannelById(reportThreadID);

            if (reportThread == null) throw new IllegalStateException("Unable to find report thread");
        }

        return reportThread;
    }
}
