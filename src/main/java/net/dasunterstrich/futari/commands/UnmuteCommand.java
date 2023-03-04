package net.dasunterstrich.futari.commands;

import net.dasunterstrich.futari.commands.internal.BotCommand;
import net.dasunterstrich.futari.moderation.Punisher;
import net.dasunterstrich.futari.moderation.reports.EvidenceMessage;
import net.dasunterstrich.futari.utils.DiscordUtils;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dasunterstrich.futari.utils.ExceptionUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Arrays;

public class UnmuteCommand extends BotCommand {
    private final Punisher punisher;

    public UnmuteCommand(Punisher punisher) {
        super("unmute", Permission.BAN_MEMBERS, "unmute <User> [Reason]");

        this.punisher = punisher;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("unmute", "Unmute a user")
                .addOption(OptionType.USER, "user", "The user to unmute", true)
                .addOption(OptionType.STRING, "reason", "Reason for the unmute", true);
    }

    @Override
    public void onTextCommand(MessageReceivedEvent event) {
        var words = event.getMessage().getContentRaw().split(" ");
        if (words.length < 3) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Please use `!unmute <User> <Reason>`")).queue();
            return;
        }

        var targetMemberOptional = DiscordUtils.parseStringAsMember(event.getGuild(), words[1]);
        if (targetMemberOptional.isEmpty()) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Cannot unmute, invalid user provided")).queue();
            return;
        }

        var targetMember = targetMemberOptional.get();
        var reason = String.join(" ", Arrays.copyOfRange(words, 2, words.length));

        punisher.unmute(event.getGuild(), targetMember, event.getMember(), reason, "", EvidenceMessage.empty()).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) {
                event.getChannel().sendMessageEmbeds(EmbedUtils.error("Could not unmute" + targetMember.getUser().getAsTag() + "!\n\n**Reason**: " + ExceptionUtils.stringify(throwable))).queue();
                return null;
            }

            var text = "**Reason**: " + reason + (communicationResponse.isFailure() ? "\n\n**Warning: Could not contact user**" : "");
            event.getChannel().sendMessageEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " unmuted", text)).queue();
            return null;
        });
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        var targetMember = event.getOption("user").getAsMember();
        var reason = event.getOption("reason").getAsString();

        punisher.unmute(event.getGuild(), targetMember, event.getMember(), reason, "", EvidenceMessage.empty()).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) {
                event.getHook().editOriginalEmbeds(EmbedUtils.error("Could not unmute " + targetMember.getUser().getAsTag() + "!\n\n**Reason**: " + ExceptionUtils.stringify(throwable))).queue();
                return null;
            }

            var text = "**Reason**: " + reason + (communicationResponse.isFailure() ? "\n\n**Warning: Could not contact user**" : "");
            event.getHook().editOriginalEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " unmuted", text)).queue();
            return null;
        });
    }
}
