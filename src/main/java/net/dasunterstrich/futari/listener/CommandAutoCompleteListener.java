package net.dasunterstrich.futari.listener;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandAutoCompleteListener extends ListenerAdapter {
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getFullCommandName().equals("ban")) {
            return;
        }

        event.replyChoice("Don't Delete Any", 0)
                .addChoice("Previous Hour (Default)", 1)
                .addChoice("Previous 3 Hours", 3)
                .addChoice("Previous Day", 24)
                .addChoice("Previous 7 Days", 24 * 7)
                .queue();
    }
}
