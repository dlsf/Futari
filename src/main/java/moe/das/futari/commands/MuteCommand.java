package moe.das.futari.commands;

import moe.das.futari.commands.internal.BotCommand;
import moe.das.futari.moderation.Punisher;
import moe.das.futari.moderation.reports.EvidenceMessage;
import moe.das.futari.utils.DiscordUtils;
import moe.das.futari.utils.DurationUtils;
import moe.das.futari.utils.EmbedUtils;
import moe.das.futari.utils.ExceptionUtils;
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

public class MuteCommand extends BotCommand {
    private final Punisher punisher;

    public MuteCommand(Punisher punisher) {
        super("mute", "Mute user", Permission.BAN_MEMBERS, "mute <User> [Duration] <Reason>");

        this.punisher = punisher;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("mute", "Mute a user")
                .addOption(OptionType.USER, "user", "The user to mute", true)
                .addOption(OptionType.STRING, "reason", "Reason for the mute", true)
                .addOption(OptionType.STRING, "duration", "Duration of the mute", false)
                .addOption(OptionType.STRING, "comments", "Further comments for other moderators", false)
                .addOption(OptionType.ATTACHMENT, "evidence", "Screenshot of additional evidence", false);
    }

    @Nullable
    @Override
    public CommandData getModalCommandData(Command.Type type) {
        return Commands.context(type, "mute_modal")
                .setName(getInteractionMenuName())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS));
    }

    @Nullable
    @Override
    public Modal buildModal(User user, Optional<Message> messageOptional) {
        var messageID = messageOptional.map(Message::getId).orElse("NONE");

        return Modal.create("mute:" + user.getId() + ":" + messageID, "Mute " + user.getAsTag())
                .addComponents(ActionRow.of(TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH).setPlaceholder("Reason").setRequired(true).build()))
                .addComponents(ActionRow.of(TextInput.create("duration", "Duration (e.g. 3d)", TextInputStyle.SHORT).setPlaceholder("Duration").setRequired(false).build()))
                .addComponents(ActionRow.of(TextInput.create("comments", "Further comments", TextInputStyle.PARAGRAPH).setPlaceholder("Comments").setRequired(false).build()))
                .build();
    }

    @Override
    public void onTextCommand(MessageReceivedEvent event) {
        var words = event.getMessage().getContentRaw().split(" ");
        if (words.length < 3) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Please use `!mute <User> [Duration] <Reason>`")).queue();
            return;
        }

        var targetMemberOptional = DiscordUtils.parseStringAsMember(event.getGuild(), words[1]);
        if (targetMemberOptional.isEmpty()) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Cannot mute, invalid user provided")).queue();
            return;
        }

        var targetMember = targetMemberOptional.get();
        String reason;

        var duration = words[2];
        if (DurationUtils.isValidDurationString(duration)) {
            reason = String.join(" ", Arrays.copyOfRange(words, 3, words.length));
        } else {
            reason = String.join(" ", Arrays.copyOfRange(words, 2, words.length));
            duration = "";
        }

        punisher.mute(event.getGuild(), targetMember, event.getMember(), reason, duration, "", EvidenceMessage.empty()).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) {
                event.getChannel().sendMessageEmbeds(EmbedUtils.error("Could not mute " + targetMember.getUser().getAsTag() + "!\n\n**Reason**: " + ExceptionUtils.stringify(throwable))).queue();
                return null;
            }

            var text = "**Reason**: " + reason + (communicationResponse.isFailure() ? "\n\n**Warning: Could not contact user**" : "");
            event.getChannel().sendMessageEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " muted", text)).queue();
            return null;
        });
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        var targetMember = event.getOption("user").getAsMember();
        var reason = event.getOption("reason").getAsString();
        var duration = optionalOption(event.getOption("duration"), OptionMapping::getAsString, "");
        var comments = optionalOption(event.getOption("comments"), OptionMapping::getAsString, "");
        var evidence = optionalOption(event.getOption("evidence"), option -> EvidenceMessage.ofEvidence(option.getAsAttachment()), EvidenceMessage.empty());

        punisher.mute(event.getGuild(), targetMember, event.getMember(), reason, duration, comments, evidence).handleAsync((communicationResponse, throwable) -> {
            if (throwable != null) {
                event.getHook().editOriginalEmbeds(EmbedUtils.error("Could not mute " + targetMember.getUser().getAsTag() + "!\n\n**Reason**: " + ExceptionUtils.stringify(throwable))).queue();
                return null;
            }

            var text = "**Reason**: " + reason + (communicationResponse.isFailure() ? "\n\n**Warning: Could not contact user**" : "");
            event.getHook().editOriginalEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " muted", text)).queue();
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
             var duration = event.getInteraction().getValue("duration").getAsString();
             var comments = event.getInteraction().getValue("comments").getAsString();
             var evidenceMessage = new EvidenceMessage(message.getContentRaw(), message.getAttachments());

             punisher.mute(event.getGuild(), targetMember, event.getMember(), reason, duration, comments, evidenceMessage).handleAsync((communicationResponse, throwable) -> {
                 if (throwable != null) {
                     event.getHook().editOriginalEmbeds(EmbedUtils.error("Could not mute " + targetMember.getUser().getAsTag() + "!\n\n**Reason**: " + ExceptionUtils.stringify(throwable))).queue();
                     return null;
                 }

                 var text = "**Reason**: " + reason + (communicationResponse.isFailure() ? "\n\n**Warning: Could not contact user**" : "");
                 event.getHook().editOriginalEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " muted", text)).queue();
                 return null;
             });
         });
    }
}
