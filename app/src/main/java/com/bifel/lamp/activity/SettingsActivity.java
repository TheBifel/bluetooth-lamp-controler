package com.bifel.lamp.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.bifel.lamp.R;

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_activity);
    }
}
