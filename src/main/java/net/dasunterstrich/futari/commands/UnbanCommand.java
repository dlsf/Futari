package net.dasunterstrich.futari.commands;

import net.dasunterstrich.futari.commands.internal.BotCommand;
import net.dasunterstrich.futari.moderation.Punisher;
import net.dasunterstrich.futari.reports.ReportedMessage;
import net.dasunterstrich.futari.utils.DiscordUtils;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class UnbanCommand extends BotCommand {
    private final Punisher punisher;

    public UnbanCommand(Punisher punisher) {
        super("unban", Permission.BAN_MEMBERS);

        this.punisher = punisher;
    }

    @Nullable
    @Override
    public CommandData getCommandData() {
        return Commands.slash("unban", "Unban a user")
                .addOption(OptionType.USER, "user", "The user to unban", true)
                .addOption(OptionType.STRING, "reason", "Reason for the unban", false)
                .addOption(OptionType.STRING, "comments", "Further comments for other moderators", false)
                .addOption(OptionType.ATTACHMENT, "evidence", "Screenshot of additional evidence", false);
    }

    @Override
    public void onTextCommand(MessageReceivedEvent event) {
        var words = event.getMessage().getContentRaw().split(" ");
        if (words.length < 3) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Please use `!unban <User> <Reason>`")).queue();
            return;
        }

        var targetUserOptional = DiscordUtils.parseStringAsUser(event.getJDA(), words[1]);
        if (targetUserOptional.isEmpty()) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Cannot unban, invalid user provided")).queue();
            return;
        }

        var targetUser = targetUserOptional.get();
        var reason = String.join(" ", Arrays.copyOfRange(words, 2, words.length));

        punisher.unban(event.getGuild(), targetUser, event.getMember(), reason, "", ReportedMessage.none());

        event.getChannel().sendMessageEmbeds(EmbedUtils.success(targetUser.getAsTag() + " unbanned", "**Reason**: " + reason)).queue();
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        var targetUser = event.getOption("user").getAsUser();
        var reason = event.getOption("reason").getAsString();

        var commentsOption = event.getOption("comments");
        var evidenceOption = event.getOption("evidence");
        var comments = commentsOption == null ? "" : commentsOption.getAsString();
        var evidence = evidenceOption == null ? ReportedMessage.none() : ReportedMessage.ofEvidence(evidenceOption.getAsAttachment());

        punisher.unban(event.getGuild(), targetUser, event.getMember(), reason, comments, evidence);

        event.replyEmbeds(EmbedUtils.success(targetUser.getAsTag() + " unbanned", "**Reason**: " + reason)).queue();
    }
}
