package com.babelsoft.babelradio;

public class Radio {
    private int radioInternalId;
    private int radioId;
    private String radioName;
    private String radioTag;
    private int radioLogo;
    private String radioStream;
    private String radioBitrate = "128kb";

    public Radio(int radioInternalId, int radioId, String radioName, String radioTag,
            int radioLogo, String radioStream) {
        this.radioInternalId = radioInternalId;
        this.radioId = radioId;
        this.radioName = radioName;
        this.radioTag = radioTag;
        this.radioLogo = radioLogo;
        this.radioStream = radioStream;
    }

    void setRadioInternalId(int radioInternalId) {
        this.radioInternalId = radioInternalId;
    }

    void setRadioId(int radioId) {
        this.radioId = radioId;
    }

    void setRadioName(String radioName) {
        this.radioName = radioName;
    }

    void setRadioStream(String radioStream) {
        this.radioStream = radioStream;
    }

    void setRadioBitrate(String radioBitrate) {
        this.radioBitrate = radioBitrate;
    }

    void setRadioTag(String radioTag) {
        this.radioTag = radioTag;
    }

    void setRadioLogo(int radioLogo) {
        this.radioLogo = radioLogo;
    }

    int getRadioInternalId() {
        return radioInternalId;
    }

    int getRadioId() {
        return radioId;
    }

    String getRadioName() {
        return radioName;
    }

    String getRadioStream() {
        return radioStream;
    }

    String getRadioBitrate() {
        return radioBitrate;
    }

    String getRadioTag() {
        return radioTag;
    }

    int getRadioLogo() {
        return radioLogo;
    }
}
