package com.babelsoft.babelradio;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;

public class SettingsActivity extends PreferenceActivity {
    private AppCompatDelegate mDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        setupActionBar();
//        initiatePreferenceChangeListener();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Settings");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    private void initiatePreferenceChangeListener() {

        final CheckBoxPreference checkBoxPreference = (CheckBoxPreference)findPreference("RUN_SERVICE_AFTER_A2DP_CONNECTED");
        ListPreference listPreference = (ListPreference)findPreference("A2DP_DISPLAY_UPDATE_TIME");

        Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                switch(preference.getKey()) {
                    case "perform_sync":
                        if (checkBoxPreference.isChecked()) checkBoxPreference.setChecked(false);
                        else checkBoxPreference.setChecked(true);
                        break;
                    case "sync_interval":
                        int value = Integer.parseInt(newValue.toString());
                        break;
                }
            return true;
            }

        };

        checkBoxPreference.setOnPreferenceChangeListener(changeListener);
        listPreference.setOnPreferenceChangeListener(changeListener);
    }

}