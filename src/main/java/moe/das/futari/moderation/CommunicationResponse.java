package moe.das.futari.moderation;

public enum CommunicationResponse {
    SUCCESS,
    FAILURE,
    NONE;

    public boolean isFailure() {
        return this == CommunicationResponse.FAILURE;
    }
}
