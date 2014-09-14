package com.shagalalab.weather.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.shagalalab.weather.Utility;

/**
 * Created by atabek on 08/27/14.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Utility.showNotificationIfEnabled(context);
        }
    }
}
