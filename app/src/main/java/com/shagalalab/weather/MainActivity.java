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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private boolean mTwoPane;
    private boolean mProgressBarState = false;
    private String uiInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.w(LOG_TAG, "onCreate");

        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        uiInterface = prefs.getString(getString(R.string.pref_interface_key),
                getString(R.string.pref_interface_default));

        if (!uiInterface.equals(getString(R.string.pref_interface_default))) {
            Utility.changeLocale(this);
        }

        setContentView(R.layout.activity_main);
        if (findViewById(R.id.weather_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, new DetailFragment())
                    .commit();
        }

        ForecastFragment forecastFragment = ((ForecastFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_forecast));
        forecastFragment.setUseTodayLayout(!mTwoPane);
    }

    @Override
    protected void onPause() {
        Log.w(LOG_TAG, "onPause");
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onResume() {
        Log.w(LOG_TAG, "onResume");
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(Utility.COMMUNICATE_WITH_MAIN_INTENT_FILTER));

        Log.w(LOG_TAG, "onResume, needRestart -> " + Utility.NEED_RESTART);
        if (Utility.NEED_RESTART) {
            Utility.NEED_RESTART = false;
            finish();
            Utility.restartApp(this, getIntent());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.w(LOG_TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);

        if (!uiInterface.equals(getString(R.string.pref_interface_default))) {
            Utility.changeLocale(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(String date, String location) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putString(DetailActivity.DATE_KEY, date);
            args.putString(DetailActivity.LOCATION_KEY, location);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, fragment)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .putExtra(DetailActivity.DATE_KEY, date)
                    .putExtra(DetailActivity.LOCATION_KEY, location);
            startActivity(intent);
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra(Utility.MESSAGE);
            if (message.equals(Utility.HIDE_PROGRESS_BAR)) {
                setProgress(false);
            }
        }
    };

    public void setProgress(boolean state) {
        mProgressBarState = state;
        setSupportProgressBarIndeterminateVisibility(mProgressBarState);
    }

    public boolean getProgressBarState() {
        return mProgressBarState;
    }
}
