package com.shagalalab.weather;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * Created by atabek on 8/20/14.
 */
public class CustomListPreference extends ListPreference {
    public CustomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomListPreference(Context context) {
        super(context);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setNegativeButton(R.string.preference_setting_cancel, null);
    }
}
