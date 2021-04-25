package com.babelsoft.babelradio;

import android.graphics.Bitmap;

public class Radio {
    private int radioInternalId;
    private int radioId;
    private String radioName;
    private String radioTag;
    private byte[] radioImage;
    private String radioStream;
    private String radioBitrate = "128kb";
    private String radioArtist = "Artist";
    private String radioTitle = "Title";

    public Radio(int radioInternalId, int radioId, String radioName, String radioTag,
                 byte[] radioImage, String radioStream) {
        this.radioInternalId = radioInternalId;
        this.radioId = radioId;
        this.radioName = radioName;
        this.radioTag = radioTag;
        this.radioImage = radioImage;
        this.radioStream = radioStream;
    }

    public void setRadioInternalId(int radioInternalId) {
        this.radioInternalId = radioInternalId;
    }

    public void setRadioId(int radioId) {
        this.radioId = radioId;
    }

    public void setRadioName(String radioName) {
        this.radioName = radioName;
    }

    public void setRadioStream(String radioStream) {
        this.radioStream = radioStream;
    }

    public void setRadioBitrate(String radioBitrate) {
        this.radioBitrate = radioBitrate;
    }

    public void setRadioTag(String radioTag) {
        this.radioTag = radioTag;
    }

    public void setRadioImage(byte[] radioImage) {
        this.radioImage = radioImage;
    }

    public void setRadioArtist(String radioArtist) {
        this.radioArtist = radioArtist;
    }

    public void setRadioTitle(String radioTitle) {
        this.radioTitle = radioTitle;
    }

    public int getRadioInternalId() {
        return radioInternalId;
    }

    public int getRadioId() {
        return radioId;
    }

    public String getRadioName() {
        return radioName;
    }

    public String getRadioStream() {
        return radioStream;
    }

    public String getRadioBitrate() {
        return radioBitrate;
    }

    public String getRadioTag() {
        return radioTag;
    }

    public byte[] getRadioImage() {
        return radioImage;
    }

    public String getRadioArtist() {
        return radioArtist;
    }

    public  String getRadioTitle() {
        return radioTitle;
    }
}
