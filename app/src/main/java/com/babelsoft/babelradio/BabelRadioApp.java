package com.babelsoft.babelradio;

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
import java.util.HashMap;
import java.util.Map;

public class BabelRadioApp extends AppCompatActivity implements IHttpPostAsyncResponse {
    private TextView txtChannel;
    private TextView txtPlayerStatusTextView;
    private TextView txtArtistTextView;
    private TextView txtTitleTextView;
    private ImageView imgRadioImage;
    private ImageButton btnPlayStop;
    private ImageButton btnFindRadio;
    private BroadcastReceiver controlReceiver;
    private String inputUrl = "https://babelradio.000webhostapp.com/continents.php";
    public static String databaseResponse;



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
        ImageButton btnPrevious;
        ImageButton btnNext;
        ImageButton btnSettings;
        setContentView(R.layout.main_layout);
        txtChannel = (TextView) findViewById(R.id.channelTextView);
        txtPlayerStatusTextView = (TextView) findViewById(R.id.txtPlayerStatusTextView);
        txtArtistTextView = (TextView) findViewById(R.id.txtArtistTextView);
        txtTitleTextView = (TextView) findViewById(R.id.txtTitleTextView);
        imgRadioImage = (ImageView) findViewById(R.id.radioImage);
        imgRadioImage.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200
                , getResources().getDisplayMetrics());

        btnPlayStop = (ImageButton) findViewById(R.id.playStopButton);
        btnPrevious = (ImageButton) findViewById(R.id.previousButton);
        btnNext = (ImageButton) findViewById(R.id.nextButton);
        btnSettings = (ImageButton) findViewById(R.id.settingsButton);
        btnFindRadio = (ImageButton) findViewById(R.id.findRadioButton);

        btnPlayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcast(new Intent(ControlAction.PLAY_STOP.name()));
            }
        });

        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcast(new Intent(ControlAction.PREVIOUS.name()));
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcast(new Intent(ControlAction.NEXT.name()));
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(BabelRadioApp.this, SettingsActivity.class));
            }
        });

        btnFindRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findRadios();
            }
        });

        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    private void initializeServices() {
        ProcessControl pc = new ProcessControl();
        if (!pc.isServiceRunning(BootService.class, this)) {
            startForegroundService(new Intent(this, BootService.class));
        }
        if (!pc.isServiceRunning(PlayerService.class, this)) {
            startForegroundService(new Intent(this, PlayerService.class));
        }
    }

    private void initializeControlReceiver() {
        controlReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(ControlAction.UPDATE_SCREEN.name())) updateScreen();
                else if (action.equals(ControlAction.CLOSE_PLAYER_SERVICE.name())) {
                    String source = intent.getStringExtra("Source");
                    if (source.equals("Notification")) {
                        finish();
                    }
                }
            }
        };

        IntentFilter controlsFilter = new IntentFilter();
        controlsFilter.addAction(ControlAction.UPDATE_SCREEN.name());
        controlsFilter.addAction(ControlAction.CLOSE_PLAYER_SERVICE.name());

        registerReceiver(controlReceiver, controlsFilter);
    }

    private void updateScreen() {
        String channelTextNew = PlayerService.channelName;
        String channelTextOld = txtChannel.getText().toString();
        if (channelTextNew != null && !channelTextNew.equals(channelTextOld)) {
            txtChannel.setText(channelTextNew);
        }
        txtPlayerStatusTextView.setText(PlayerService.playerStatus.getText());
        txtArtistTextView.setText(PlayerService.artistText);
        txtTitleTextView.setText(PlayerService.titleText);
        imgRadioImage.setImageResource(PlayerService.channelImage);

        if (PlayerService.playerStatus == PlayerStatus.READY)
            btnPlayStop.setImageDrawable(getResources().getDrawable(R.drawable.button_play));
        else btnPlayStop.setImageDrawable(getResources().getDrawable(R.drawable.button_stop));
    }

    private void stopPlayerService() {
        Intent stopPlayerServiceIntent = new Intent();
        stopPlayerServiceIntent.putExtra("Source", "BabelRadioApp");
        stopPlayerServiceIntent.setAction(ControlAction.CLOSE_PLAYER_SERVICE.name());
        sendBroadcast(stopPlayerServiceIntent);
    }

    private void findRadios() {
        btnFindRadio.setEnabled(false);
        Map<String, String> postData = new HashMap<>();
        postData.put("continent", "continent");
        HttpPostAsync httpPost = new HttpPostAsync(postData);
        httpPost.delegate = this;
        httpPost.execute(inputUrl);
    }

    @Override
    public void postResult(String asyncResult) {
        Intent myIntent = new Intent(BabelRadioApp.this, ContinentsListActivity.class);
        databaseResponse = asyncResult;
        startActivity(myIntent);
        btnFindRadio.setEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendBroadcast(new Intent(ControlAction.REQUEST_UPDATE_SCREEN.name()));
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(controlReceiver);
        stopPlayerService();
        super.onDestroy();
    }

}