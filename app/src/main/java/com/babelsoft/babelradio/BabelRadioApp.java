package com.babelsoft.babelradio;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class BabelRadioApp extends AppCompatActivity {
    private TextView txtChannel;
    private TextView txtPlayerStatusTextView;
    private TextView txtArtistTextView;
    private TextView txtTitleTextView;
    private ImageView imgRadioImage;
    private static ImageButton btnPlayStop;
    private static ImageButton btnPrevious;
    private static ImageButton btnNext;
    private static ImageButton btnSettings;
    private BroadcastReceiver controlReceiver;
    private static String PLAYER_STATUS_TEXT = "Ready";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeSharedPreferences();

        initializeUI();

        initializeServices();

        initializeControlReceiver();

    }

    private void initializeSharedPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    private void initializeUI() {
        setContentView(R.layout.main_layout);
        txtChannel = (TextView) findViewById(R.id.channelTextView);
        txtPlayerStatusTextView = (TextView) findViewById(R.id.txtPlayerStatusTextView);
        txtArtistTextView = (TextView) findViewById(R.id.txtArtistTextView);
        txtTitleTextView = (TextView) findViewById(R.id.txtTitleTextView);
        imgRadioImage = (ImageView) findViewById(R.id.radioImage);
        imgRadioImage.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200
                , getResources().getDisplayMetrics());;

        btnPlayStop = (ImageButton) findViewById(R.id.playStopButton);
        btnPrevious = (ImageButton) findViewById(R.id.previousButton);
        btnNext = (ImageButton) findViewById(R.id.nextButton);
        btnSettings = (ImageButton) findViewById(R.id.settingsButton);

        btnPlayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcast(new Intent("PlayStop"));
            }
        });

        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcast(new Intent("Previous"));
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcast(new Intent("Next"));
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(BabelRadioApp.this, SettingsActivity.class));
            }
        });

        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

    }

    private void initializeServices() {
        if (!isServiceRunning(BootService.class)) {
            startForegroundService(new Intent(this, BootService.class));
        }
        if (!isServiceRunning(PlayerService.class)) {
            startForegroundService(new Intent(this, PlayerService.class));
        }
    }

    private void initializeControlReceiver() {
        controlReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "UpdateScreen":
                        updateScreen(intent);
                        break;
                    case "ClosePlayerService":
                        String source = intent.getStringExtra("Source");
                        if (source.equals("Notification")) {
                            finish();
                        }
                        break;
                }
            }
        };

        IntentFilter controlsFilter = new IntentFilter();
        controlsFilter.addAction("UpdateScreen");
        controlsFilter.addAction("ClosePlayerService");

        registerReceiver(controlReceiver, controlsFilter);
    }

    private void updateScreen(Intent intent) {

        PLAYER_STATUS_TEXT = intent.getStringExtra("Player_Status");

        String channelTextNew = intent.getStringExtra("Channel_Name");
        String channelTextOld = txtChannel.getText().toString();
        if (!channelTextNew.equals(channelTextOld)) {
            txtChannel.setText(channelTextNew);
        }
        txtPlayerStatusTextView.setText(PLAYER_STATUS_TEXT);
        txtArtistTextView.setText(intent.getStringExtra("Artist"));
        txtTitleTextView.setText(intent.getStringExtra("Title"));

        imgRadioImage.setImageResource(intent.getIntExtra("Image", 0));

        if (PLAYER_STATUS_TEXT.equals("Ready")) btnPlayStop.setImageDrawable(getResources().getDrawable(R.mipmap.button_play));
        else btnPlayStop.setImageDrawable(getResources().getDrawable(R.mipmap.button_stop));
    }

    private void stopPlayerService() {
        Intent stopPlayerServiceIntenet = new Intent();
        stopPlayerServiceIntenet.putExtra("Source", "BabelRadioApp");
        stopPlayerServiceIntenet.setAction("ClosePlayerService");
        sendBroadcast(stopPlayerServiceIntenet);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendBroadcast(new Intent("RequestUpdateScreen"));
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(controlReceiver);
        stopPlayerService();
        super.onDestroy();
    }
}