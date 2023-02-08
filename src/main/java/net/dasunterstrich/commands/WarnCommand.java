package net.dasunterstrich.commands;

import net.dasunterstrich.commands.internal.BotCommand;
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

public class WarnCommand extends BotCommand {
    public WarnCommand() {
        super("warn", "Warn user");
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
        var author = event.getAuthor();
        var content = event.getMessage().getContentRaw().split(" ");

        event.getChannel().sendMessage(content[1] + " was successfully warned!").queue();
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        var targetUser = event.getOption("user").getAsUser();
        event.reply(targetUser.getAsTag() + " was successfully warned! Case ID: #123").queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        try {
            var targetUser = event.getJDA().retrieveUserById(event.getModalId().split(":")[1]).complete();

            var channel = event.getChannel();
            var message = channel.retrieveMessageById(event.getModalId().split(":")[2]).complete();
            var messageContent = message.getContentRaw();
            var messageAttachments = message.getAttachments();

            var reason = event.getInteraction().getValue("reason").getAsString();
            var comments = event.getInteraction().getValue("comments").getAsString();

            // TODO
            // var bannable = Punishments.ban(targetUser, reason, TimeUtils.parseDuration());

            event.reply(targetUser.getAsTag() + " was successfully warned! Case ID: #123").setEphemeral(true).queue();
        } finally {
            if (!event.isAcknowledged()) {
                event.reply("An error occurred").setEphemeral(true).queue();
            }
        }
    }
}
