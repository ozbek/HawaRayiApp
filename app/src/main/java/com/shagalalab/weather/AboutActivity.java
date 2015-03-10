package com.shagalalab.weather;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends ActionBarActivity {
    private static String WEB_SITE = "http://www.shagalalab.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        TextView tvDescription = (TextView) findViewById(R.id.description);
        tvDescription.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
