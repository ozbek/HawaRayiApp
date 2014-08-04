package com.shagalalab.weather.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.shagalalab.weather.R;
import com.shagalalab.weather.Utility;

/**
 * Created by atabek on 08/03/14.
 */
public class HawaRayiWidgetConfigure extends Activity {
    int mAppWidgetId;
    private Button createWidgetButton;
    private TextView editCity;

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
        editCity = (TextView) findViewById(R.id.widget_conf_city);
        editCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertListDialog dialogFragment = new AlertListDialog();
                dialogFragment.show(getFragmentManager(), "dialog");
            }
        });

        // When the configuration is complete, get an instance of the AppWidgetManager by calling getInstance(Context):
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        // Update the App Widget with a RemoteViews layout by calling updateAppWidget(int, RemoteViews):
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_layout);
        views.setTextViewText(R.id.widget_city, "Nukus");
        views.setTextViewText(R.id.widget_today_temp, "35");
        views.setImageViewResource(R.id.widget_today_icon, Utility.getArtResourceForWeatherCondition(500));
        appWidgetManager.updateAppWidget(mAppWidgetId, views);

        createWidgetButton = (Button) findViewById(R.id.widget_create);
        createWidgetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Finally, create the return Intent, set it with the Activity result, and finish the Activity:
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
    }

    public class AlertListDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle("Select city")
                    .setItems(R.array.pref_location_options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            Toast.makeText(getActivity(), "Selected: " + which, Toast.LENGTH_LONG).show();
                        }
                    })
                    .create();
        }
    }
}
