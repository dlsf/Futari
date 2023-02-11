package net.dasunterstrich.futari.moderation;

import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ReportedMessage(@Nullable String messageContent, List<Message.Attachment> messageAttachments) {
    public static ReportedMessage none() {
        return new ReportedMessage(null, List.of());
    }

    public static ReportedMessage ofEvidence(Message.Attachment attachment) {
        return new ReportedMessage(null, List.of(attachment));
    }
}
