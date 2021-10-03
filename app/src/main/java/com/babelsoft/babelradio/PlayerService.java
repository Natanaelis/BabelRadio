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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PlayerService extends Service implements IMetadataAsyncResponse{
    public static Radio currentRadio;
    public static PlayerStatus playerStatus = PlayerStatus.READY;
    public static List<Radio> radioList = new ArrayList<Radio>();
    private InternalDatabaseHandler internalDatabaseHandler;
    private int currentRadioNumber;
    private AudioControl audioControl;
    private PlayerStatus playerPreviousStatus = PlayerStatus.READY;
    private MediaPlayer mediaPlayer = null;
    private MediaSession mediaSession;
    private BroadcastReceiver controlReceiver;
    private PendingIntent resultPendingIntent;
    private SharedPreferences preferences;
    private RemoteViews notificationView;
    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener;
    private String displayText = null;
    private DisplayMode displayMode;
    private Timer updateA2dpDisplayTimer;
    private Timer downloadMetaDataTimer;
    private CountDownTimer reBufferingTimer;
    private Notification.Builder notificationBuilder;
    private final int notificationId = 78;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        loadRadios();
        loadSettings();
        initiateNotification();
        showNotification();
        initializeVolumeControl();
        initializeMediaPlayer();
        initializeControlReceiver();
        initializeAudioChangeFocus();
        initializeMediaButtons();
        setMaxVolume();
        playerStatusChanged(PlayerStatus.READY);
        updateNotificationStatus();
        updateNotificationArtistTitle();
        updateScreenStatus();
        updateScreenArtistTitle();
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
        notificationBuilder = new Notification.Builder(this, getString(R.string.app_name))
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.icons8_radio_tower_noti)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setContentIntent(resultPendingIntent)
                .setCustomContentView(notificationView)
                .setAutoCancel(false)
                .setOngoing(true);

        startForeground(notificationId, notificationBuilder.build());
    }

    private void initializeVolumeControl() {
        audioControl = new AudioControl(this);
        audioControl.readStartupVolume();
    }

    private void initializeMediaPlayer() {
        mediaPlayer = audioControl.initializeMediaPlayer();
    }

    private void setMaxVolume() {
        if (audioControl.isBluetoothA2dpOn()) {
            audioControl.setMaxVolume();
        }
    }

    private void initializeMediaButtons() {
        playDummyAudio();

        mediaSession = new MediaSession(this, "MyMediaSession");
        mediaSession.setCallback(new MediaSession.Callback() {

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

        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);

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
                                    audioControl.setStartupVolume();
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
        onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        audioControl.setCurrentVolume();
                        if (playerStatus != PlayerStatus.READY) playerStatusChanged(playerPreviousStatus);
                        if (playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.BUFFERING) playRadio();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        audioControl.readCurrentVolume();
                        playerStatusChanged(PlayerStatus.READY);
                        stopPlay();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        audioControl.readCurrentVolume();
                        playerStatusChanged(PlayerStatus.INTERRUPTED_PAUSE);
                        stopPlay();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        audioControl.readCurrentVolume();
                        audioControl.setHalfVolume();
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
        notificationView.setTextViewText(R.id.artist_text, currentRadio.getRadioArtist());
        notificationView.setTextViewText(R.id.title_text, currentRadio.getRadioTitle());

        notificationBuilder.setCustomContentView(notificationView);
        startForeground(notificationId, notificationBuilder.build());
    }

    private void updateNotificationStatus() {
        notificationView.setTextViewText(R.id.radio_name_text, currentRadio.getRadioName());
        notificationView.setTextViewText(R.id.player_status_text, playerStatus.getText());
        notificationView.setImageViewBitmap(R.id.radio_image, DbBitmapUtility.getBitmap(currentRadio.getRadioImage()));

        if (playerStatus == PlayerStatus.READY) notificationView.setImageViewResource(R.id.play_stop_button, R.drawable.button_play);
        else notificationView.setImageViewResource(R.id.play_stop_button, R.drawable.button_stop);

        notificationBuilder.setCustomContentView(notificationView);
        startForeground(notificationId, notificationBuilder.build());
    }

    private void playDummyAudio() {
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
        at.play();
        at.stop();
        at.release();
    }

    private void sendTrackInfoToA2dp() {
        if (updateA2dpDisplayTimer != null) {
            updateA2dpDisplayTimer.cancel();
            updateA2dpDisplayTimer = null;
        }
        if (playerStatus != PlayerStatus.PLAYING) {
            displayText = currentRadio.getRadioName() + " (" + playerStatus.getText() + ")";
            updateA2dpDisplay(displayText);
        }
        else {
            updateA2dpDisplayTimer = new Timer();
            displayMode = DisplayMode.PLAYING;
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    switch (displayMode) {
                        case PLAYING:
                            displayText = currentRadio.getRadioName() + " (" + playerStatus.getText() + ")";
                            displayMode = DisplayMode.ARTIST;
                            break;
                        case ARTIST:
                            displayText = currentRadio.getRadioArtist();
                            displayMode = DisplayMode.TITLE;
                            break;
                        case TITLE:
                            displayText = currentRadio.getRadioTitle();
                            displayMode = DisplayMode.TAG;
                            break;
                        case TAG:
                            displayText = currentRadio.getRadioTag();
                            displayMode = DisplayMode.BITRATE;
                            break;
                        case BITRATE:
                            displayText = currentRadio.getRadioBitrate();
                            displayMode = DisplayMode.PLAYING;
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
                    mt.execute(new URL(currentRadio.getRadioStream()));
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

        mediaSession.setMetadata(metadata);

        PlaybackState state = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY)
                .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f, SystemClock.elapsedRealtime())
                .build();

        mediaSession.setPlaybackState(state);
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
        if (radioList.size() > 0) {
            if (playerStatus != PlayerStatus.INTERRUPTED_PAUSE) {
                currentRadioNumber--;
                if (currentRadioNumber < 0) {
                    currentRadioNumber = radioList.size() - 1;
                }
                rememberCurrentRadioNumber();
            }
            currentRadio = radioList.get(currentRadioNumber);
            if (playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.BUFFERING) {
                playRadio();
            } else if (playerStatus == PlayerStatus.READY) {
                playerStatusChanged(PlayerStatus.READY);
            }
            resetArtistTitle();
            updateScreenStatus();
            updateScreenArtistTitle();
            updateNotificationStatus();
            updateNotificationArtistTitle();
        }
    }

    private void onNextClick() {
        playBeep(Beep.NEXT);
        if (radioList.size() > 0) {
            if (playerStatus != PlayerStatus.INTERRUPTED_PAUSE) {
                currentRadioNumber++;
                if (currentRadioNumber > radioList.size() - 1 ) {
                    currentRadioNumber = 0;
                }
            rememberCurrentRadioNumber();
            }
            currentRadio = radioList.get(currentRadioNumber);
            if (playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.BUFFERING) {
                playRadio();
            } else if (playerStatus == PlayerStatus.READY){
                playerStatusChanged(PlayerStatus.READY);
            }
            resetArtistTitle();
            updateScreenStatus();
            updateScreenArtistTitle();
            updateNotificationStatus();
            updateNotificationArtistTitle();
        }
    }

    private void rememberCurrentRadioNumber() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(Settings.CURRENT_RADIO_NUMBER.name(), currentRadioNumber);
        editor.apply();
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
            if (audioControl.isBluetoothA2dpOn()) sendTrackInfoToA2dp();
        }
    }

    private void stopPlay() {
        stopBufferingCountDown();
        stopDownloadMetaData();

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void loadSettings() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        currentRadioNumber = preferences.getInt(Settings.CURRENT_RADIO_NUMBER.name(), 0);
        if (currentRadioNumber > radioList.size() - 1) {
            currentRadioNumber = 0;
        }
        else {
            currentRadio = radioList.get(currentRadioNumber);
        }
    }

    private void loadRadios() {
        internalDatabaseHandler = new InternalDatabaseHandler(this);
        int numberOfRadios = internalDatabaseHandler.count();

        if (numberOfRadios > 0) {
            radioList = internalDatabaseHandler.getAllRadios();
        }
        else {
            Bitmap image = BitmapFactory.decodeResource(this.getResources(), R.drawable.nologo);


            currentRadio = new Radio(0, 0, "No Radio Stations to play",
                    "", DbBitmapUtility.getBytes(image), "");
            currentRadio.setRadioArtist("Add New Radio Station");
            currentRadio.setRadioTitle("Use bottom left button to find and add radio");
        }
    }

    private void autoPlay() {
        if (preferences.getBoolean(Settings.RUN_SERVICE_AFTER_A2DP_CONNECTED.name(), true) &&
            preferences.getBoolean(Settings.AUTO_PLAY_AFTER_A2DP_CONNECTED.name(), false)) playRadio();
    }

    private void resetArtistTitle() {
        currentRadio.setRadioArtist("Artist");
        currentRadio.setRadioTitle("Title");
    }

    private void playRadio() {
        stopPlay();

        if (audioControl.requestAudioFocus(onAudioFocusChangeListener)) {
            playerStatusChanged(PlayerStatus.BUFFERING);
            reBufferingCountDown();

            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                mediaPlayer.setDataSource(currentRadio.getRadioStream());
                mediaPlayer.prepareAsync();

            } catch (IOException e) {
                e.printStackTrace();
            }

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer player) {
                    if (playerStatus != PlayerStatus.INTERRUPTED_PAUSE) {
                        stopBufferingCountDown();
                        mediaPlayer.start();
                        playerStatusChanged(PlayerStatus.PLAYING);
                        downloadMetaData();
                    }
                }
            });

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    playBeep(Beep.BUFFERING);
                    playRadio();
                }
            });

            mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
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
    }

    @Override
    public void onDestroy() {
        playerStatusChanged(PlayerStatus.READY);
        updateScreenStatus();
        updateNotificationStatus();
        stopPlay();
        audioControl.setStartupVolume();
        cancelNotification();
        audioControl.releaseAudioFocus(onAudioFocusChangeListener);
        mediaSession.release();
        unregisterReceiver(controlReceiver);
        super.onDestroy();
    }

    @Override
    public void metadataResult(String asyncResult) {
        Metadata m = new Metadata(asyncResult);
        if (!m.getArtist().equals(currentRadio.getRadioArtist()) && !m.getTitle().equals(currentRadio.getRadioTitle())) {
            currentRadio.setRadioArtist(m.getArtist());
            currentRadio.setRadioTitle(m.getTitle());
            updateNotificationArtistTitle();
            updateScreenArtistTitle();
        }
    }
}
