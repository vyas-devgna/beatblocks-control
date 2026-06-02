package com.devgnav.beatblocks.spotify;

public final class BeatBlocksApiException extends RuntimeException {
    private final int status;
    private final String responseBody;

    public BeatBlocksApiException(int status, String message, String responseBody) {
        super(message);
        this.status = status;
        this.responseBody = responseBody;
    }

    public int status() {
        return status;
    }

    public String responseBody() {
        return responseBody;
    }
}
