package net.dasunterstrich.futari.commands;

import net.dasunterstrich.futari.commands.internal.BotCommand;
import net.dasunterstrich.futari.moderation.Punisher;
import net.dasunterstrich.futari.reports.EvidenceMessage;
import net.dasunterstrich.futari.utils.DiscordUtils;
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

public class KickCommand extends BotCommand {
    private final Punisher punisher;

    public KickCommand(Punisher punisher) {
        super("kick", "Kick user", Permission.KICK_MEMBERS, "kick <User> <Reason>");

        this.punisher = punisher;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("kick", "Kick a user")
                .addOption(OptionType.USER, "user", "The user to kick", true)
                .addOption(OptionType.STRING, "reason", "Reason for the kick", true)
                .addOption(OptionType.STRING, "comments", "Further comments for other moderators", false)
                .addOption(OptionType.ATTACHMENT, "evidence", "Screenshot of additional evidence", false);
    }

    @Nullable
    @Override
    public CommandData getModalCommandData() {
        return Commands.context(Command.Type.MESSAGE, "kick_modal")
                .setName(getInteractionMenuName())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS));
    }

    @Nullable
    @Override
    public Modal buildModal(MessageContextInteractionEvent event) {
        var message = event.getTarget();
        var author = message.getAuthor();

        return Modal.create("kick:" + author.getId() + ":" + message.getId(), "Kick " + author.getAsTag())
                .addActionRows(ActionRow.of(TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH).setPlaceholder("Reason").setRequired(true).build()))
                .addActionRows(ActionRow.of(TextInput.create("comments", "Further comments", TextInputStyle.PARAGRAPH).setPlaceholder("Comments").setRequired(false).build()))
                .build();
    }

    @Override
    public void onTextCommand(MessageReceivedEvent event) {
        var words = event.getMessage().getContentRaw().split(" ");
        if (words.length < 3) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Please use `!kick <User> <Reason>`")).queue();
            return;
        }

        var targetMemberOptional = DiscordUtils.parseStringAsMember(event.getGuild(), words[1]);
        if (targetMemberOptional.isEmpty()) {
            event.getChannel().sendMessageEmbeds(EmbedUtils.error("Cannot kick, invalid user provided")).queue();
            return;
        }

        var targetMember = targetMemberOptional.get();
        var reason = String.join(" ", Arrays.copyOfRange(words, 2, words.length));

        // TODO: Error handling
        var kickable = punisher.kick(event.getGuild(), targetMember, event.getMember(), reason, "", EvidenceMessage.none());

        event.getChannel().sendMessageEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " kicked", "**Reason**: " + reason)).queue();
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        var targetMember = event.getOption("user").getAsMember();
        var reason = event.getOption("reason").getAsString();

        var commentsOption = event.getOption("comments");
        var evidenceOption = event.getOption("evidence");
        var comments = commentsOption == null ? "" : commentsOption.getAsString();
        var evidence = evidenceOption == null ? EvidenceMessage.none() : EvidenceMessage.ofEvidence(evidenceOption.getAsAttachment());

        // TODO: Error handling
        punisher.kick(event.getGuild(), targetMember, event.getMember(), reason, comments, evidence);

        event.replyEmbeds(EmbedUtils.success(targetMember.getUser().getAsTag() + " kicked", "**Reason**: " + reason)).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        event.deferReply(true).queue();

        try {
            event.getGuild().retrieveMemberById(event.getModalId().split(":")[1]).queue(targetUser -> {
                var channel = event.getChannel();
                channel.retrieveMessageById(event.getModalId().split(":")[2]).queue(message -> {
                    var reason = event.getInteraction().getValue("reason").getAsString();
                    var comments = event.getInteraction().getValue("comments").getAsString();

                    var kickable = punisher.kick(event.getGuild(), targetUser, event.getMember(), reason, comments, new EvidenceMessage(message.getContentRaw(), message.getAttachments()));
                    if (!kickable.success()) return;

                    event.getHook().editOriginalEmbeds(EmbedUtils.success(targetUser.getUser().getAsTag() + " was kicked. Reason: " + reason)).queue();
                });
            });

        } finally {
            if (!event.isAcknowledged()) {
                event.replyEmbeds(EmbedUtils.error("An error occurred, the user was **not** kicked!")).setEphemeral(true).queue();
            }
        }
    }
}
