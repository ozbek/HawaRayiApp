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
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.shagalalab.weather.data.WeatherContract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utility {
    public static String COMMUNICATE_WITH_MAIN_INTENT_FILTER = "COMMUNICATE_WITH_MAIN_INTENT_FILTER";
    public static String MESSAGE = "message";
    public static String HIDE_PROGRESS_BAR = "hide_progressbar";
    public static boolean NEED_RESTART = false;
    public static String UPDATE_WIDGET = "updateWidget";
    public static String APP_WIDGET_ID = "appWidgetId";

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

    public static String getPreferredWidgetLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_widget_location_key),
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
        int index = Arrays.asList(cityValues).indexOf(location);
        return index;
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
        Locale locale = new Locale("ru");
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
}
