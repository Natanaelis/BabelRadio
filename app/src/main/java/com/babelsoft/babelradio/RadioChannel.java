package com.babelsoft.babelradio;

public class RadioChannel {
    private int channelNumber;
    private String channelName;
    private String channelURL;
    private String channelBitrate;
    private String channelDescription;
    private int channelImage;

    void setChannelNumber(int mChannelNumber) {
        channelNumber = mChannelNumber;
    }

    void setChannelName (String mChannelName) {
        channelName = mChannelName;
    }

    void setChannelURL (String mChannelURL) {
        channelURL = mChannelURL;
    }

    void setChannelBitrate (String mChannelBitrate) {
        channelBitrate = mChannelBitrate;
    }

    void setChannelDescription (String mChannelDescription) {
        channelDescription = mChannelDescription;
    }

    void setChannelImage(int mchannelImage) {
        channelImage = mchannelImage;

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
