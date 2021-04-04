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
        // TODO To be used in later stage
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

    // TODO To be used in later stage
    private void initiatePreferenceChangeListener() {

        final CheckBoxPreference checkBoxPreference = (CheckBoxPreference)findPreference(Settings.RUN_SERVICE_AFTER_A2DP_CONNECTED.name());
        ListPreference listPreference = (ListPreference)findPreference(Settings.A2DP_DISPLAY_UPDATE_TIME.name());

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