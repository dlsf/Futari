package net.dasunterstrich.futari.commands;

import net.dasunterstrich.futari.commands.internal.BotCommand;
import net.dasunterstrich.futari.moderation.Punisher;
import net.dasunterstrich.futari.reports.ReportedMessage;
import net.dasunterstrich.futari.utils.DiscordUtils;
import net.dasunterstrich.futari.utils.DurationUtils;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class MuteCommand extends BotCommand {
    private final Punisher punisher;

    public MuteCommand(Punisher punisher) {
        super("mute", "Mute user", Permission.BAN_MEMBERS);

        this.punisher = punisher;
    }

    @Nullable
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
    public CommandData getModalCommandData() {
        return Commands.context(Command.Type.MESSAGE, "mute_modal")
                .setName(getInteractionMenuName())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS));
    }

    @Nullable
    @Override
    public Modal buildModal(MessageContextInteractionEvent event) {
        var message = event.getTarget();
        var author = message.getAuthor();

        return Modal.create("mute:" + author.getId() + ":" + message.getId(), "Mute " + author.getAsTag())
                .addActionRows(ActionRow.of(TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH).setPlaceholder("Reason").setRequired(true).build()))
                .addActionRows(ActionRow.of(TextInput.create("duration", "Duration (e.g. 3d)", TextInputStyle.SHORT).setPlaceholder("Duration").setRequired(false).build()))
                .addActionRows(ActionRow.of(TextInput.create("comments", "Further comments", TextInputStyle.PARAGRAPH).setPlaceholder("Comments").setRequired(false).build()))
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

        // TODO: Error handling
        var muteable = punisher.mute(event.getGuild(), targetMember, event.getMember(), reason, duration, "", ReportedMessage.none());
        if (!muteable.success()) return;

        event.getChannel().sendMessageEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " was muted", "**Reason**: " + reason)).queue();
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        var targetMember = event.getOption("user").getAsMember();
        var reason = event.getOption("reason").getAsString();

        var commentsOption = event.getOption("comments");
        var evidenceOption = event.getOption("evidence");
        var durationOption = event.getOption("duration");
        var comments = commentsOption == null ? "" : commentsOption.getAsString();
        var evidence = evidenceOption == null ? ReportedMessage.none() : ReportedMessage.ofEvidence(evidenceOption.getAsAttachment());
        var duration = durationOption == null ? "" : durationOption.getAsString();

        // TODO: Error handling
        punisher.mute(event.getGuild(), targetMember, event.getMember(), reason, duration, comments, evidence);

        event.replyEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " muted", "**Reason**: " + reason)).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        try {
            event.deferReply(true).queue();

            event.getGuild().retrieveMemberById(event.getModalId().split(":")[1]).queue(targetUser -> {
                var channel = event.getChannel();
                channel.retrieveMessageById(event.getModalId().split(":")[2]).queue(message -> {
                    var reason = event.getInteraction().getValue("reason").getAsString();
                    var duration = event.getInteraction().getValue("duration").getAsString();
                    var comments = event.getInteraction().getValue("comments").getAsString();

                    // TODO: Error handling
                    var muteable = punisher.mute(event.getGuild(), targetUser, event.getMember(), reason, duration, comments, new ReportedMessage(message.getContentRaw(), message.getAttachments()));
                    if (!muteable.success()) return;

                    event.getHook().editOriginalEmbeds(EmbedUtils.success(targetUser.getUser().getAsTag() + " was muted. **Reason**: " + reason)).queue();
                });
            });

        } finally {
            if (!event.isAcknowledged()) {
                event.replyEmbeds(EmbedUtils.error("An error occurred, the user was **not** muted!")).setEphemeral(true).queue();
            }
        }
    }
}
