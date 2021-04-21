package com.babelsoft.babelradio;

import java.io.UnsupportedEncodingException;

public class Metadata {
    private String artist = "";
    private String title = "";
    private String metadataInput;

    public Metadata(String metadataInput) {
        this.metadataInput = metadataInput;
        parse();
    }

    private void parse() {
        if (metadataInput.isEmpty() || metadataInput == "") {
            artist = "Artist";
            title = "Title";
        }
        else if (metadataInput.contains("-")) {
            split(metadataInput);
        }
        else if (metadataInput.contains("/")) {
            metadataInput = metadataInput.replace("/", "-");
            split(metadataInput);
        }
        else {
            artist = metadataInput;
            title = metadataInput;
        }
    }

    private void split(String input) {
        input = input.trim();
        String[] inputTable = input.split("-", -1);
        int separatorsCount = inputTable.length - 1;
        title = inputTable[separatorsCount];
        for (int i = 0; i <= separatorsCount - 1; i++) {
            if (i != 0) artist += "-";
            artist += inputTable[i];
        }
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }
}
