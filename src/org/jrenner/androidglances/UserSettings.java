package org.jrenner.androidglances;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import org.jrenner.glances.Process;

import java.util.Comparator;

public class UserSettings extends SherlockPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "Glances-UserSettings";
    private static final String UPDATE_INTERVAL = "update_interval";
	private static final String PROCESS_COMP = "process_sort";

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
            MonitorFragment.setUpdateInterval(newInterval);
        }
		else if (key.equals(PROCESS_COMP)) {
			String value = sharedPreferences.getString(key, "cpu_and_mem");
			updateProcessComparator(value);
		}
    }

	private static void updateProcessComparator(String value) {
		Comparator<Process> comp = null;
		switch (value) {
			case "cpu_and_mem":
				comp = ProcessComparators.processCPUPlusMemoryComparator;
				break;
			case "cpu":
				comp = ProcessComparators.processCPUComparator;
				break;
			case "mem":
				comp = ProcessComparators.processMemoryComparator;
				break;
			case "name":
				comp = ProcessComparators.processNameComparator;
				break;
			case "IO":
				comp = ProcessComparators.processIOComparator;
				break;
			default:
				Log.e(TAG, "unhandled process comparator value in update sharedprefs: " + value);
				// default failsafe
				comp = ProcessComparators.processCPUPlusMemoryComparator;

		}
		ProcessComparators.setActiveComparator(comp);
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

	public static void setProcessComparatorFromSettings(SharedPreferences prefs) {
		String value = prefs.getString(PROCESS_COMP, "cpu_and_mem");
		updateProcessComparator(value);
	}
}