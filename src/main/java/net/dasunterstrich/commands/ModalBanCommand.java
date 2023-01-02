package net.dasunterstrich.commands;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ModalBanCommand extends ListenerAdapter {

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().startsWith("ban:")) {
            return;
        }

        try {
            var targetUser = event.getJDA().retrieveUserById(event.getModalId().split(":")[1]).complete();

            var channel = event.getChannel();
            var message = channel.retrieveMessageById(event.getModalId().split(":")[2]).complete();
            var messageContent = message.getContentRaw();
            var messageAttachments = message.getAttachments();

            var reason = event.getInteraction().getValue("reason").getAsString();
            var duration = event.getInteraction().getValue("duration").getAsString();
            var comments = event.getInteraction().getValue("comments").getAsString();

            // TODO
            // var bannable = Punishments.ban(targetUser, reason, TimeUtils.parseDuration());

            event.reply(targetUser.getAsTag() + " was successfully banned! Case ID: #123").setEphemeral(true).queue();
        } finally {
            if (!event.isAcknowledged()) {
                event.reply("An error occurred").setEphemeral(true).queue();
            }
        }


    }

}
