package com.shagalalab.weather.receiver;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.shagalalab.weather.Utility;

/**
 * Created by atabek on 08/27/14.
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // update weather data if needed
        Utility.updateWeatherData(context);

        // update widget
        Intent update = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        update.putExtra(Utility.UPDATE_WIDGET, true);
        context.sendBroadcast(update);

        // update notification
        Utility.showNotificationIfEnabled(context);
    }
}
