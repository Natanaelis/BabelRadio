package com.babelsoft.babelradio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

//import wseemann.media.FFmpegMediaMetadataRetriever;

public class PlayerService extends Service implements IMetadataAsyncResponse{
    public static String artistText;
    public static String titleText;
    public static String channelName;
    public static int channelImage;
    public static PlayerStatus playerStatus = PlayerStatus.READY;
    private int currentChannelTableNumber;
    private int currentVolume, startupVolume;
    private PlayerStatus playerPreviousStatus = PlayerStatus.READY;
    public static ArrayList<RadioChannel> radioChannels = new ArrayList<RadioChannel>();
    private MediaPlayer mp = null;
    private AudioManager am;
    private MediaSession ms;
    private BroadcastReceiver controlReceiver;
    private PendingIntent resultPendingIntent;
    private SharedPreferences preferences;
    private RemoteViews notificationView;
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;
    private String displayText = null;
    private DisplayMode mode;
    private Timer updateA2dpDisplayTimer;
    private Timer downloadMetaDataTimer;
    private CountDownTimer reBufferingTimer;
    private RadioChannel radioZlotePrzeboje, radioZET, rmfFM, smoothJazz, p7klem; // TODO It will be moved to server
    private Notification.Builder builder;
    private final int notificationId = 78;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        loadSettings();
        initiateNotification();
        showNotification();
        initializeMediaPlayer();
        initializeControlReceiver();
        initializeAudioChangeFocus();
        initializeMediaButtons();
        registerChannels();
        resetArtistTitle();
        setChannelNameIcon();
        playerStatusChanged(PlayerStatus.READY);
        rememberStartupVolume();
        autoPlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }

    private void initiateNotification() {
        Intent activityIntent = new Intent(this, BabelRadioApp.class);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(getString(R.string.app_name),
                getResources().getString(R.string.notification_channel_name), importance);
        notificationManager.createNotificationChannel(mChannel);

        resultPendingIntent = PendingIntent.getActivity(this, 0,
                activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notificationView = new RemoteViews(getPackageName(), R.layout.notification);

        setNotificationButtonsListener(notificationView);
    }

    private void showNotification() {
        builder = new Notification.Builder(this, getString(R.string.app_name))
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.icons8_radio_tower_noti)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setContentIntent(resultPendingIntent)
                .setCustomContentView(notificationView)
                .setAutoCancel(false)
                .setOngoing(true);

        startForeground(notificationId, builder.build());
    }

    private void initializeMediaPlayer() {
        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (am.isBluetoothA2dpOn()) {
            setVolumeMax();
        }
    }

    private void initializeMediaButtons() {
        playDummyAudio();

        ms = new MediaSession(this, "MyMediaSession");
        ms.setCallback(new MediaSession.Callback() {

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (event.getKeyCode()) {
                        case KeyEvent.KEYCODE_MEDIA_STOP:
                            sendBroadcast(new Intent(ControlAction.STOP.name()));
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            sendBroadcast(new Intent(ControlAction.PAUSE.name()));
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            sendBroadcast(new Intent(ControlAction.PLAY.name()));
                            break;
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            sendBroadcast(new Intent(ControlAction.NEXT.name()));
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            sendBroadcast(new Intent(ControlAction.PREVIOUS.name()));
                            break;
                    }
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }
        });

        ms.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        ms.setActive(true);

        playDummyAudio();
    }

    private void initializeControlReceiver() {
        controlReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ControlAction action = ControlAction.valueOf(intent.getAction());
                switch (action) {
                    case STOP:
                        onStopClick();
                        break;
                    case PLAY:
                        onPlayClick();
                        break;
                    case PLAY_STOP:
                        if (playerStatus == PlayerStatus.READY) onPlayClick();
                        else onStopClick();
                        break;
                    case PAUSE:
                        onPauseClick();
                        break;
                    case NEXT:
                        onNextClick();
                        break;
                    case PREVIOUS:
                        onPreviousClick();
                        break;
                    case CLOSE_PLAYER_SERVICE:
                        String source = intent.getStringExtra("Source");
                        switch (source) {
                            case "BabelRadioApp":
                                if (playerStatus == PlayerStatus.READY) {
                                    stopSelf();
                                }
                                break;
                            case "BootService":
                                ProcessControl pc = new ProcessControl();
                                if (pc.isActivityRunning(BabelRadioApp.class, getApplicationContext())) {
                                    setVolume(startupVolume);
                                    onStopClick();
                                }
                                else stopSelf();
                                break;
                            case "Notification":
                                stopSelf();
                                break;
                        }
                        break;
                    case REQUEST_SCREEN_UPDATE:
                        updateScreenStatus();
                        updateScreenArtistTitle();
                        break;
                }
            }
        };

        IntentFilter controlsFilter = new IntentFilter();
        controlsFilter.addAction(ControlAction.STOP.name());
        controlsFilter.addAction(ControlAction.PLAY.name());
        controlsFilter.addAction(ControlAction.PLAY_STOP.name());
        controlsFilter.addAction(ControlAction.PAUSE.name());
        controlsFilter.addAction(ControlAction.NEXT.name());
        controlsFilter.addAction(ControlAction.PREVIOUS.name());
        controlsFilter.addAction(ControlAction.CLOSE_PLAYER_SERVICE.name());
        controlsFilter.addAction(ControlAction.REQUEST_SCREEN_UPDATE.name());

        registerReceiver(controlReceiver, controlsFilter);
    }

    private void initializeAudioChangeFocus() {
        mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        setVolume(currentVolume);
                        if (playerStatus != PlayerStatus.READY) playerStatusChanged(playerPreviousStatus);
                        if (playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.BUFFERING) playRadio();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        rememberCurrentVolume();
                        playerStatusChanged(PlayerStatus.READY);
                        stopPlay();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        rememberCurrentVolume();
                        playerStatusChanged(PlayerStatus.INTERRUPTED_PAUSE);
                        stopPlay();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        rememberCurrentVolume();
                        setVolumeHalf();
                        break;
                }
            }
        };
    }

    private void setNotificationButtonsListener(RemoteViews view) {
        //listener 1
        Intent nextButtonIntent = new Intent(ControlAction.NEXT.name());
        PendingIntent nextButtonPendingIntent = PendingIntent.getBroadcast(this, 1, nextButtonIntent, 0);
        view.setOnClickPendingIntent(R.id.next_button, nextButtonPendingIntent);

        //listener 2
        Intent playStopButtonIntent = new Intent(ControlAction.PLAY_STOP.name());
        PendingIntent playStopButtonPendingIntent = PendingIntent.getBroadcast(this, 1, playStopButtonIntent, 0);
        view.setOnClickPendingIntent(R.id.play_stop_button, playStopButtonPendingIntent);

        //listener 3
        Intent closeButtonIntent = new Intent(ControlAction.CLOSE_PLAYER_SERVICE.name());
        closeButtonIntent.putExtra("Source", "Notification");
        PendingIntent closeButtonPendingIntent = PendingIntent.getBroadcast(this, 0, closeButtonIntent, 0);
        view.setOnClickPendingIntent(R.id.close_button, closeButtonPendingIntent);

        //listener 4
        Intent previousButtonIntent = new Intent(ControlAction.PREVIOUS.name());
        PendingIntent previousButtonPendingIntent = PendingIntent.getBroadcast(this, 1, previousButtonIntent, 0);
        view.setOnClickPendingIntent(R.id.previous_button, previousButtonPendingIntent);
    }

    private void cancelNotification() {
        stopForeground(true);
    }

    public void updateNotificationArtistTitle() {
        notificationView.setTextViewText(R.id.artist_text, artistText);
        notificationView.setTextViewText(R.id.title_text, titleText);

        builder.setCustomContentView(notificationView);
        startForeground(notificationId, builder.build());
    }

    private void updateNotificationStatus() {
        notificationView.setTextViewText(R.id.channel_name_text, channelName);
        notificationView.setTextViewText(R.id.status_text, playerStatus.getText());
        notificationView.setImageViewResource(R.id.channel_icon, channelImage);

        if (playerStatus == PlayerStatus.READY) notificationView.setImageViewResource(R.id.play_stop_button, R.drawable.button_play);
        else notificationView.setImageViewResource(R.id.play_stop_button, R.drawable.button_stop);

        builder.setCustomContentView(notificationView);
        startForeground(notificationId, builder.build());
    }

    private void playDummyAudio() {
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
        at.play();
        at.stop();
        at.release();
    }

    private boolean requestAudioFocus() {
        int result = am.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void releaseAudioFocus(final Context context) {
        am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(mOnAudioFocusChangeListener);
    }

    private void sendTrackInfoToA2dp() {
        if (updateA2dpDisplayTimer != null) {
            updateA2dpDisplayTimer.cancel();
            updateA2dpDisplayTimer = null;
        }
        if (playerStatus != PlayerStatus.PLAYING) {
            displayText = channelName + " (" + playerStatus.getText() + ")";
            updateA2dpDisplay(displayText);
        }
        else {
            updateA2dpDisplayTimer = new Timer();
            mode = DisplayMode.PLAYING;
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    switch (mode) {
                        case PLAYING:
                            displayText = channelName + " (" + playerStatus.getText() + ")";
                            mode = DisplayMode.ARTIST;
                            break;
                        case ARTIST:
                            displayText = artistText;
                            mode = DisplayMode.TITLE;
                            break;
                        case TITLE:
                            displayText = titleText;
                            mode = DisplayMode.CATEGORY;
                            break;
                        case CATEGORY:
                            displayText = "Category: " + radioChannels.get(currentChannelTableNumber).getChannelDescription();
                            mode = DisplayMode.BITRATE;
                            break;
                        case BITRATE:
                            displayText = "Bitrate: " + radioChannels.get(currentChannelTableNumber).getChannelBitrate();
                            mode = DisplayMode.PLAYING;
                            break;
                    }
                    updateA2dpDisplay(displayText);
                }
            };
            updateA2dpDisplayTimer.scheduleAtFixedRate(tt, 0, Integer.parseInt(preferences.getString(Settings.A2DP_DISPLAY_UPDATE_TIME.name(), "8000")));
        }
    }

    private void downloadMetaData() {
        stopDownloadMetaData();

        downloadMetaDataTimer = new Timer();
        TimerTask mt = new TimerTask() {
            @Override
            public void run() {
                MetadataTask mt = new MetadataTask();
                mt.delegate = PlayerService.this;
                try {
                    mt.execute(new URL(radioChannels.get(currentChannelTableNumber).getChannelURL()));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        };
        downloadMetaDataTimer.scheduleAtFixedRate(mt, 0,
                Integer.parseInt(preferences.getString(Settings.METADATA_REFRESH_TIME.name(),"12000")));
    }

    private void updateA2dpDisplay(final String text) {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, text)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, -1)
                .build();

        ms.setMetadata(metadata);

        PlaybackState state = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY)
                .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f, SystemClock.elapsedRealtime())
                .build();

                ms.setPlaybackState(state);
    }

    //TODO COMBINE updateStatuses
    private void updateScreenStatus() {
        Intent updateUpdateScreenStatusIntent = new Intent();
        updateUpdateScreenStatusIntent.setAction(ControlAction.UPDATE_SCREEN_STATUS.name());
        sendBroadcast(updateUpdateScreenStatusIntent);
    }

    private void updateScreenArtistTitle() {
        Intent updateUpdateScreenArtistTitleIntent = new Intent();
        updateUpdateScreenArtistTitleIntent.setAction(ControlAction.UPDATE_SCREEN_ARTIST_TITLE.name());
        sendBroadcast(updateUpdateScreenArtistTitleIntent);
    }

    private void rememberCurrentVolume() {
        currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void rememberStartupVolume() {
        startupVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void setVolume(int volume) {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    private void setVolumeMax() {
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    }

    private void setVolumeHalf() {
        am.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume / 2, 0);
    }

    private void playRadio() {
        stopPlay();

        if (requestAudioFocus()) {
            playMusic();
        }
    }

    private void onPlayClick() {
        if (playerStatus == PlayerStatus.READY) {
            playBeep(Beep.BUFFERING);
            playRadio();
        }
    }

    private void onStopClick() {
        if (playerStatus != PlayerStatus.READY) {
            playBeep(Beep.STOP);
            resetArtistTitle();
            playerStatusChanged(PlayerStatus.READY);
            stopPlay();
        }
    }

    private void onPauseClick() {
        if (playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.BUFFERING) onStopClick();
        else if (playerStatus == PlayerStatus.READY) onPlayClick();
    }

    private void onPreviousClick() {
        playBeep(Beep.PREVIOUS);
        if (playerStatus != PlayerStatus.INTERRUPTED_PAUSE) {
            currentChannelTableNumber--;
            if (currentChannelTableNumber < 0 ) {
                currentChannelTableNumber = radioChannels.size() - 1;
            }
            rememberCurrentChannelTableNumber();
            setChannelNameIcon();
        }
        if (playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.BUFFERING) {
            playRadio();
        } else if (playerStatus == PlayerStatus.READY){
            playerStatusChanged(PlayerStatus.READY);
        }
        resetArtistTitle();
        updateScreenStatus();
        updateScreenArtistTitle();
        updateNotificationStatus();
    }

    private void onNextClick() {
        playBeep(Beep.NEXT);
        if (playerStatus != PlayerStatus.INTERRUPTED_PAUSE) {
            currentChannelTableNumber++;
            if (currentChannelTableNumber > radioChannels.size() - 1 ) {
                currentChannelTableNumber = 0;
            }
            rememberCurrentChannelTableNumber();
            setChannelNameIcon();
        }
        if (playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.BUFFERING) {
            playRadio();
        } else if (playerStatus == PlayerStatus.READY){
            playerStatusChanged(PlayerStatus.READY);
        }
        resetArtistTitle();
        updateScreenStatus();
        updateScreenArtistTitle();
        updateNotificationStatus();
    }

    private void setChannelNameIcon() {
        channelName = radioChannels.get(currentChannelTableNumber).getChannelName();
        channelImage = radioChannels.get(currentChannelTableNumber).getChannelImage();
    }

    private void rememberCurrentChannelTableNumber() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(Settings.CURRENT_CHANNEL_TABLE_NUMBER.name(), currentChannelTableNumber);
        editor.commit();
    }

    private void reBufferingCountDown() {
        stopBufferingCountDown();
        int reBufferingTime = Integer.parseInt(preferences.getString(Settings.REBUFFERING_DELAY_TIME.name(), "8000"));
        reBufferingTimer = new CountDownTimer(reBufferingTime, reBufferingTime) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                playBeep(Beep.BUFFERING);
                playRadio();
            }
        }.start();
    }

    private void stopDownloadMetaData() {
        if (downloadMetaDataTimer != null) {
            downloadMetaDataTimer.cancel();
            downloadMetaDataTimer = null;
        }
    }

    private void stopBufferingCountDown() {
        if (reBufferingTimer != null) {
            reBufferingTimer.cancel();
            reBufferingTimer = null;
        }
    }

    // TODO To be moved to server
    private void registerChannels() {
        radioZlotePrzeboje = new RadioChannel();
        radioZlotePrzeboje.setChannelNumber(1);
        radioZlotePrzeboje.setChannelName("Radio Złote Przeboje");
        radioZlotePrzeboje.setChannelURL("http://gdansk1-1.radio.pionier.net.pl:8000/pl/tuba9-1.mp3");
        radioZlotePrzeboje.setChannelBitrate("128kb/s");
        radioZlotePrzeboje.setChannelDescription("Oldies");
        radioZlotePrzeboje.setChannelImage(R.drawable.logo_radio_zlote_przeboje);
        radioZET = new RadioChannel();
        radioZET.setChannelNumber(2);
        radioZET.setChannelName("Radio ZET");
        radioZET.setChannelURL("https://zt.cdn.eurozet.pl/zet-net.mp3");
        radioZET.setChannelBitrate("128kb/s");
        radioZET.setChannelDescription("Adult Contemporary");
        radioZET.setChannelImage(R.drawable.logo_radio_zet);
        rmfFM = new RadioChannel();
        rmfFM.setChannelNumber(3);
        rmfFM.setChannelName("RMF FM");
        rmfFM.setChannelURL("http://31.192.216.8:8000/rmf_fm");
        rmfFM.setChannelBitrate("128kb/s");
        rmfFM.setChannelDescription("Adult Contemporary");
        rmfFM.setChannelImage(R.drawable.logo_rmf);
        smoothJazz = new RadioChannel();
        smoothJazz.setChannelNumber(4);
        smoothJazz.setChannelName("Smooth Jazz");
        smoothJazz.setChannelURL("http://79.143.187.96:8090/128stream");
        smoothJazz.setChannelBitrate("128kb/s");
        smoothJazz.setChannelDescription("Smooth Jazz");
        smoothJazz.setChannelImage(R.drawable.logo_smooth_jazz);
        p7klem = new RadioChannel();
        p7klem.setChannelNumber(5);
        p7klem.setChannelName("P7 Klem");
        p7klem.setChannelURL("https://p7.p4groupaudio.com/P07_MM");
        p7klem.setChannelBitrate("128kb/s");
        p7klem.setChannelDescription("Oldies");
        p7klem.setChannelImage(R.drawable.logo_p7_klem);
//        radioChannels = new RadioChannel[]{radioZlotePrzeboje, radioZET, rmfFM, smoothJazz, p7klem};
        radioChannels.add(radioZlotePrzeboje);
        radioChannels.add(radioZET);
        radioChannels.add(rmfFM);
        radioChannels.add(smoothJazz);
        radioChannels.add(p7klem);
    }

    private void playBeep(Beep beep) {
        MediaPlayer mpBeep;
        if (beep == Beep.BUFFERING) mpBeep = MediaPlayer.create(this, R.raw.beep_low_3x);
        else if (beep == Beep.NEXT) mpBeep = MediaPlayer.create(this, R.raw.beep_low_high);
        else if (beep == Beep.PREVIOUS) mpBeep = MediaPlayer.create(this, R.raw.beep_high_low);
        else mpBeep = MediaPlayer.create(this, R.raw.beep_low);

        mpBeep.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.stop();
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
            }
        });
        mpBeep.start();
    }

    private void playerStatusChanged(PlayerStatus newStatus) {
        if (playerStatus != newStatus) {
            playerPreviousStatus = playerStatus;
            playerStatus = newStatus;

            updateScreenStatus();
            updateNotificationStatus();
            if (am.isBluetoothA2dpOn()) sendTrackInfoToA2dp();
        }
    }

    private void stopPlay() {
        stopBufferingCountDown();
        stopDownloadMetaData();

        if (mp != null) {
            if (mp.isPlaying()) mp.stop();
            mp.reset();
            mp.release();
            mp = null;
        }
    }

    private void loadSettings() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
