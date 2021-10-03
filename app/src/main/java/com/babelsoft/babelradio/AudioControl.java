package com.babelsoft.babelradio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;

public class AudioControl {
    private AudioManager audioManager;
    private int currentVolume;
    private int startupVolume;
    private final Context context;

    public AudioControl(Context ctx) {
        context = ctx;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void readCurrentVolume() {
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public void setCurrentVolume() {
        setVolume(currentVolume);
    }

    public void readStartupVolume() {
        readCurrentVolume();
        startupVolume = currentVolume;
    }

    public void setStartupVolume() {
        setVolume(startupVolume);
    }

    public void setMaxVolume() {
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        setVolume(maxVolume);
    }

    public void setHalfVolume() {
        int halfVolume = currentVolume / 2;
        setVolume(halfVolume);
    }

    private void setVolume(int volume) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    public MediaPlayer initializeMediaPlayer() {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return mediaPlayer;
    }

    public boolean requestAudioFocus(AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener) {
        int result = audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    public void releaseAudioFocus(AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener) {
        audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(onAudioFocusChangeListener);
    }

    public boolean isBluetoothA2dpOn() {
        return audioManager.isBluetoothA2dpOn();
    }
}

