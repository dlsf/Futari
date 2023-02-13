package net.dasunterstrich.futari.commands.internal;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.Nullable;

public abstract class BotCommand {
    public abstract CommandData getCommandData();
    public abstract void onTextCommand(MessageReceivedEvent event);
    public abstract void onSlashCommand(SlashCommandInteractionEvent event);

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

    public @Nullable CommandData getModalCommandData() {
        return null;
    }

    public @Nullable Modal buildModal(MessageContextInteractionEvent event) {
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
}