//        currentChannelTableNumber = preferences.getInt(Settings.CURRENT_CHANNEL_TABLE_NUMBER.name(), 0);
        currentChannelTableNumber = 0;
    }

    private void autoPlay() {
        if (preferences.getBoolean(Settings.RUN_SERVICE_AFTER_A2DP_CONNECTED.name(), true) &&
            preferences.getBoolean(Settings.AUTO_PLAY_AFTER_A2DP_CONNECTED.name(), false)) playRadio();
    }
    
    private void resetArtistTitle() {
        artistText = "Artist";
        titleText = "Title";
    }

    private void playMusic() {
        playerStatusChanged(PlayerStatus.BUFFERING);
        reBufferingCountDown();

        try {
            mp = new MediaPlayer();
            mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mp.setDataSource(radioChannels.get(currentChannelTableNumber).getChannelURL());
            mp.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }

        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                if (playerStatus != PlayerStatus.INTERRUPTED_PAUSE) {
                    stopBufferingCountDown();
                    mp.start();
                    playerStatusChanged(PlayerStatus.PLAYING);
                    downloadMetaData();
                }
            }
        });

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                playBeep(Beep.BUFFERING);
                playRadio();
            }
        });

        mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer, int info, int extra) {
                if (info == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    playBeep(Beep.BUFFERING);
                    playRadio();
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        playerStatusChanged(PlayerStatus.READY);
        updateScreenStatus();
        updateNotificationStatus();
        stopPlay();
        setVolume(startupVolume);
        cancelNotification();
        releaseAudioFocus(this);
        ms.release();
        unregisterReceiver(controlReceiver);
        super.onDestroy();
    }

    @Override
    public void metadataResult(String asyncResult) {
        Metadata m = new Metadata(asyncResult);
        if (!m.getArtist().equals(artistText) && !m.getTitle().equals(titleText)) {
            artistText = m.getArtist();
            titleText = m.getTitle();
            updateNotificationArtistTitle();
            updateScreenArtistTitle();
        }
    }
}
