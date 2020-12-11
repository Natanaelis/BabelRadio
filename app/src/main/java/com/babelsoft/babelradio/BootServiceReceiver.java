package com.babelsoft.babelradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent bootIntent = new Intent(context, BootService.class);
        context.startForegroundService(bootIntent);
    }
}
