package com.babelsoft.babelradio;

public enum PlayerStatus {
    READY("Ready"),
    BUFFERING("Buffering"),
    PLAYING("Playing"),
    INTERRUPTED_PAUSE("Paused");

    private final String text;

    PlayerStatus(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
