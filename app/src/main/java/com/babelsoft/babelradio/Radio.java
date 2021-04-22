package com.babelsoft.babelradio;

public class Radio {
    private int radioInternalId;
    private int radioId;
    private String radioName;
    private String radioTag;
    private int radioImage;
    private String radioStream;
    private String radioBitrate = "128kb";
    private String radioArtist = "Artist";
    private String radioTitle = "Title";

    public Radio(int radioInternalId, int radioId, String radioName, String radioTag,
                 int radioImage, String radioStream) {
        this.radioInternalId = radioInternalId;
        this.radioId = radioId;
        this.radioName = radioName;
        this.radioTag = radioTag;
        this.radioImage = radioImage;
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

    void setRadioImage(int radioImage) {
        this.radioImage = radioImage;
    }

    void setRadioArtist(String radioArtist) {
        this.radioArtist = radioArtist;
    }

    void setRadioTitle(String radioTitle) {
        this.radioTitle = radioTitle;
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

    int getRadioImage() {
        return radioImage;
    }

    String getRadioArtist() {
        return radioArtist;
    }

    String getRadioTitle() {
        return radioTitle;
    }
}
