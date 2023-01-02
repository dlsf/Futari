package net.dasunterstrich.listener;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getFullCommandName().equals("ban")) {
            return;
        }

        var targetUser = event.getOption("user").getAsUser();
        event.reply(targetUser.getAsTag() + " was successfully banned! Case ID: #123").setEphemeral(true).queue();
    }

}
