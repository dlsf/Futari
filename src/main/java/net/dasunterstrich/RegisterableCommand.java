package net.dasunterstrich;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public enum RegisterableCommand {

    BAN(
            Commands.slash("ban", "Ban a user")
                    .addOption(OptionType.USER, "user", "The user to ban", true)
                    .addOption(OptionType.STRING, "reason", "Reason for the ban", true)
                    .addOption(OptionType.STRING, "duration", "Duration of the ban", false)
                    .addOption(OptionType.STRING, "comments", "Further comments for other moderators", false)
                    .addOption(OptionType.ATTACHMENT, "evidence", "Further comments for other moderators", false)
    ),
    BAN_MODAL(
            Commands.context(Command.Type.MESSAGE, "ban_modal")
                    .setName("Ban user")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
    );

    private CommandData commandData;

    RegisterableCommand(CommandData commandData) {
        this.commandData = commandData;
    }

    public CommandData getCommandData() {
        return commandData;
    }

}
