package org.jrenner.androidglances;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class UserSettings extends SherlockPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "Glances-UserSettings";
    static final String UPDATE_INTERVAL = "update_interval";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(UPDATE_INTERVAL)) {
            int newInterval = getUpdateIntervalFromSettings(sharedPreferences);
            Log.i(TAG, "User set new update interval for servers: " + newInterval + "ms");
            MonitorFragment.setUpdateInterval(newInterval);
        }
    }

    public static int getUpdateIntervalFromSettings(SharedPreferences prefs) {
        String value = prefs.getString(UPDATE_INTERVAL, "3000");
        Integer newInterval;
        try {
            newInterval = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Log.e(TAG, e.toString());
            newInterval = 3000;
        }
        return newInterval;
    }
}