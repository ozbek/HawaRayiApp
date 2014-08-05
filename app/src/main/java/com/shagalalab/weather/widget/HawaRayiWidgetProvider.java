package com.shagalalab.weather.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

import com.shagalalab.weather.MainActivity;
import com.shagalalab.weather.R;
import com.shagalalab.weather.Utility;
import com.shagalalab.weather.data.WeatherContract;

import java.util.Date;

/**
 * Created by atabek on 8/1/14.
 */
public class HawaRayiWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        boolean updateWidget = intent.getBooleanExtra(Utility.UPDATE_WIDGET, false);
        if (updateWidget) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, HawaRayiWidgetProvider.class);
            int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, allWidgetIds);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            String mLocation = Utility.getWidgetLocation(context, appWidgetId);
            Date todayDate = new Date();
            String todayStr = Utility.getDbDateString(todayDate);
            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    mLocation, todayStr);
            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";
            Cursor cursor = context.getContentResolver().query(
                    weatherForLocationUri,
                    Utility.WIDGET_FORECAST_COLUMNS,
                    null,
                    null,
                    sortOrder
            );

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            if (cursor != null && cursor.moveToFirst()){
                int weatherId = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));

                String date = cursor.getString(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATETEXT));
                String friendlyDateText = Utility.getDayName(context, date);
                String dateText = Utility.getFormattedMonthDay(context, date);

                int cityId = cursor.getInt(cursor.getColumnIndex(WeatherContract.LocationEntry.COLUMN_CITY_ID));
                String city = context.getResources().getStringArray(R.array.pref_location_options)[cityId];

                double high = cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP));
                String highString = Utility.formatTemperature(context, high);

                double low = cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));
                String lowString = Utility.formatTemperature(context, low);

                views.setTextViewText(R.id.widget_city, city);
                views.setTextViewText(R.id.widget_today_temp, highString);
                views.setImageViewResource(R.id.widget_today_icon, Utility.getArtResourceForWeatherCondition(weatherId));
            }

            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widget_city, pendingIntent);
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
