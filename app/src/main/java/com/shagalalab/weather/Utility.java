/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shagalalab.weather;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.shagalalab.weather.data.WeatherContract;
import com.shagalalab.weather.receiver.AlarmReceiver;
import com.shagalalab.weather.receiver.BootReceiver;
import com.shagalalab.weather.service.HawaRayiService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utility {
    public static String COMMUNICATE_WITH_MAIN_INTENT_FILTER = "COMMUNICATE_WITH_MAIN_INTENT_FILTER";
    public static String MESSAGE = "message";
    public static String HIDE_PROGRESS_BAR = "hide_progressbar";
    public static boolean NEED_RESTART = false;
    public static String UPDATE_WIDGET = "updateWidget";
    public static String APP_WIDGET_ID = "appWidgetId";
    private static int NOTIFICATION_ID = 3456;

    public static final String[] WIDGET_FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            // This works because the WeatherProvider returns location data joined with
            // weather data, even though they're stored in two different tables.
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.LocationEntry.COLUMN_CITY_ID
    };

    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    public static String formatTemperature(Context context, double temperature) {
        return context.getString(R.string.format_temperature, temperature);
    }

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users.  As classy and polished a user experience as "20140102" is, we can do better.
     *
     * @param context Context to use for resource localization
     * @param dateStr The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return a user-friendly representation of the date.
     */
    public static String getFriendlyDayString(Context context, String dateStr) {
        // The day string for forecast uses the following logic:
        // For today: "Today, June 8"
        // For tomorrow:  "Tomorrow"
        // For the next 5 days: "Wednesday" (just the day name)
        // For all days after that: "Mon Jun 8"

        Date todayDate = new Date();
        String todayStr = getDbDateString(todayDate);

        // If the date we're building the String for is today's date, the format
        // is "Today, June 24"
        if (todayStr.equals(dateStr)) {
            String today = context.getString(R.string.today);
            int formatId = R.string.format_full_friendly_date;
            return String.format(context.getString(
                    formatId,
                    today,
                    getFormattedMonthDay(context, dateStr)));
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTime(todayDate);
            cal.add(Calendar.DATE, 7);
            String weekFutureString = getDbDateString(cal.getTime());

            if (dateStr.compareTo(weekFutureString) < 0) {
                // If the input date is less than a week in the future, just return the day name.
                return getDayName(context, dateStr);
            } else {
                int formatId = R.string.format_full_friendly_date;
                return String.format(context.getString(
                        formatId,
                        getFormattedMonthDay(context, dateStr),
                        getDayName(context, dateStr).toLowerCase()));
            }
        }
    }

    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "wednesday".
     *
     * @param context Context to use for resource localization
     * @param dateStr The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return
     */
    public static String getDayName(Context context, String dateStr) {
        Date todayDate = new Date();
        if (getDbDateString(todayDate).equals(dateStr)) {
            return context.getString(R.string.today);
        } else {
            // If the date is set for tomorrow, the format is "Tomorrow".
            Calendar cal = Calendar.getInstance();
            cal.setTime(todayDate);
            cal.add(Calendar.DATE, 1);
            Date tomorrowDate = cal.getTime();
            if (getDbDateString(tomorrowDate).equals(dateStr)) {
                return context.getString(R.string.tomorrow);
            } else {
                // Otherwise, the format is just the day of the week (e.g "Wednesday".
                Date date = getDateFromDb(dateStr);
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                String[] dayOfWeeks = context.getResources().getStringArray(R.array.day_of_week);
                return dayOfWeeks[c.get(Calendar.DAY_OF_WEEK) - 1];
            }
        }
    }

    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     * @param context Context to use for resource localization
     * @param dateStr The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(Context context, String dateStr) {
        Date date = getDateFromDb(dateStr);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        String[] months = context.getResources().getStringArray(R.array.months);
        return c.get(Calendar.DAY_OF_MONTH) + "-" + months[c.get(Calendar.MONTH)];
    }

    public static String getWeekOfDay(Context context, String dateStr) {
        Date date = getDateFromDb(dateStr);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        String[] dayOfWeeks = context.getResources().getStringArray(R.array.day_of_week);
        return dayOfWeeks[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static String getFormattedWind(Context context, float windSpeed, float degrees) {
        int windFormat;
        windFormat = R.string.format_wind_kmh;

        // From wind direction in degrees, determine compass direction as a string (e.g NW)
        // You know what's fun, writing really long if/else statements with tons of possible
        // conditions.  Seriously, try it!
        String direction = "Unknown";
        if (degrees >= 337.5 || degrees < 22.5) {
            direction = context.getResources().getString(R.string.wind_direction_north);
        } else if (degrees >= 22.5 && degrees < 67.5) {
            direction = context.getResources().getString(R.string.wind_direction_north_east);
        } else if (degrees >= 67.5 && degrees < 112.5) {
            direction = context.getResources().getString(R.string.wind_direction_east);
        } else if (degrees >= 112.5 && degrees < 157.5) {
            direction = context.getResources().getString(R.string.wind_direction_south_east);
        } else if (degrees >= 157.5 && degrees < 202.5) {
            direction = context.getResources().getString(R.string.wind_direction_south);
        } else if (degrees >= 202.5 && degrees < 247.5) {
            direction = context.getResources().getString(R.string.wind_direction_south_west);
        } else if (degrees >= 247.5 && degrees < 292.5) {
            direction = context.getResources().getString(R.string.wind_direction_west);
        } else if (degrees >= 292.5 || degrees < 22.5) {
            direction = context.getResources().getString(R.string.wind_direction_north_west);
        }
        return String.format(context.getString(windFormat), windSpeed, direction);
    }

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    public static int getNotificationIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_noti_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_noti_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_noti_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_noti_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_noti_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_noti_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_noti_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_noti_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_noti_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_noti_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_noti_cloudy;
        }
        return -1;
    }

    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding image. -1 if no relation is found.
     */
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.art_rain;
        } else if (weatherId == 511) {
            return R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.art_rain;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.art_storm;
        } else if (weatherId == 800) {
            return R.drawable.art_clear;
        } else if (weatherId == 801) {
            return R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.art_clouds;
        }
        return -1;
    }

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static String getWeatherCondition(Resources res, int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return res.getString(R.string.storm);
        } else if (weatherId >= 300 && weatherId <= 321) {
            return res.getString(R.string.light_rain);
        } else if (weatherId >= 500 && weatherId <= 504) {
            return res.getString(R.string.rain);
        } else if (weatherId == 511) {
            return res.getString(R.string.snow);
        } else if (weatherId >= 520 && weatherId <= 531) {
            return res.getString(R.string.rain);
        } else if (weatherId >= 600 && weatherId <= 622) {
            return res.getString(R.string.snow);
        } else if (weatherId >= 701 && weatherId <= 761) {
            return res.getString(R.string.fog);
        } else if (weatherId == 761 || weatherId == 781) {
            return res.getString(R.string.storm);
        } else if (weatherId == 800) {
            return res.getString(R.string.clear);
        } else if (weatherId == 801) {
            return res.getString(R.string.light_clouds);
        } else if (weatherId >= 802 && weatherId <= 804) {
            return res.getString(R.string.cloudy);
        }
        return "";
    }

    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c <= '/' || c >= ':') {
                return false;
            }
        }
        return true;
    }

    public static int getCityIndex(Resources res, String location) {
        String[] cityValues = res.getStringArray(R.array.pref_location_values);
        return Arrays.asList(cityValues).indexOf(location);
    }

    public static float getConvertedPressure(float rawPressure) {
        return rawPressure * 0.750063f;
    }

    public static void restartApp(Context context, Intent intent) {
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId,
                intent, PendingIntent.FLAG_CANCEL_CURRENT|PendingIntent.FLAG_ONE_SHOT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }

    public static void changeLocale(Context context) {
        Locale locale = new Locale("pt");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    /**
     * Converts Date class to a string representation, used for easy comparison and database lookup.
     * @param date The input date
     * @return a DB-friendly representation of the date, using the format defined in DATE_FORMAT.
     */
    public static String getDbDateString(Date date){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat sdf = new SimpleDateFormat(WeatherContract.DATE_FORMAT);
        return sdf.format(date);
    }

    /**
     * Converts a dateText to a long Unix time representation
     * @param dateText the input date string
     * @return the Date object
     */
    public static Date getDateFromDb(String dateText) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(WeatherContract.DATE_FORMAT);
        try {
            return dbDateFormat.parse(dateText);
        } catch ( ParseException e ) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getFormattedLastUpdate(Context context, Long timestamp) {
        Date date = new Date(timestamp);
        TimeZone tz = TimeZone.getDefault();
        Calendar c = Calendar.getInstance(tz);
        c.setTime(date);
        String[] months = context.getResources().getStringArray(R.array.months);
        return String.format("%02d:%02d, %s-%s", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                c.get(Calendar.DAY_OF_MONTH), months[c.get(Calendar.MONTH)]);
    }

    public static void insertWidgetLocationInDatabase(Context context, String locationSetting, int appWidgetId) {

        // First, check if the location with this city name exists in the db
        Cursor cursor = context.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_WIDGET_URI,
                new String[]{WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING},
                WeatherContract.LocationEntry.COLUMN_APP_WIDGET_ID + " = ?",
                new String[]{Integer.toString(appWidgetId)},
                null);

        if (!cursor.moveToFirst()) {
            ContentValues locationValues = new ContentValues();
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_APP_WIDGET_ID, appWidgetId);

            Uri locationInsertUri = context.getContentResolver()
                    .insert(WeatherContract.LocationEntry.CONTENT_WIDGET_URI, locationValues);
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    public static String getWidgetLocation(Context context, int appWidgetId) {
        String result;

        // First, check if the location with this city name exists in the db
        Cursor cursor = context.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_WIDGET_URI,
                new String[]{WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING},
                WeatherContract.LocationEntry.COLUMN_APP_WIDGET_ID + " = ?",
                new String[]{Integer.toString(appWidgetId)},
                null);

        if (cursor.moveToFirst()) {
            int locationSettingIdx = cursor.getColumnIndex(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING);
            result = cursor.getString(locationSettingIdx);
        } else {
            result = null;
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return result;
    }

    public static void showNotification(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String locationKey = context.getString(R.string.pref_location_key);

        // Getting saved location
        String locationDefault = context.getString(R.string.pref_location_default);
        String location = prefs.getString(locationKey, locationDefault);

        // Get all saved data for location
        Date todayDate = new Date();
        Cursor cursor = Utility.getForecastCursor(context, location, todayDate);

        if (cursor != null && cursor.moveToFirst()) {
            int weatherId = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));

            String date = cursor.getString(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATETEXT));
            String dateText = Utility.getFormattedMonthDay(context, date);

            int cityId = cursor.getInt(cursor.getColumnIndex(WeatherContract.LocationEntry.COLUMN_CITY_ID));
            String city = context.getResources().getStringArray(R.array.pref_location_options)[cityId];

            double high = cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP));
            String highString = Utility.formatTemperature(context, high);

            double low = cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));
            String lowString = Utility.formatTemperature(context, low);

            int iconId = Utility.getNotificationIconResourceForWeatherCondition(weatherId);
            String title = context.getString(R.string.format_notification_title, city);

            String notificationText = context.getString(R.string.format_notification, dateText, highString + "C", lowString + "C");

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                            .setSmallIcon(iconId)
                            .setContentTitle(title)
                            .setContentText(notificationText);
            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(context, MainActivity.class);

            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        // enabling receiver to show notification after reboot
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static void hideNotification(Context context) {
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);

        // disabling receiver to show notification after reboot
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static void showNotificationIfEnabled(Context context) {
        // show notification if enabled in preferences...
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean defaultForNotifications =
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default));
        boolean notificationsEnabled =
                prefs.getBoolean(displayNotificationsKey, defaultForNotifications);

        if (notificationsEnabled) {
            showNotification(context);
        }
    }

    public static Cursor getForecastCursor(Context context, String location, Date date) {
        String dateStr = Utility.getDbDateString(date);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                location, dateStr);
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";
        return context.getContentResolver().query(
                weatherForLocationUri,
                Utility.WIDGET_FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    private static void setAlarm(Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        // Set the alarm to start at 00:01
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 1);

        alarmMgr.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);
    }

    public static void setAlarmIfNotRunning(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        boolean alarmUp = (PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE) != null);

        if (!alarmUp) {
            setAlarm(context);
        }
    }

    public static void cancelAlarm(Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        // If the alarm has been set, cancel it.
        if (alarmMgr!= null) {
            alarmMgr.cancel(alarmIntent);
        }
    }

    public static void updateWeatherData(Context context) {
        Uri locationUri = WeatherContract.LocationEntry.CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(
                locationUri,
                new String[] { WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING },
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String location = cursor.getString(cursor.getColumnIndex(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING));
            String startDate = Utility.getDbDateString(new Date());

            // Sort order:  Ascending, by date.
            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                    location, startDate);

            Cursor cursorLocation = context.getContentResolver().query(
                    weatherForLocationUri,
                    new String[] { WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING },
                    null,
                    null,
                    sortOrder
            );

            if (cursorLocation.getCount() < 4) {
                Intent serviceIntent = new Intent(context, HawaRayiService.class);
                serviceIntent.putExtra(HawaRayiService.LOCATION_QUERY_EXTRA, location);
                context.startService(serviceIntent);
            }

            if (cursorLocation != null && !cursorLocation.isClosed()) {
                cursorLocation.close();
            }
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }
}
