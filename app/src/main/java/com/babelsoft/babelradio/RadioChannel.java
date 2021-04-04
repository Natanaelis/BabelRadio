package com.babelsoft.babelradio;

public class RadioChannel {
    private int channelNumber;
    private String channelName;
    private String channelURL;
    private String channelBitrate;
    private String channelDescription;
    private int channelImage;

    void setChannelNumber(int channelNumber) {
        this.channelNumber = channelNumber;
    }

    void setChannelName (String channelName) {
        this.channelName = channelName;
    }

    void setChannelURL (String channelURL) {
        this.channelURL = channelURL;
    }

    void setChannelBitrate (String channelBitrate) {
        this.channelBitrate = channelBitrate;
    }

    void setChannelDescription (String channelDescription) {
        this.channelDescription = channelDescription;
    }

    void setChannelImage(int channelImage) {
        this.channelImage = channelImage;
    }

    int getChannelNumber() {
        return channelNumber;
    }

    String getChannelName() {
        return channelName;
    }

    String getChannelURL() {
        return channelURL;
    }

    String getChannelBitrate() {
        return channelBitrate;
    }

    String getChannelDescription() {
        return channelDescription;
    }

    int getChannelImage() {
        return channelImage;
    }
}
