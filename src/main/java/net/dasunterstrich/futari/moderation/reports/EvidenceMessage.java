package net.dasunterstrich.futari.moderation.reports;

import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record EvidenceMessage(@Nullable String messageContent, List<Message.Attachment> messageAttachments) {
    public static EvidenceMessage none() {
        return new EvidenceMessage(null, List.of());
    }

    public static EvidenceMessage ofEvidence(Message.Attachment attachment) {
        return new EvidenceMessage(null, List.of(attachment));
    }
}
