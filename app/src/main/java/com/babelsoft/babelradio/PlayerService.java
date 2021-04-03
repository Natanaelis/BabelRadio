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
import android.view.KeyEvent;
import android.widget.RemoteViews;
import java.io.IOException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class PlayerService extends Service {
    private MediaPlayer mp = null;
    private AudioManager am;
    private BroadcastReceiver controlReceiver;
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;
    private int currentVolume, startupVolume;
    public static String artistText;
    public static String titleText;
    public static String channelName;
    public static int channelImage;
    public static PlayerStatus playerStatus = PlayerStatus.READY;
    private PlayerStatus playerPreviousStatus = PlayerStatus.READY;
    private String displayText = null;
    private Timer updateA2dpDisplayTimer;
    private Timer downloadMetaDataTimer;
    private static RadioChannel[] radioChannels;
    private static int currentChannelTableNumber = 0;
    private CountDownTimer reBufferingTimer;
    private RadioChannel radioZlotePrzeboje, radioZET, rmfFM, smoothJazz, p7klem;
    private MediaSession ms;
    private IcyStreamMeta streamMeta = new IcyStreamMeta();
    private String notificationChannelId = "Babel Radio";
    private String notificationChannelName = "Babel Radio Notification";
    private final static int NOTIFICATION_ID = 78;
    private RemoteViews notificationView;
    private PendingIntent resultPendingIntent;
    private SharedPreferences preferences;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        initiateNotification();

        showNotification();

//        cancelNotification();

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

        NotificationChannel mChannel = new NotificationChannel(
                notificationChannelId, notificationChannelName, importance);
        notificationManager.createNotificationChannel(mChannel);

        resultPendingIntent = PendingIntent.getActivity(this, 0,
                activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        notificationView = new RemoteViews(getPackageName(), R.layout.notification);

        setNotificationButtonsListener(notificationView);

    }

    private void showNotification() {

        Notification.Builder builder = new Notification.Builder(this, notificationChannelId)
                .setContentTitle("Babel Radio")
                .setContentText("Notification Text")
                .setSmallIcon(R.mipmap.icons8_radio_tower_noti)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setContentIntent(resultPendingIntent)
                .setCustomContentView(notificationView)
                .setAutoCancel(false)
                .setOngoing(true);

        Notification notification = builder.build();

        startForeground(NOTIFICATION_ID, notification);
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
                            sendBroadcast(new Intent(ControlAction.PAUSE.name()));
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            sendBroadcast(new Intent(ControlAction.PLAY.name()));
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            sendBroadcast(new Intent(ControlAction.PAUSE.name()));
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
                String action = intent.getAction();
                if (action == ControlAction.STOP.name()) onStopClick();
                else if (action == ControlAction.PLAY.name()) onPlayClick();
                else if (action == ControlAction.PLAY_STOP.name()) {
                    if (playerStatus == PlayerStatus.READY) onPlayClick();
                    else onStopClick();
                }
                else if (action == ControlAction.PAUSE.name()) onPauseClick();
                else if (action == ControlAction.NEXT.name()) onNextClick();
                else if (action == ControlAction.PREVIOUS.name()) onPreviousClick();
                else if (action == ControlAction.CLOSE_PLAYER_SERVICE.name()) {
                    String source = intent.getStringExtra("Source");
                    if (source.equals("BabelRadioApp")) {
                        if (playerStatus == PlayerStatus.READY) {
                            stopSelf();
                        }
                    }
                    else if (source.equals("BootService")) {
                        ProcessControl pc = new ProcessControl();
                        if (pc.isActivityRunning(BabelRadioApp.class, getApplicationContext())) {
                            setVolume(startupVolume);
                            onStopClick();
                        }
                        else stopSelf();
                    }
                    else if (source.equals("Notification")) {
                        stopSelf();
                    }
                }
                else if (action == ControlAction.UPDATE_SCREEN.name()) updateNotification(intent);
                else if (action == ControlAction.REQUEST_UPDATE_SCREEN.name()) updateScreen();
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
        controlsFilter.addAction(ControlAction.UPDATE_SCREEN.name());
        controlsFilter.addAction(ControlAction.REQUEST_UPDATE_SCREEN.name());

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

    private void updateNotification(Intent intent) {
        notificationView.setTextViewText(R.id.channel_name_text, channelName);
        notificationView.setTextViewText(R.id.artist_text, artistText);
        notificationView.setTextViewText(R.id.title_text, titleText);
        notificationView.setTextViewText(R.id.status_text, playerStatus.getText());
        notificationView.setImageViewResource(R.id.channel_icon, channelImage);

        if (playerStatus == PlayerStatus.READY) notificationView.setImageViewResource(R.id.play_stop_button, R.mipmap.button_play);
        else notificationView.setImageViewResource(R.id.play_stop_button, R.mipmap.button_stop);

        showNotification();
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
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return true;
        else return false;
    }

    private void releaseAudioFocus(final Context context) {
        am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(mOnAudioFocusChangeListener);
    }

    private void sendTrackInfoToA2dp() {
        final String[] mode;
        if (updateA2dpDisplayTimer != null) {
            updateA2dpDisplayTimer.cancel();
            updateA2dpDisplayTimer = null;
        }
        if (playerStatus != PlayerStatus.PLAYING) {
            displayText = channelName + " (" + playerStatus.getText() + ")";
            updateA2dpDisplay(displayText);
        }
        else {
            mode = new String[]{"playing"};

            updateA2dpDisplayTimer = new Timer();
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    switch (mode[0]) {
                        case "playing":
                            displayText = channelName + " (" + playerStatus.getText() + ")";
                            mode[0] = "artist";
                            break;
                        case "artist":
                            displayText = artistText;
                            mode[0] = "title";
                            break;
                        case "title":
                            displayText = titleText;
                            mode[0] = "category";
                            break;
                        case "category":
                            displayText = "Category: " + radioChannels[currentChannelTableNumber].getChannelDescription();
                            mode[0] = "bitrate";
                            break;
                        case "bitrate":
                            displayText = "Bitrate: " + radioChannels[currentChannelTableNumber].getChannelBitrate();
                            mode[0] = "playing";
                            break;
                    }
                    updateA2dpDisplay(displayText);
                }
            };
            updateA2dpDisplayTimer.scheduleAtFixedRate(tt, 0, Integer.valueOf(preferences.getString(Settings.A2DP_DISPLAY_UPDATE_TIME.name(), "8000")));
        }
    }

    private void downloadMetaData() {

        stopDownloadMetaData();

        downloadMetaDataTimer = new Timer();
        TimerTask mt = new TimerTask() {
            @Override
            public void run() {
                refreshStreamMeta(radioChannels[currentChannelTableNumber].getChannelURL());
                artistText = getStreamArtist();
                String titleTextNew = getStreamTitle();
                if (!titleTextNew.equals(titleText)) {
                    titleText = titleTextNew;
                    updateScreen();
                }
            }
        };
        downloadMetaDataTimer.scheduleAtFixedRate(mt, 0, Integer.valueOf(preferences.getString(Settings.METADATA_REFRESH_TIME.name(), "12000")));
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

    private void refreshStreamMeta(String streamUrl) {
        try {
            streamMeta.setStreamUrl(new URL(streamUrl));
            streamMeta.refreshMeta();
            streamMeta.sortData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getStreamArtist() {
        return streamMeta.getArtist();
    }

    private String getStreamTitle() {
        return streamMeta.getTitle();
    }

    private void updateScreen() {
        Intent updateUpdateScreenIntent = new Intent();
        updateUpdateScreenIntent.setAction(ControlAction.UPDATE_SCREEN.name());
        updateUpdateScreenIntent.putExtra("Image", radioChannels[currentChannelTableNumber].getChannelImage());
        sendBroadcast(updateUpdateScreenIntent);
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
                currentChannelTableNumber = radioChannels.length - 1;
            }
            setChannelNameIcon();
        }
        if (playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.BUFFERING) {
            playRadio();
        } else if (playerStatus == PlayerStatus.READY){
            playerStatusChanged(PlayerStatus.READY);
        }
        resetArtistTitle();
        updateScreen();
    }

    private void onNextClick() {
        playBeep(Beep.NEXT);
        if (playerStatus != PlayerStatus.INTERRUPTED_PAUSE) {
            currentChannelTableNumber++;
            if (currentChannelTableNumber > radioChannels.length - 1 ) {
                currentChannelTableNumber = 0;
            }
            setChannelNameIcon();
        }
        if (playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.BUFFERING) {
            playRadio();
        } else if (playerStatus == PlayerStatus.READY){
            playerStatusChanged(PlayerStatus.READY);
        }
        resetArtistTitle();
        updateScreen();
    }

    private void setChannelNameIcon() {
        channelName = radioChannels[currentChannelTableNumber].getChannelName();
        channelImage = radioChannels[currentChannelTableNumber].getChannelImage();
    }

    private void reBufferingCountDown() {
        stopBufferingCountDown();
        int reBufferingTime = Integer.valueOf(preferences.getString(Settings.REBUFFERING_DELAY_TIME.name(), "8000"));
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
        radioChannels = new RadioChannel[]{radioZlotePrzeboje, radioZET, rmfFM, smoothJazz, p7klem};
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

            updateScreen();
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

    private void autoPlay() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean(Settings.RUN_SERVICE_AFTER_A2DP_CONNECTED.name(), true) &&
            preferences.getBoolean(Settings.AUTO_PLAY_AFTER_A2DP_CONNECTED.name(), false)) playRadio();
    }
    
    private void resetArtistTitle() {
        artistText = "Artist";
        titleText = "Title";
    }

    @Override
    public void onDestroy() {
        playerStatusChanged(PlayerStatus.READY);
        updateScreen();
        stopPlay();
        setVolume(startupVolume);
        cancelNotification();
        releaseAudioFocus(this);
        ms.release();
        unregisterReceiver(controlReceiver);
        super.onDestroy();
    }

    private void playMusic() {

        playerStatusChanged(PlayerStatus.BUFFERING);
        reBufferingCountDown();

        try {
            mp = new MediaPlayer();
            mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mp.setDataSource(radioChannels[currentChannelTableNumber].getChannelURL());
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
            public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        playBeep(Beep.BUFFERING);
                        playRadio();
                        break;
                }
                return false;
            }
        });
    }
}
