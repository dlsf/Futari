package moe.das.futari.listener;

import moe.das.futari.scheduler.MessageLogHandler;
import moe.das.futari.utils.DiscordUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class MessageDeletionListener extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final MessageLogHandler messageLogHandler;

    public MessageDeletionListener(MessageLogHandler messageLogHandler) {
        this.messageLogHandler = messageLogHandler;
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        var logChannel = event.getGuild().getTextChannelById(1082433497672716388L);
        messageLogHandler.getMessageByID(event.getMessageIdLong()).handleAsync((messageOptional, throwable) -> {
            if (throwable != null) return null;
            if (messageOptional.isEmpty()) return null;

            var jda = event.getJDA();
            var message = messageOptional.get();
            var channelName = event.getChannel().getName();
            var imageAttachments = message.attachments().stream().filter(DiscordUtils::isImageFileURL).toList();
            var nonImageAttachments = message.attachments().stream().filter(url -> !DiscordUtils.isImageFileURL(url)).toList();

            var messageCreateAction = logChannel.sendMessageEmbeds(buildEmbed(jda, message, channelName));
            imageAttachments.forEach(image -> messageCreateAction.addEmbeds(new EmbedBuilder().setImage(image).build()));
            messageCreateAction.setAllowedMentions(List.of(Message.MentionType.EMOJI, Message.MentionType.CHANNEL, Message.MentionType.SLASH_COMMAND, Message.MentionType.USER))
                    .setActionRow(Button.primary("deleted_message:" + event.getMessageId(), "Use as Evidence"))
                    .queue(deletionMessage -> {
                        var otherAttachments = DiscordUtils.getAttachedFilesFromUrls(nonImageAttachments);
                        deletionMessage.editMessageAttachments(otherAttachments).queue(null, attachmentUploadThrowable -> {});
                    }, deletionThrowable -> {
                        logger.warn("Could not send part of deleted message", deletionThrowable);
                    });

            return null;
        });
    }

    private MessageEmbed buildEmbed(JDA jda, MessageLogHandler.SimpleMessage message, String channelName) {
        var user = DiscordUtils.parseStringAsUser(jda, String.valueOf(message.userID()));
        var attachedFiles = message.attachments().isEmpty() ? "" : "\n\n**Attached Files**: \n" + message.attachments().stream()
                .map(DiscordUtils::getFileName)
                .collect(Collectors.joining("\n"));

        var embedBuilder = new EmbedBuilder()
                .setAuthor(user.map(User::getAsTag).orElse(null), null, user.map(User::getAvatarUrl).orElse(null))
                .setTitle("Message deleted in #" + channelName)
                .setDescription(message.content() + attachedFiles + "\n\n**Created at**: <t:" + message.creationTime() + ">")
                .setColor(Color.RED)
                .setFooter("User ID: " + message.userID())
                .setTimestamp(Instant.now());

        return embedBuilder.build();
    }
}
