package com.babelsoft.babelradio;

import android.app.ActivityManager;
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
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PlayerService extends Service {
    private MediaPlayer mp = null;
    private AudioManager am;
    private BroadcastReceiver controlReceiver;
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;
    private int currentVolume;
    private static final int STATUS_READY = 0;
    private static final int STATUS_BUFFERING = 1;
    private static final int STATUS_PLAYING = 2;
    private static final int STATUS_INTERRUPTED_PAUSE = 3;
    private static String PLAYER_STATUS_TEXT = "Ready";
    private static String ARTIST_TEXT;
    private static String TITLE_TEXT;
    private static final int LOW_BEEP = 0;
    private static final int HIGH_BEEP = 1;
    private static final int NEXT_BEEPS = 0;
    private static final int PREVIOUS_BEEPS = 1;
    private static final int REBUFFERING_BEEPS = 2;
    private int playerStatus = STATUS_READY;
    private String displayText = null;
    private Timer updateA2dpDisplayTimer;
    private Timer downloadMetaDataTimer;
    private static RadioChannel[] radioChannels;
    private static int currentChannelTableNumber = 0;
    private CountDownTimer reBufferingTimer;
    private final static int BEEP_PLAY_SEPARATION_TIME = 200;
    private RadioChannel radioZlotePrzeboje, radioZET, rmfFM, smoothJazz, p7klem;
    private Timer playBeepTimer;
    private int playerPreviousStatus = STATUS_READY;
    private MediaSession ms;
    IcyStreamMeta streamMeta = new IcyStreamMeta();
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

        playerStatusChanged(STATUS_READY);

        resetArtistTitle();

        updateScreen();

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
                            sendBroadcast(new Intent("Stop"));
                            Log.i("Test", "Stop");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            sendBroadcast(new Intent("Pause"));
                            Log.i("Test", "Pause");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            sendBroadcast(new Intent("Play"));
                            Log.i("Test", "Play");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            sendBroadcast(new Intent("Pause"));
                            Log.i("Test", "Pause");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            sendBroadcast(new Intent("Next"));
                            Log.i("Test", "Next");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            sendBroadcast(new Intent("Previous"));
                            Log.i("Test", "Previous");
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
                switch (intent.getAction()) {
                    case "Stop":
                        onStopClick();
                        break;
                    case "Play":
                        onPlayClick();
                        break;
                    case "PlayStop":
                        if (playerStatus == STATUS_READY) onPlayClick();
                        else onStopClick();
                        break;
                    case "Pause":
                        onPauseClick();
                        break;
                    case "Next":
                        onNextClick();
                        break;
                    case "Previous":
                        onPreviousClick();
                        break;
                    case "ClosePlayerService":
                        String source = intent.getStringExtra("Source");
                        if (source.equals("BabelRadioApp")) {
                            if (playerStatus == STATUS_READY) {
                                stopSelf();
                            }
                        }
                        else if (source.equals("BootService")) {
                            if (isActivityRunning()) onStopClick();
                            else stopSelf();
                        }
                        else if (source.equals("Notification")) {
                            stopSelf();
                        }
                        break;
                    case "UpdateScreen":
                        updateNotification(intent);
                        break;
                    case "RequestUpdateScreen":
                        updateScreen();
                        break;
                }
            }
        };

        IntentFilter controlsFilter = new IntentFilter();
        controlsFilter.addAction("Stop");
        controlsFilter.addAction("Play");
        controlsFilter.addAction("PlayStop");
        controlsFilter.addAction("Pause");
        controlsFilter.addAction("Next");
        controlsFilter.addAction("Previous");
        controlsFilter.addAction("ClosePlayerService");
        controlsFilter.addAction("UpdateScreen");
        controlsFilter.addAction("RequestUpdateScreen");

        registerReceiver(controlReceiver, controlsFilter);
    }

    private void initializeAudioChangeFocus() {
        mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        resetToCurrentVolume();
                        playerStatusChanged(playerPreviousStatus);
                        if (playerStatus == STATUS_PLAYING || playerStatus == STATUS_BUFFERING) {
                            playRadio();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        rememberCurrentVolume();
                        playerStatusChanged(STATUS_READY);
                        stopPlay();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        rememberCurrentVolume();
                        playerStatusChanged(STATUS_INTERRUPTED_PAUSE);
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
        Intent nextButtonIntent = new Intent("Next");
        PendingIntent nextButtonPendingIntent = PendingIntent.getBroadcast(this, 1, nextButtonIntent, 0);
        view.setOnClickPendingIntent(R.id.next_button, nextButtonPendingIntent);

        //listener 2
        Intent playStopButtonIntent = new Intent("PlayStop");
        PendingIntent playStopButtonPendingIntent = PendingIntent.getBroadcast(this, 1, playStopButtonIntent, 0);
        view.setOnClickPendingIntent(R.id.play_stop_button, playStopButtonPendingIntent);

        //listener 3
        Intent closeButtonIntent = new Intent("ClosePlayerService");
        closeButtonIntent.putExtra("Source", "Notification");
        PendingIntent closeButtonPendingIntent = PendingIntent.getBroadcast(this, 0, closeButtonIntent, 0);
        view.setOnClickPendingIntent(R.id.close_button, closeButtonPendingIntent);

        //listener 4
        Intent previousButtonIntent = new Intent("Previous");
        PendingIntent previousButtonPendingIntent = PendingIntent.getBroadcast(this, 1, previousButtonIntent, 0);
        view.setOnClickPendingIntent(R.id.previous_button, previousButtonPendingIntent);
    }

    private void cancelNotification() {
        stopForeground(true);
    }

    private void updateNotification(Intent intent) {
        notificationView.setTextViewText(R.id.channel_name_text, intent.getStringExtra("Channel_Name"));
        notificationView.setTextViewText(R.id.artist_text, intent.getStringExtra("Artist"));
        notificationView.setTextViewText(R.id.title_text, intent.getStringExtra("Title"));
        notificationView.setTextViewText(R.id.status_text, intent.getStringExtra("Player_Status"));
        notificationView.setImageViewResource(R.id.channel_icon, intent.getIntExtra("Image", 0));

        if (playerStatus == STATUS_READY) notificationView.setImageViewResource(R.id.play_stop_button, R.mipmap.button_play);
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
        if (playerStatus != STATUS_PLAYING) {
            displayText = radioChannels[currentChannelTableNumber].getChannelName() + " (" + PLAYER_STATUS_TEXT + ")";
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
                            displayText = radioChannels[currentChannelTableNumber].getChannelName() + " (" + PLAYER_STATUS_TEXT + ")";
                            mode[0] = "artist";
                            break;
                        case "artist":
                            displayText = ARTIST_TEXT;
                            mode[0] = "title";
                            break;
                        case "title":
                            displayText = TITLE_TEXT;
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
            updateA2dpDisplayTimer.scheduleAtFixedRate(tt, 0, Integer.valueOf(preferences.getString("A2DP_DISPLAY_UPDATE_TIME", "8000")));
        }
    }

    private void downloadMetaData() {

        stopDownloadMetaData();

        downloadMetaDataTimer = new Timer();
        TimerTask mt = new TimerTask() {
            @Override
            public void run() {
                refreshStreamMeta(radioChannels[currentChannelTableNumber].getChannelURL());
                ARTIST_TEXT = getStreamArtist();
                String TITLE_TEXT_NEW = getStreamTitle();
                if (!TITLE_TEXT_NEW.equals(TITLE_TEXT)) {
                    TITLE_TEXT = TITLE_TEXT_NEW;
                    updateScreen();
                }
            }
        };
        downloadMetaDataTimer.scheduleAtFixedRate(mt, 0, Integer.valueOf(preferences.getString("METADATA_REFRESH_TIME", "12000")));
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
        updateUpdateScreenIntent.setAction("UpdateScreen");
        updateUpdateScreenIntent.putExtra("Channel_Name", radioChannels[currentChannelTableNumber].getChannelName());
        updateUpdateScreenIntent.putExtra("Player_Status", PLAYER_STATUS_TEXT);
        updateUpdateScreenIntent.putExtra("Artist", ARTIST_TEXT);
        updateUpdateScreenIntent.putExtra("Title", TITLE_TEXT);
        updateUpdateScreenIntent.putExtra("Image", radioChannels[currentChannelTableNumber].getChannelImage());
        sendBroadcast(updateUpdateScreenIntent);
    }

    private void rememberCurrentVolume() {
        currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void resetToCurrentVolume() {
//        if (am.isBluetoothA2dpOn()) setVolumeMax();
//        else
            am.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
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
        if (playerStatus == STATUS_READY) {
            playBeeps(REBUFFERING_BEEPS);
            playRadio();
        }
    }

    private void onStopClick() {
        if (playerStatus == STATUS_PLAYING || playerStatus == STATUS_BUFFERING) {
            playBeep(HIGH_BEEP);
            resetArtistTitle();
            playerStatusChanged(STATUS_READY);
            stopPlay();
        }
    }

    private void onPauseClick() {
        if (playerStatus == STATUS_PLAYING || playerStatus == STATUS_BUFFERING) onStopClick();
        else if (playerStatus == STATUS_READY) onPlayClick();
    }

    private void onPreviousClick() {
        playBeeps(PREVIOUS_BEEPS);
        if (playerStatus != STATUS_INTERRUPTED_PAUSE) {
            currentChannelTableNumber--;
            if (currentChannelTableNumber < 0 ) {
                currentChannelTableNumber = radioChannels.length - 1;
            }
        }
        if (playerStatus == STATUS_PLAYING || playerStatus == STATUS_BUFFERING) {
            playRadio();
        } else if (playerStatus == STATUS_READY){
            playerStatusChanged(STATUS_READY);
        }
        resetArtistTitle();
        updateScreen();
    }

    private void onNextClick() {
        playBeeps(NEXT_BEEPS);
        if (playerStatus != STATUS_INTERRUPTED_PAUSE) {
            currentChannelTableNumber++;
            if (currentChannelTableNumber > radioChannels.length - 1 ) {
                currentChannelTableNumber = 0;
            }
        }
        if (playerStatus == STATUS_PLAYING || playerStatus == STATUS_BUFFERING) {
            playRadio();
        } else if (playerStatus == STATUS_READY){
            playerStatusChanged(STATUS_READY);
        }
        resetArtistTitle();
        updateScreen();
    }

    private void reBufferingCountDown() {
        stopBufferingCountDown();
        int reBufferingTime = Integer.valueOf(preferences.getString("REBUFFERING_DELAY_TIME", "8000"));
        reBufferingTimer = new CountDownTimer(reBufferingTime, reBufferingTime) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                Log.i("Test", "reBufferingCountDown started");
                playBeeps(REBUFFERING_BEEPS);
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

    private void playBeep(int beep) {
        final MediaPlayer mp_beep;
        if (beep == LOW_BEEP) {
            mp_beep = MediaPlayer.create(this, R.raw.beep_low2);
        }
        else {
            mp_beep = MediaPlayer.create(this, R.raw.beep_high2);
        }
        mp_beep.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.stop();
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
            }
        });
        mp_beep.start();
    }

    private void playBeeps (int beeps) {
        if (playBeepTimer != null) {
            playBeepTimer.cancel();
            playBeepTimer.purge();
        }
        playBeepTimer = new Timer();
        TimerTask beep_tt = null;
        switch (beeps) {
            case NEXT_BEEPS:
                beep_tt = new TimerTask() {
                    int numberBeeps = 2;
                    int beepNumber = 0;
                    @Override
                    public void run() {
                        beepNumber++;
                        if (beepNumber == 1) playBeep(LOW_BEEP);
                        else playBeep(HIGH_BEEP);
                        if (beepNumber >= numberBeeps) {
                            playBeepTimer.cancel();
                            playBeepTimer.purge();
                        }
                    }
                };
                break;
            case PREVIOUS_BEEPS:
                beep_tt = new TimerTask() {
                    int numberBeeps = 2;
                    int beepNumber = 0;
                    @Override
                    public void run() {
                        beepNumber++;
                        if (beepNumber == 1) playBeep(HIGH_BEEP);
                        else playBeep(LOW_BEEP);
                        if (beepNumber >= numberBeeps) {
                            playBeepTimer.cancel();
                            playBeepTimer.purge();
                        }
                    }
                };
                break;
            case REBUFFERING_BEEPS:
                beep_tt = new TimerTask() {
                    int numberBeeps = 3;
                    int beepNumber = 0;
                    @Override
                    public void run() {
                        beepNumber++;
                        playBeep(LOW_BEEP);
                        if (beepNumber >= numberBeeps) {
                            playBeepTimer.cancel();
                            playBeepTimer.purge();
                        }
                    }
                };
                break;

        }
        playBeepTimer.scheduleAtFixedRate(beep_tt, 0, BEEP_PLAY_SEPARATION_TIME);
    }

    private void playerStatusChanged(int newStatus) {
        if (playerStatus != newStatus) {
            playerPreviousStatus = playerStatus;
            playerStatus = newStatus;

            switch (playerStatus) {
                case STATUS_READY:
                    PLAYER_STATUS_TEXT = "Ready";
                    break;
                case STATUS_BUFFERING:
                    PLAYER_STATUS_TEXT = "Buffering";
                    break;
                case STATUS_PLAYING:
                    PLAYER_STATUS_TEXT = "Playing";
                    break;
                case STATUS_INTERRUPTED_PAUSE:
                    PLAYER_STATUS_TEXT = "Paused";
                    break;
            }

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
        if (preferences.getBoolean("RUN_SERVICE_AFTER_A2DP_CONNECTED", true) &&
            preferences.getBoolean("AUTO_PLAY_AFTER_A2DP_CONNECTED", false)) playRadio();
    }
    
    private void resetArtistTitle() {
        ARTIST_TEXT = "Artist";
        TITLE_TEXT = "Title";
    }

    public boolean isActivityRunning() {
        boolean isActivityFound = false;

        ActivityManager activityManager = (ActivityManager) getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> activities = activityManager.getRunningTasks(Integer.MAX_VALUE);
        for (int i = 0; i < activities.size(); i++) {
            if (activities.get(i).topActivity.toString().equalsIgnoreCase("ComponentInfo{com.babelsoft.babelradio/com.babelsoft.babelradio.BabelRadioApp}")) {
                isActivityFound = true;
                break;
            }
        }
        return isActivityFound;
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if (service.started && service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        playerStatusChanged(STATUS_READY);
        updateScreen();
        stopPlay();
        cancelNotification();
        releaseAudioFocus(this);
        ms.release();
        unregisterReceiver(controlReceiver);
        super.onDestroy();
    }

    private void playMusic() {

        playerStatusChanged(STATUS_BUFFERING);
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
                if (playerStatus != STATUS_INTERRUPTED_PAUSE) {
                    stopBufferingCountDown();
                    mp.start();
                    playerStatusChanged(STATUS_PLAYING);
                    downloadMetaData();
                }
            }
        });

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.i("Test", "OnCompletition started");
                playBeeps(REBUFFERING_BEEPS);
                playRadio();
            }
        });

        mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        Log.i("Test", "Buffering started");
                        playBeeps(REBUFFERING_BEEPS);
                        playRadio();
                        break;
                }
                return false;
            }
        });
    }
}
