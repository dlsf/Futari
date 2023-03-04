package net.dasunterstrich.futari.commands;

import net.dasunterstrich.futari.commands.internal.BotCommand;
import net.dasunterstrich.futari.commands.internal.CommandManager;
import net.dasunterstrich.futari.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class HelpCommand extends BotCommand {
    private final CommandManager commandManager;

    public HelpCommand(CommandManager commandManager) {
        super("help", Permission.BAN_MEMBERS, "help");

        this.commandManager = commandManager;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("help", "Shows a list of all commands");
    }

    @Override
    public void onTextCommand(MessageReceivedEvent event) {
        event.getChannel().sendMessageEmbeds(buildHelpEmbed()).queue();
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) {
        event.replyEmbeds(buildHelpEmbed()).queue();
    }

    private MessageEmbed buildHelpEmbed() {
        var stringBuilder = new StringBuilder("**Please use Slash commands or the Apps menu!**\n\n");
        commandManager.getCommands().stream()
                .map(BotCommand::getSyntax)
                .forEach(syntax -> stringBuilder.append("`").append("!").append(syntax).append("`").append("\n"));

        var body = stringBuilder.toString();
        return EmbedUtils.success("Help - Available Commands", body.substring(0, body.length() - 1));
    }
}
