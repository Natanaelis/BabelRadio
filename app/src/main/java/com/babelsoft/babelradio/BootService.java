package com.babelsoft.babelradio;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BootService extends Service {
    private final static String TAG = "Test";
    private static boolean isBluetoothOn = false;
    private static boolean isBluetoothA2dpConnected = false;
    private BroadcastReceiver a2dpReceiver;
    private BroadcastReceiver connectedDevicesReceiver;
    private BroadcastReceiver bluetoothReceiver;
    private Intent a2dpIntent = null;
    private BluetoothAdapter mBluetoothAdapter;
    private String a2dpDeviceName = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        initiateNotification();
        checkBluetoothStatus();
        registerBluetoothStatusReceiver();
        registerBluetoothA2dpStatusReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void initiateNotification() {
        // This is only to allow run service in foreground
        startForeground(79, new Notification.Builder(this).build());
    }

    private void checkBluetoothStatus() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.i(TAG, "Device does not support Bluetooth");
            isBluetoothOn = false;
            isBluetoothA2dpConnected = false;
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                isBluetoothOn = true;
                checkBluetoothA2dpStatus();
            } else {
                isBluetoothOn = false;
                isBluetoothA2dpConnected = false;
                Log.i(TAG, "Bluetooth is off");
            }
        }
    }

    private void registerBluetoothStatusReceiver() {
        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            isBluetoothOn = false;
                            isBluetoothA2dpConnected = false;
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            isBluetoothOn = false;
                            isBluetoothA2dpConnected = false;
                            break;
                        case BluetoothAdapter.STATE_ON:
                            isBluetoothOn = true;
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            isBluetoothOn = false;
                            isBluetoothA2dpConnected = false;
                            break;
                    }
                }
            }
        };

        registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    private void registerBluetoothA2dpStatusReceiver() {
        a2dpReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (isBluetoothOn) {
                    a2dpIntent = intent;
                    String action = intent.getAction();
                    if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED);
                        if (state == BluetoothA2dp.STATE_CONNECTED) {
                            playConnectionSound();
                            a2dpDeviceName = device.getName();
                            Log.i(TAG, "BluetoothA2dp Device Connected: " + a2dpDeviceName);
                            isBluetoothA2dpConnected = true;
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);;
                            if (preferences.getBoolean("RUN_SERVICE_AFTER_A2DP_CONNECTED", true)) launchPlayerService();
                        } else if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                            if (isBluetoothA2dpConnected) {
                                a2dpDeviceName = device.getName();
                                Log.i(TAG, "BluetoothA2dp Device Disconnected: " + a2dpDeviceName);
                                isBluetoothA2dpConnected = false;
                                if (isServiceRunning(PlayerService.class)) closePlayerService();
                            }
                        }
                    }
/*
                    else if (action.equals(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)) {
                        int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_NOT_PLAYING);
                        if (state == BluetoothA2dp.STATE_PLAYING) {
                        }
                        else if (state == BluetoothA2dp.STATE_NOT_PLAYING) {
                        }
                    }
*/
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction((BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED));
        intentFilter.addAction((BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED));

        registerReceiver(a2dpReceiver, intentFilter);
    }

    private void checkBluetoothA2dpStatus() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean status = mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.A2DP) == BluetoothHeadset.STATE_CONNECTED;
        if (status) {
            isBluetoothA2dpConnected = true;
            playConnectionSound();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);;
            new CountDownTimer(Integer.valueOf(preferences.getString("CHECK_A2DP_DEVICE_DELAY_TIME", "15000")), 100) {
                public void onTick(long millisUntilFinished) {
                    if (a2dpIntent != null) {
                        BluetoothDevice device = a2dpIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Log.i(TAG, "BluetoothA2dp Device Connected: " + device.getName());
                        cancel();
                    }
                }
                public void onFinish() {
                    Log.i(TAG, "BluetoothA2dp Device Not Recognized");
                }
            }.start();
        } else {
            isBluetoothA2dpConnected = false;
            Log.i(TAG, "BluetoothA2dp Not Connected");
        }
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

    private void launchPlayerService() {
        if (!isServiceRunning(PlayerService.class)) startForegroundService(new Intent(this, PlayerService.class));
    }

    private void closePlayerService() {
        Intent stopPlayerServiceIntent = new Intent();
        stopPlayerServiceIntent.putExtra("Source", "BootService");
//        stopPlayerServiceIntent.setAction("ClosePlayerService");
        stopPlayerServiceIntent.setAction(ControlAction.CLOSE_PLAYER_SERVICE.name());
        sendBroadcast(stopPlayerServiceIntent);
    }

    private void getListOfPairedBluetoothDevices() {
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = ba.getBondedDevices();

        List<String> s = new ArrayList<String>();
        for (BluetoothDevice bt : pairedDevices)
            s.add(bt.getName());
        Log.i(TAG, "Number of Paired Bluetooth Devices: " + String.valueOf(s.size()));
        for (String x : s)
            Log.i(TAG, x);
//        setListAdapter(new ArrayAdapter<String>(this, R.layout.list, s));
    }

    private void getListOfConnectedBluetoothDevices() {
        connectedDevicesReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                // When discovery finds a device
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    Log.i(TAG, "Device Connected: " + device.getName());
                    //you can get name by device.getName()

                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    Log.i(TAG, "Device Connected");

                }
            }
        };

        registerReceiver(connectedDevicesReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

    }

    private void playConnectionSound() {
        final MediaPlayer mp = MediaPlayer.create(this, R.raw.changer_connection);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.stop();
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
            }
        });
        mp.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (a2dpReceiver != null) unregisterReceiver(a2dpReceiver);
        if (connectedDevicesReceiver != null) unregisterReceiver(connectedDevicesReceiver);
        if (bluetoothReceiver != null) unregisterReceiver(bluetoothReceiver);
        startForegroundService(new Intent(this, BootService.class));
    }
}