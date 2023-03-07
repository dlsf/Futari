package net.dasunterstrich.futari.scheduler;

import net.dasunterstrich.futari.database.DatabaseHandler;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MessageLogHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final long CACHE_DURATION = Duration.ofMinutes(30).toMillis();
    private final DatabaseHandler databaseHandler;
    private final Set<SimpleMessage> messageHistory = new HashSet<>();

    public MessageLogHandler(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            addMessagesToDatabase();
            refreshMessageCache();
        }, 5, 5, TimeUnit.MINUTES);
    }

    public void addMessage(Message message) {
        messageHistory.add(SimpleMessage.fromDiscordMessage(message));
    }

    public CompletableFuture<Optional<SimpleMessage>> getMessageByID(long messageID) {
        return CompletableFuture.supplyAsync(() -> {
            var message = messageHistory.stream().filter(simpleMessage -> simpleMessage.messageID == messageID).findAny();
            if (message.isPresent()) return message;

            // Fetch message from database
            try (var connection = databaseHandler.getConnection(); var statement = connection.createStatement()) {
                var resultSet = statement.executeQuery("SELECT * FROM MessageHistory WHERE message_id = " + messageID);
                if (!resultSet.next()) throw new IllegalStateException("No message found in database");

                var userID = resultSet.getLong("user_id");
                var content = resultSet.getString("content");
                var creationTime = resultSet.getLong("creation_time");
                var attachmentString = resultSet.getString("attachments");
                var attachments = new ArrayList<String>();

                if (!attachmentString.isEmpty()) {
                    attachments.addAll(Arrays.asList(attachmentString.split("\\|")));
                }

                return Optional.of(new SimpleMessage(messageID, userID, content, attachments, creationTime));
            } catch (Exception exception) {
                logger.error("Failed to find message " + messageID, exception);
                return Optional.empty();
            }
        });
    }

    public void addMessagesToDatabase() {
        try (var connection = databaseHandler.getConnection()) {
            var statement = connection.prepareStatement("INSERT INTO MessageHistory (message_id, user_id, content, creationTime, attachments) VALUES (?, ?, ?, ?, ?)");

            for (SimpleMessage message : messageHistory) {
                // Not-so-atomic value, but I couldn't care less. It's not like they are super relevant, anyway
                var attachmentString = String.join("|", message.attachments);

                statement.setLong(1, message.messageID);
                statement.setLong(2, message.userID);
                statement.setString(3, message.content);
                statement.setLong(4, message.creationTime);
                statement.setString(5, attachmentString);
                statement.execute();
            }

            statement.close();
        } catch (Exception exception) {
            logger.error("Failed to save message logs into the database", exception);
        }
    }

    private void refreshMessageCache() {
        var deletionSet = new HashSet<SimpleMessage>();

        for (SimpleMessage message : messageHistory) {
            var timeDifference = System.currentTimeMillis() - message.creationTime;

            if (timeDifference > CACHE_DURATION) {
                deletionSet.add(message);
            }
        }

        messageHistory.removeAll(deletionSet);
    }

    public record SimpleMessage(long messageID, long userID, String content, List<String> attachments, long creationTime) {
        public static SimpleMessage fromDiscordMessage(Message message) {
            return new SimpleMessage(
                    message.getIdLong(),
                    message.getAuthor().getIdLong(),
                    message.getContentRaw(),
                    message.getAttachments().stream().map(Message.Attachment::getProxyUrl).toList(),
                    message.getTimeCreated().toEpochSecond()
            );
        }
    }
}
