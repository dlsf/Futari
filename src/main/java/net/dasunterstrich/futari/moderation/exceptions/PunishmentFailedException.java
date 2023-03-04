package net.dasunterstrich.futari.moderation.exceptions;

public class PunishmentFailedException extends RuntimeException {
    public PunishmentFailedException(String message) {
        super(message);
    }

    public PunishmentFailedException(Throwable cause) {
        super(cause);
    }
}
