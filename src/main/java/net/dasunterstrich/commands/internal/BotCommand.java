package net.dasunterstrich.commands.internal;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.Nullable;

public abstract class BotCommand {
    public abstract @Nullable CommandData getCommandData();
    public abstract @Nullable CommandData getModalCommandData();
    public abstract @Nullable Modal buildModal(MessageContextInteractionEvent event);
    public abstract void onTextCommand(MessageReceivedEvent event);
    public abstract void onSlashCommand(SlashCommandInteractionEvent event);
    public abstract void onModalInteraction(ModalInteractionEvent event);

    private final String name;
    private final @Nullable String interactionMenuName;

    public BotCommand(String name, @Nullable String interactionMenuName) {
        this.name = name;
        this.interactionMenuName = interactionMenuName;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getInteractionMenuName() {
        return interactionMenuName;
    }
}
