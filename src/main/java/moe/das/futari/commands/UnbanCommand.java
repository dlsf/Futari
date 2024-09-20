package moe.das.futari.commands;

import moe.das.futari.commands.internal.BotCommand;
import moe.das.futari.moderation.Punisher;
import moe.das.futari.moderation.reports.EvidenceMessage;
import moe.das.futari.utils.DiscordUtils;
import moe.das.futari.utils.EmbedUtils;
import moe.das.futari.utils.ExceptionUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Arrays;

public class UnbanCommand extends BotCommand {
    private final Punisher punisher;

    public UnbanCommand(Punisher punisher) {
        super("unban", Permission.BAN_MEMBERS, "unban <User> [Reason]");

        this.punisher = punisher;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("unban", "Unban a user")
                .addOption(OptionType.USER, "user", "The user to unban", true)
                .addOption(OptionType.STRING, "reason", "Reason for the unban", true);
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

        punisher.unban(event.getGuild(), targetUser, event.getMember(), reason, "", EvidenceMessage.empty()).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) {
                event.getChannel().sendMessageEmbeds(EmbedUtils.error("Could not unban " + targetUser.getAsTag() + "!\n\n**Reason**: " + ExceptionUtils.stringify(throwable))).queue();
                return null;
            }

            var text = "**Reason**: " + reason + (communicationResponse.isFailure() ? "\n\n**Warning: Could not contact user**" : "");
            event.getChannel().sendMessageEmbeds(EmbedUtils.success(targetUser.getAsTag() + " unbanned", text)).queue();
            return null;
        });
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        var targetUser = event.getOption("user").getAsUser();
        var reason = event.getOption("reason").getAsString();

        punisher.unban(event.getGuild(), targetUser, event.getMember(), reason, "", EvidenceMessage.empty()).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) {
                event.getHook().editOriginalEmbeds(EmbedUtils.error("Could not unban " + targetUser.getAsTag() + "!\n\n**Reason**: " + ExceptionUtils.stringify(throwable))).queue();
                return null;
            }

            var text = "**Reason**: " + reason + (communicationResponse.isFailure() ? "\n\n**Warning: Could not contact user**" : "");
            event.getHook().editOriginalEmbeds(EmbedUtils.success(targetUser.getAsTag() + " unbanned", text)).queue();
            return null;
        });

    }
}
