package net.dasunterstrich.futari.listener;

import net.dasunterstrich.futari.scheduler.MessageLogHandler;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageCreationListener extends ListenerAdapter {
    private final MessageLogHandler messageLogHandler;

    public MessageCreationListener(MessageLogHandler messageLogHandler) {
        this.messageLogHandler = messageLogHandler;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.isWebhookMessage() || event.getAuthor().isBot()) return;

        messageLogHandler.addMessage(event.getMessage());
    }
}
