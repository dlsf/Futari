package net.dasunterstrich.futari.moderation;

public record PunishmentResponse(boolean success, boolean contactedUser) {
    public static PunishmentResponse failed() {
        return new PunishmentResponse(false, false);
    }
}
