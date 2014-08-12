package com.shagalalab.weather.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RemoteViews;

import com.shagalalab.weather.R;
import com.shagalalab.weather.Utility;
import com.shagalalab.weather.data.WeatherContract;
import com.shagalalab.weather.service.HawaRayiService;

import java.util.Date;

/**
 * Created by atabek on 08/03/14.
 */
public class HawaRayiWidgetConfigure extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private int mAppWidgetId;
    private String selectedCity;
    private int cityIndex;
    private RemoteViews views;
    private AppWidgetManager appWidgetManager = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String uiInterface = prefs.getString(getString(R.string.pref_interface_key),
                getString(R.string.pref_interface_default));
        if (!uiInterface.equals(getString(R.string.pref_interface_default))) {
            Utility.changeLocale(this);
        }

        addPreferencesFromResource(R.xml.widget_configure);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_widget_location_key)));

        // Perform your App Widget configuration.
        ListView v = getListView();
        Button buttonCreateWidget = new Button(this);
        buttonCreateWidget.setText(R.string.create_widget);
        buttonCreateWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parseCursor();

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
        v.addFooterView(buttonCreateWidget);
    }

    private void parseCursor() {
        // First, get the App Widget ID from the Intent that launched the Activity:
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // When the configuration is complete, get an instance of the AppWidgetManager by calling getInstance(Context):
        appWidgetManager = AppWidgetManager.getInstance(this);

        Utility.insertWidgetLocationInDatabase(this, selectedCity, mAppWidgetId);
        Date todayDate = new Date();
        String todayStr = Utility.getDbDateString(todayDate);
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

        AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(mAppWidgetId);
        String widgetLabel = info.label;

        // if configuration for small widget
        if (widgetLabel.equals(getString(R.string.widget_label_small))) {
            views = new RemoteViews(getPackageName(), R.layout.widget_layout_small);

            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    selectedCity, todayStr);
            Cursor cursor = getContentResolver().query(
                    weatherForLocationUri,
                    Utility.WIDGET_FORECAST_COLUMNS,
                    null,
                    null,
                    sortOrder
            );

            if (cursor != null && cursor.moveToFirst()){
                int weatherId = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));

                String date = cursor.getString(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATETEXT));
                String dateText = Utility.getFormattedMonthDay(this, date);

                int cityId = cursor.getInt(cursor.getColumnIndex(WeatherContract.LocationEntry.COLUMN_CITY_ID));
                String city = getResources().getStringArray(R.array.pref_location_options)[cityId];

                double high = cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP));
                String highString = Utility.formatTemperature(this, high);

                views.setTextViewText(R.id.widget_city, city);
                views.setTextViewText(R.id.widget_today_temp, highString);
                views.setTextViewText(R.id.widget_today_date, dateText);
                views.setImageViewResource(R.id.widget_today_icon, Utility.getArtResourceForWeatherCondition(weatherId));
                appWidgetManager.updateAppWidget(mAppWidgetId, views);
            } else {
                Intent serviceIntent = new Intent(this, HawaRayiService.class);
                serviceIntent.putExtra(HawaRayiService.LOCATION_QUERY_EXTRA, selectedCity);
                startService(serviceIntent);
            }
        } else if (widgetLabel.equals(getString(R.string.widget_label_large))) {
            views = new RemoteViews(getPackageName(), R.layout.widget_layout_large);

            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                    selectedCity, todayStr);
            Cursor cursor = getContentResolver().query(
                    weatherForLocationUri,
                    Utility.WIDGET_FORECAST_COLUMNS,
                    null,
                    null,
                    sortOrder
            );

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
                    String highString = Utility.formatTemperature(this, high);

                    double low = cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));
                    String lowString = Utility.formatTemperature(this, low);

                    if (cursor.getPosition() == 0) {
                        int cityId = cursor.getInt(cursor.getColumnIndex(WeatherContract.LocationEntry.COLUMN_CITY_ID));
                        String city = getResources().getStringArray(R.array.pref_location_options)[cityId];
                        views.setTextViewText(R.id.widget_large_city, city);

                        dateText = Utility.getFormattedMonthDay(this, date);

                        temperature = highString;
                    } else {
                        dateText = Utility.getWeekOfDay(this, date).substring(0, 3);

                        temperature = highString + "/" + lowString;
                    }

                    views.setTextViewText(HawaRayiWidgetProviderLarge.widgetTemperature[cursor.getPosition()], temperature);
                    views.setTextViewText(HawaRayiWidgetProviderLarge.widgetDays[cursor.getPosition()], dateText);
                    views.setImageViewResource(HawaRayiWidgetProviderLarge.widgetIcons[cursor.getPosition()], Utility.getArtResourceForWeatherCondition(weatherId));
                    appWidgetManager.updateAppWidget(mAppWidgetId, views);
                }
            } else {
                Intent serviceIntent = new Intent(this, HawaRayiService.class);
                serviceIntent.putExtra(HawaRayiService.LOCATION_QUERY_EXTRA, selectedCity);
                startService(serviceIntent);
            }
        }
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String newValue = value.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(newValue);
            if (prefIndex >= 0) {
                preference.setTitle(listPreference.getEntries()[prefIndex]);
            }
            if (preference.getKey().equals(getString(R.string.pref_widget_location_key))) {
                selectedCity = newValue;
                cityIndex = prefIndex;
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setTitle(newValue);
        }

        return true;
    }
}
