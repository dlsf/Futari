package net.dasunterstrich.listener;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getMessage().getContentRaw().startsWith("!ban")) {
            return;
        }

        if (!event.getChannelType().isGuild()) {
            return;
        }

        // TODO: Permissions check
        var author = event.getAuthor();
        var content = event.getMessage().getContentRaw().split(" ");

        event.getChannel().sendMessage(content[1] + " was successfully banned!").queue();
    }
}
