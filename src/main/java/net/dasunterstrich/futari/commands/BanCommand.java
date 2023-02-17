package net.dasunterstrich.futari.commands;

import net.dasunterstrich.futari.commands.internal.BotCommand;
import net.dasunterstrich.futari.moderation.Punisher;
import net.dasunterstrich.futari.moderation.reports.EvidenceMessage;
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

public class BanCommand extends BotCommand {
    private final int DEFAULT_INTERVAL = 1;
    private final Punisher punisher;

    public BanCommand(Punisher punisher) {
        super("ban", "Ban user", Permission.BAN_MEMBERS, "ban <User> [Duration] <Reason>");

        this.punisher = punisher;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("ban", "Ban a user")
                .addOption(OptionType.USER, "user", "The user to ban", true)
                .addOption(OptionType.STRING, "reason", "Reason for the ban", true)
                .addOption(OptionType.STRING, "duration", "Duration of the ban", false)
                .addOption(OptionType.INTEGER, "delete_messages", "How many messages should be deleted", false, true)
                .addOption(OptionType.STRING, "comments", "Further comments for other moderators", false)
                .addOption(OptionType.ATTACHMENT, "evidence", "Screenshot of additional evidence", false);
    }

    @Nullable
    @Override
    public CommandData getModalCommandData() {
        return Commands.context(Command.Type.MESSAGE, "ban_modal")
                .setName(getInteractionMenuName())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS));
    }

    @Nullable
    @Override
    public Modal buildModal(MessageContextInteractionEvent event) {
        var message = event.getTarget();
        var author = message.getAuthor();

        return Modal.create("ban:" + author.getId() + ":" + message.getId(), "Ban " + author.getAsTag())
                .addActionRows(ActionRow.of(TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH).setPlaceholder("Reason").setRequired(true).build()))
                .addActionRows(ActionRow.of(TextInput.create("duration", "Duration (e.g. 3d)", TextInputStyle.SHORT).setPlaceholder("Duration").setRequired(false).build()))
                .addActionRows(ActionRow.of(TextInput.create("comments", "Further comments", TextInputStyle.PARAGRAPH).setPlaceholder("Comments").setRequired(false).build()))
                .build();
    }

    @Override
    public void onTextCommand(MessageReceivedEvent event) {
        var words = event.getMessage().getContentRaw().split(" ");
        if (words.length < 3) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Please use `!ban <User> [Duration] <Reason>`")).queue();
            return;
        }

        var targetMemberOptional = DiscordUtils.parseStringAsMember(event.getGuild(), words[1]);
        if (targetMemberOptional.isEmpty()) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Cannot ban, invalid user provided")).queue();
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
        var bannable = punisher.ban(event.getGuild(), targetMember, event.getMember(), reason, duration, DEFAULT_INTERVAL, "", EvidenceMessage.none());
        if (!bannable.success()) return;

        event.getChannel().sendMessageEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " was banned", "**Reason**: " + reason)).queue();
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        var targetMember = event.getOption("user").getAsMember();
        var reason = event.getOption("reason").getAsString();

        var durationOption = event.getOption("duration");
        var deletionIntervalOption = event.getOption("delete_messages");
        var commentsOption = event.getOption("comments");
        var evidenceOption = event.getOption("evidence");
        var duration = durationOption == null ? "" : durationOption.getAsString();
        var deletionInterval = deletionIntervalOption == null ? 0 : deletionIntervalOption.getAsInt();
        var comments = commentsOption == null ? "" : commentsOption.getAsString();
        var evidence = evidenceOption == null ? EvidenceMessage.none() : EvidenceMessage.ofEvidence(evidenceOption.getAsAttachment());

        // TODO: Error handling
        punisher.ban(event.getGuild(), targetMember, event.getMember(), reason, duration, deletionInterval, comments, evidence);

        event.replyEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " banned", "**Reason**: " + reason)).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        event.deferReply(true).queue();

        try {
            event.getGuild().retrieveMemberById(event.getModalId().split(":")[1]).queue(targetUser -> {
                var channel = event.getChannel();
                channel.retrieveMessageById(event.getModalId().split(":")[2]).queue(message -> {
                    var reason = event.getInteraction().getValue("reason").getAsString();
                    var duration = event.getInteraction().getValue("duration").getAsString();
                    var comments = event.getInteraction().getValue("comments").getAsString();

                    var bannable = punisher.ban(event.getGuild(), targetUser, event.getMember(), reason, duration, DEFAULT_INTERVAL, comments, new EvidenceMessage(message.getContentRaw(), message.getAttachments()));
                    if (!bannable.success()) return;

                    event.getHook().editOriginalEmbeds(EmbedUtils.success(targetUser.getUser().getAsTag() + " was banned. **Reason**: " + reason)).queue();
                });
            });
        } finally {
            // TODO: Replace
            if (!event.isAcknowledged()) {
                event.replyEmbeds(EmbedUtils.error("An error occurred, the user was **not** banned!")).setEphemeral(true).queue();
            }
        }
    }
}
