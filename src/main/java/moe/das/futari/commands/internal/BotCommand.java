package moe.das.futari.commands.internal;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public abstract class BotCommand {
    private final String name;
    private final @Nullable String interactionMenuName;
    private final @Nullable Permission permission;
    private final String syntax;

    public BotCommand(String name, @Nullable String interactionMenuName, @Nullable Permission permission, String syntax) {
        this.name = name;
        this.interactionMenuName = interactionMenuName;
        this.permission = permission;
        this.syntax = syntax;
    }

    public BotCommand(String name, @Nullable Permission permission, String syntax) {
        this.name = name;
        this.interactionMenuName = null;
        this.permission = permission;
        this.syntax = syntax;
    }

    public abstract CommandData getCommandData();

    public abstract void onTextCommand(MessageReceivedEvent event);

    public abstract void onSlashCommand(SlashCommandInteractionEvent event);

    public @Nullable CommandData getModalCommandData(Command.Type type) {
        return null;
    }

    public @Nullable Modal buildModal(User user, Optional<Message> messageOptional) {
        return null;
    }

    public void onModalInteraction(ModalInteractionEvent event) {
        // Do nothing
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getInteractionMenuName() {
        return interactionMenuName;
    }

    @Nullable
    public Permission getPermission() {
        return permission;
    }

    public String getSyntax() {
        return syntax;
    }

    protected <T> T optionalOption(OptionMapping optionMapping, Function<OptionMapping, T> mapping, T defaultValue) {
        if (optionMapping == null) {
            return defaultValue;
        }

        return mapping.apply(optionMapping);
    }
}
