package net.dasunterstrich.futari.commands;

import net.dasunterstrich.futari.commands.internal.BotCommand;
import net.dasunterstrich.futari.moderation.Punisher;
import net.dasunterstrich.futari.moderation.reports.EvidenceMessage;
import net.dasunterstrich.futari.utils.DiscordUtils;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

public class WarnCommand extends BotCommand {
    private final Punisher punisher;

    public WarnCommand(Punisher punisher) {
        super("warn", "Warn user", Permission.BAN_MEMBERS, "warn <User> <Reason>");

        this.punisher = punisher;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("warn", "Warn a user")
                .addOption(OptionType.USER, "user", "The user to warn", true)
                .addOption(OptionType.STRING, "reason", "Reason for the warn", true)
                .addOption(OptionType.STRING, "comments", "Further comments for other moderators", false)
                .addOption(OptionType.ATTACHMENT, "evidence", "Screenshot of additional evidence", false);
    }

    @Nullable
    @Override
    public CommandData getModalCommandData(Command.Type type) {
        return Commands.context(type, "warn_modal")
                .setName(getInteractionMenuName())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS));
    }

    @Nullable
    @Override
    public Modal buildModal(User user, Optional<Message> messageOptional) {
        var messageID = messageOptional.map(Message::getId).orElse("NONE");

        return Modal.create("warn:" + user.getId() + ":" + messageID, "Warn " + user.getAsTag())
                .addActionRows(ActionRow.of(TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH).setPlaceholder("Reason").setRequired(true).build()))
                .addActionRows(ActionRow.of(TextInput.create("comments", "Further comments", TextInputStyle.PARAGRAPH).setPlaceholder("Comments").setRequired(false).build()))
                .build();
    }

    @Override
    public void onTextCommand(MessageReceivedEvent event) {
        var words = event.getMessage().getContentRaw().split(" ");
        if (words.length < 3) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Please use `!warn <User> <Reason>`")).queue();
            return;
        }

        var targetMemberOptional = DiscordUtils.parseStringAsMember(event.getGuild(), words[1]);
        if (targetMemberOptional.isEmpty()) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Cannot mute, invalid user provided")).queue();
            return;
        }

        var targetMember = targetMemberOptional.get();
        var reason = String.join(" ", Arrays.copyOfRange(words, 2, words.length));

        punisher.warn(event.getGuild(), targetMember, event.getMember(), reason, "", EvidenceMessage.empty()).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) {
                event.getChannel().sendMessageEmbeds(EmbedUtils.error("Could not warn " + targetMember.getUser().getAsTag() + "!\n\n**Reason**: " + throwable.getMessage())).queue();
                return null;
            }

            var text = "**Reason**: " + reason + (communicationResponse.isFailure() ? "\n\n**Warning: Could not contact user**" : "");
            event.getChannel().sendMessageEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " warned", text)).queue();
            return null;
        });
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        var targetMember = event.getOption("user").getAsMember();
        var reason = event.getOption("reason").getAsString();
        var comments = optionalOption(event.getOption("comments"), OptionMapping::getAsString, "");
        var evidence = optionalOption(event.getOption("evidence"), option -> EvidenceMessage.ofEvidence(option.getAsAttachment()), EvidenceMessage.empty());

        punisher.warn(event.getGuild(), targetMember, event.getMember(), reason, comments, evidence).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) {
                event.getHook().editOriginalEmbeds(EmbedUtils.error("Could not warn " + targetMember.getUser().getAsTag() + "!\n\n**Reason**: " + throwable.getMessage())).queue();
                return null;
            }

            var text = "**Reason**: " + reason + (communicationResponse.isFailure() ? "\n\n**Warning: Could not contact user**" : "");
            event.getHook().editOriginalEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " warned", text)).queue();
            return null;
        });
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        event.deferReply(true).queue();

        event.getGuild().retrieveMemberById(event.getModalId().split(":")[1]).queue(targetMember -> {
            var channel = event.getChannel();
            var message = channel.retrieveMessageById(event.getModalId().split(":")[2]).complete();

            var reason = event.getInteraction().getValue("reason").getAsString();
            var comments = event.getInteraction().getValue("comments").getAsString();
            var evidenceMessage = new EvidenceMessage(message.getContentRaw(), message.getAttachments());

            punisher.warn(event.getGuild(), targetMember, event.getMember(), reason, comments, evidenceMessage).handleAsync((communicationResponse, throwable) -> {
                if (throwable != null) {
                    event.getHook().editOriginalEmbeds(EmbedUtils.error("Could not warn " + targetMember.getUser().getAsTag() + "!\n\n**Reason**: " + throwable.getMessage())).queue();
                    return null;
                }
                var text = "**Reason**: " + reason + (communicationResponse.isFailure() ? "\n\n**Warning: Could not contact user**" : "");
                event.getHook().editOriginalEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " warned", text)).queue();
                return null;
            });
        });
    }
}
