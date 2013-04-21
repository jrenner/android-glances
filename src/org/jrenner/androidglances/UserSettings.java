package org.jrenner.androidglances;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

/**
 * Created with IntelliJ IDEA.
 * User: jrenner
 * Date: 4/18/13
 * Time: 1:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserSettings {
    private static long serverUpdateInterval;

    public static void setServerUpdateInterval(long interval) {
        serverUpdateInterval = interval;
    }

    public static long getServerUpdateInterval() {
        return serverUpdateInterval;
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }
}
