package moe.das.futari.commands.internal;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.*;

public class CommandManager extends ListenerAdapter {
    private final Set<BotCommand> commands = new LinkedHashSet<>();

    public void addCommand(BotCommand command) {
        commands.add(command);
    }

    public Collection<CommandData> registeredCommandData() {
        var set = new LinkedHashSet<CommandData>();

        for (var command : commands) {
            var commandData = command.getCommandData().setDefaultPermissions(DefaultMemberPermissions.enabledFor(command.getPermission()));
            set.add(commandData);
            set.add(command.getModalCommandData(Command.Type.MESSAGE));
            set.add(command.getModalCommandData(Command.Type.USER));
        }

        set.removeIf(Objects::isNull);
        return set;
    }

    public Set<BotCommand> getCommands() {
        return new LinkedHashSet<>(commands);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) return;

        var executedCommand = commands.stream().filter(command -> event.getFullCommandName().equalsIgnoreCase(command.getName())).findAny();
        if (executedCommand.isEmpty()) return;

        if (executedCommand.get().getPermission() != null && !event.getMember().hasPermission(executedCommand.get().getPermission())) return;

        executedCommand.get().onSlashCommand(event);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getChannelType().isGuild() || event.isWebhookMessage()) return;

        var executedCommand = commands.stream().filter(command -> ("!" + command.getName()).equalsIgnoreCase(event.getMessage().getContentRaw().split(" ")[0])).findAny();
        if (executedCommand.isEmpty()) return;

        if (executedCommand.get().getPermission() != null && !event.getMember().hasPermission(executedCommand.get().getPermission())) return;

        executedCommand.get().onTextCommand(event);
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        if (!event.isFromGuild()) return;

        var executedCommand = commands.stream().filter(command -> event.getName().equals(command.getInteractionMenuName())).findAny();
        if (executedCommand.isEmpty()) return;

        if (executedCommand.get().getPermission() != null && !event.getMember().hasPermission(executedCommand.get().getPermission())) return;

        var message = event.getTarget();
        event.replyModal(executedCommand.get().buildModal(message.getAuthor(), Optional.of(message))).queue();
    }

    @Override
    public void onUserContextInteraction(UserContextInteractionEvent event) {
        if (!event.isFromGuild()) return;

        var executedCommand = commands.stream().filter(command -> event.getName().equals(command.getInteractionMenuName())).findAny();
        if (executedCommand.isEmpty()) return;

        if (executedCommand.get().getPermission() != null && !event.getMember().hasPermission(executedCommand.get().getPermission())) return;

        event.replyModal(executedCommand.get().buildModal(event.getTarget(), Optional.empty())).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.isFromGuild()) return;

        var executedCommand = commands.stream().filter(command -> event.getModalId().startsWith(command.getName() + ":")).findAny();
        if (executedCommand.isEmpty()) return;

        if (executedCommand.get().getPermission() != null && !event.getMember().hasPermission(executedCommand.get().getPermission())) return;

        executedCommand.get().onModalInteraction(event);
    }
}
