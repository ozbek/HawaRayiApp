package com.shagalalab.weather.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.shagalalab.weather.MainActivity;
import com.shagalalab.weather.R;
import com.shagalalab.weather.Utility;
import com.shagalalab.weather.data.WeatherContract;

import java.util.Date;

/**
 * Created by atabek on 8/11/14.
 */
public class HawaRayiWidgetProviderLarge extends AppWidgetProvider {
    private String LOG_TAG = HawaRayiWidgetProviderLarge.class.getSimpleName();

    public static final int[] widgetDays = { R.id.widget_large_today_date, R.id.widget_large_tomorrow_day, R.id.widget_large_tomorrow2_day, R.id.widget_large_tomorrow3_day };
    public static final int[] widgetIcons = { R.id.widget_large_today_icon, R.id.widget_large_tomorrow_icon, R.id.widget_large_tomorrow2_icon, R.id.widget_large_tomorrow3_icon };
    public static final int[] widgetTemperature = { R.id.widget_large_today_temp_high, R.id.widget_large_tomorrow_temp, R.id.widget_large_tomorrow2_temp, R.id.widget_large_tomorrow3_temp };

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w(LOG_TAG, "onReceive - start");
        super.onReceive(context, intent);

        boolean updateWidget = intent.getBooleanExtra(Utility.UPDATE_WIDGET, false);
        if (updateWidget) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, HawaRayiWidgetProviderLarge.class);
            int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, allWidgetIds);
        }
        Log.w(LOG_TAG, "onReceive - end");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.w(LOG_TAG, "onUpdate - start");

        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            String mLocation = Utility.getWidgetLocation(context, appWidgetId);
            Date todayDate = new Date();
            String todayStr = Utility.getDbDateString(todayDate);
            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                    mLocation, todayStr);
            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";
            Cursor cursor = context.getContentResolver().query(
                    weatherForLocationUri,
                    Utility.WIDGET_FORECAST_COLUMNS,
                    null,
                    null,
                    sortOrder
            );

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_large);

            if (cursor != null && cursor.getCount() > 3){
                while (cursor.moveToNext()) {
                    if (cursor.getPosition() > 3) {
                        break;
                    }

                    String date = cursor.getString(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATETEXT));
                    String dateText;
                    String temperature;

                    int weatherId = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));

                    double high = cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP));
                    String highString = Utility.formatTemperature(context, high);

                    double low = cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));
                    String lowString = Utility.formatTemperature(context, low);

                    if (cursor.getPosition() == 0) {
                        int cityId = cursor.getInt(cursor.getColumnIndex(WeatherContract.LocationEntry.COLUMN_CITY_ID));
                        String city = context.getResources().getStringArray(R.array.pref_location_options)[cityId];
                        views.setTextViewText(R.id.widget_large_city, city);

                        dateText = Utility.getFormattedMonthDay(context, date);

                        views.setTextViewText(R.id.widget_large_today_temp_low, lowString);

                        temperature = highString;
                    } else {
                        dateText = Utility.getWeekOfDay(context, date).substring(0, 3);

                        temperature = highString + "/" + lowString;
                    }

                    views.setTextViewText(widgetTemperature[cursor.getPosition()], temperature);
                    views.setTextViewText(widgetDays[cursor.getPosition()], dateText);
                    views.setImageViewResource(widgetIcons[cursor.getPosition()], Utility.getArtResourceForWeatherCondition(weatherId));
                }
            }

            // Create an Intent to launch MainActivity
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            intent.putExtra(Utility.APP_WIDGET_ID, appWidgetId);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, 0);
            views.setOnClickPendingIntent(R.id.large_layout, pendingIntent);
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        Log.w(LOG_TAG, "onUpdate - end");
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.w(LOG_TAG, "onDeleted - start");
        super.onDeleted(context, appWidgetIds);

        for (int q = 0; q < appWidgetIds.length; q++) {
            int result = context.getContentResolver().delete(WeatherContract.LocationEntry.buildWidgetUri(appWidgetIds[q]),
                    WeatherContract.LocationEntry.COLUMN_APP_WIDGET_ID + " = " + appWidgetIds[q], null);
        }
        Log.w(LOG_TAG, "onDeleted - end");
    }
}
