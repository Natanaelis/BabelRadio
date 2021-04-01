package com.babelsoft.babelradio;

import android.app.ActivityManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;

public class ProcessControl extends AppCompatActivity  {

    public boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isActivityRunning(Class<?> activityClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningTaskInfo activity : manager.getRunningTasks(Integer.MAX_VALUE)) {
            if (activityClass.getName().equals(activity.topActivity.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
