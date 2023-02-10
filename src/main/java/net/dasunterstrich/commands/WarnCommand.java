package net.dasunterstrich.commands;

import net.dasunterstrich.commands.internal.BotCommand;
import net.dasunterstrich.moderation.Punisher;
import net.dasunterstrich.moderation.ReportedMessage;
import net.dasunterstrich.utils.DiscordUtils;
import net.dasunterstrich.utils.EmbedUtils;
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

public class WarnCommand extends BotCommand {
    private final Punisher punisher;

    public WarnCommand(Punisher punisher) {
        super("warn", "Warn user", Permission.BAN_MEMBERS);

        this.punisher = punisher;
    }

    @Nullable
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
    public CommandData getModalCommandData() {
        return Commands.context(Command.Type.MESSAGE, "warn_modal")
                .setName(getInteractionMenuName())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS));
    }

    @Nullable
    @Override
    public Modal buildModal(MessageContextInteractionEvent event) {
        var message = event.getTarget();
        var author = message.getAuthor();

        return Modal.create("warn:" + author.getId() + ":" + message.getId(), "Warn " + author.getAsTag())
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

        // TODO: Error handling
        var warnable = punisher.warn(event.getGuild(), targetMember, event.getMember(), reason, "", ReportedMessage.none());

        event.getChannel().sendMessageEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " warned", "**Reason**: " + reason)).queue();
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        var targetMember = event.getOption("user").getAsMember();
        var reason = event.getOption("reason").getAsString();

        var commentsOption = event.getOption("comments");
        var evidenceOption = event.getOption("evidence");
        var comments = commentsOption == null ? "" : commentsOption.getAsString();
        var evidence = evidenceOption == null ? ReportedMessage.none() : ReportedMessage.ofEvidence(evidenceOption.getAsAttachment());

        // TODO: Error handling
        punisher.warn(event.getGuild(), targetMember, event.getMember(), reason, comments, evidence);

        event.replyEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " warned", "**Reason**: " + reason)).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        try {
            var targetUser = event.getGuild().retrieveMemberById(event.getModalId().split(":")[1]).complete();
            var channel = event.getChannel();
            var message = channel.retrieveMessageById(event.getModalId().split(":")[2]).complete();

            var reason = event.getInteraction().getValue("reason").getAsString();
            var comments = event.getInteraction().getValue("comments").getAsString();

            var warnable = punisher.warn(event.getGuild(), targetUser, event.getMember(), reason, comments, new ReportedMessage(message.getContentRaw(), message.getAttachments()));
            if (!warnable) return;

            event.replyEmbeds(EmbedUtils.success(targetUser.getUser().getAsTag() + " was warned. Reason: " + reason)).setEphemeral(true).queue();
        } finally {
            if (!event.isAcknowledged()) {
                event.replyEmbeds(EmbedUtils.error("An error occurred, the user was **not** warned!")).setEphemeral(true).queue();
            }
        }
    }
}
