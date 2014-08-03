package com.shagalalab.weather.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.shagalalab.weather.R;

/**
 * Created by atabek on 08/03/14.
 */
public class HawaRayiWidgetConfigure extends Activity {
    int mAppWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.widget_configure);

        // First, get the App Widget ID from the Intent that launched the Activity:
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Perform your App Widget configuration.

        // When the configuration is complete, get an instance of the AppWidgetManager by calling getInstance(Context):
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        // Update the App Widget with a RemoteViews layout by calling updateAppWidget(int, RemoteViews):
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_layout);
        appWidgetManager.updateAppWidget(mAppWidgetId, views);

        // Finally, create the return Intent, set it with the Activity result, and finish the Activity:
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
