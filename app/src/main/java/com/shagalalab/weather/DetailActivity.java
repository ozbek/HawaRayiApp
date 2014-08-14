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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class DetailActivity extends ActionBarActivity {
    private final String LOG_TAG = DetailActivity.class.getSimpleName();
    public static final String DATE_KEY = "forecast_date";
    public static final String LOCATION_KEY = "forecast_location";
    private String uiInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w(LOG_TAG, "onCreate - start");
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        uiInterface = prefs.getString(getString(R.string.pref_interface_key),
                getString(R.string.pref_interface_default));

        if (!uiInterface.equals(getString(R.string.pref_interface_default))) {
            Utility.changeLocale(this);
        }

        setContentView(R.layout.activity_detail);

        getSupportActionBar().setTitle(R.string.title_activity_detail);

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            String date = getIntent().getStringExtra(DATE_KEY);
            String location = getIntent().getStringExtra(LOCATION_KEY);

            Bundle arguments = new Bundle();
            arguments.putString(DetailActivity.DATE_KEY, date);
            arguments.putString(DetailActivity.LOCATION_KEY, location);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.weather_detail_container, fragment)
                    .commit();
        }
        Log.w(LOG_TAG, "onCreate - end");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
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
    protected void onResume() {
        Log.w(LOG_TAG, "onResume");
        super.onResume();

        if (Utility.NEED_RESTART) {
            finish();
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
}
