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

public class BanCommand extends BotCommand {
    public BanCommand() {
        super("ban", "Ban user");
    }

    @Nullable
    @Override
    public CommandData getCommandData() {
        return Commands.slash("ban", "Ban a user")
                .addOption(OptionType.USER, "user", "The user to ban", true)
                .addOption(OptionType.STRING, "reason", "Reason for the ban", true)
                .addOption(OptionType.STRING, "duration", "Duration of the ban", false)
                .addOption(OptionType.STRING, "comments", "Further comments for other moderators", false)
                .addOption(OptionType.ATTACHMENT, "evidence", "Screenshot of additional evidence", false);
    }

    @Nullable
    @Override
    public CommandData getModalCommandData() {
        return Commands.context(Command.Type.USER, "ban_modal")
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
        var author = event.getAuthor();
        var content = event.getMessage().getContentRaw().split(" ");

        event.getChannel().sendMessage(content[1] + " was successfully banned!").queue();
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        var targetUser = event.getOption("user").getAsUser();
        event.reply(targetUser.getAsTag() + " was successfully banned! Case ID: #123").queue();
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
            var duration = event.getInteraction().getValue("duration").getAsString();
            var comments = event.getInteraction().getValue("comments").getAsString();

            // TODO
            // var bannable = Punishments.ban(targetUser, reason, TimeUtils.parseDuration());

            event.reply(targetUser.getAsTag() + " was successfully banned! Case ID: #123").setEphemeral(true).queue();
        } finally {
            if (!event.isAcknowledged()) {
                event.reply("An error occurred").setEphemeral(true).queue();
            }
        }
    }
}
