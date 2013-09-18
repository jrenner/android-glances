package org.jrenner.androidglances;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.jrenner.androidglances.Constants.UPDATE_ERROR;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.jrenner.androidglances.TextSetter.*;

public class MonitorFragment extends Fragment {
    private static final String TAG = "Glances-MonitorFrag";
    private GlancesInstance monitored;
    private static List<GlancesInstance> allGlances;
    private Handler updateHandler;
    // This is how often the monitorFrag checks for updates
    // it is NOT related to the individual server's updateInterval
    private int checkForUpdateInterval = 100;  // milliseconds
    private TextView nameText;
	private TextView versionText;
    private TextView serverAddress;
    private TextView updateTimeText;
    private TextView updateAgeText;
    private TextView systemText;
	private TextView batteryHeader;
	private TextView batteryText;
    private TextView cpuHeader;
    private TextView cpuUsage;
    private ProgressBar pgCpu;
    private TextView cpuLoad;
    private TextView memoryHeader;
    private TextView memory;
    private ProgressBar pgMemory;
    private TextView swapHeader;
    private TextView swap;
    private TextView netHeader;
    private TextView nets;
    private TextView diskIOHeader;
    private TextView diskIO;
    private TextView fsHeader;
    private TextView fileSystems;
    private TextView hddTempHeader;
    private TextView hddTemp;
    private TextView sensorsHeader;
    private TextView sensors;
    private TextView procHeader;
    private TextView processes;
	private TextView monitoredProcessHeader;
	private TextView monitoredProcessText;

    private TextView[] allHeaders;
    private TextView[] allTexts;
    private long lastUpdateTime;
    private long connectStartTime; // time of trying to get first update from a server after start of monitoring

	private LinearLayout monitorTable;

    @Override
    public void onCreate(Bundle onSavedInstance) {
        super.onCreate(onSavedInstance);
        updateHandler = new Handler();
        Log.d(TAG, "monitor fragment created");
        allGlances = new ArrayList<GlancesInstance>();
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        lastUpdateTime = 0;
        connectStartTime = 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.monitor, container, false);
		monitorTable = (LinearLayout) view;

        // assign views
        nameText = (TextView) view.findViewById(R.id.nameText);
		versionText = (TextView) view.findViewById(R.id.versionText);
        serverAddress = (TextView) view.findViewById(R.id.serverAddress);
        updateTimeText = (TextView) view.findViewById(R.id.updateTimeText);
        updateAgeText = (TextView) view.findViewById(R.id.updateAgeText);
        systemText = (TextView) view.findViewById(R.id.systemText);
		batteryHeader = (TextView) view.findViewById(R.id.batteryHeader);
		batteryText = (TextView) view.findViewById(R.id.batteryText);
        cpuHeader = (TextView) view.findViewById(R.id.CPUHeader);
        cpuUsage = (TextView) view.findViewById(R.id.cpuUsage);
        pgCpu = (ProgressBar) view.findViewById(R.id.pgCpu);
        cpuLoad = (TextView) view.findViewById(R.id.cpuLoad);
        memoryHeader = (TextView) view.findViewById(R.id.memoryHeader);
        memory = (TextView) view.findViewById(R.id.memory);
        pgMemory = (ProgressBar) view.findViewById(R.id.pgMemory);
        swapHeader = (TextView) view.findViewById(R.id.swapHeader);
        swap = (TextView) view.findViewById(R.id.swap);
        netHeader = (TextView) view.findViewById(R.id.netHeader);
        nets = (TextView) view.findViewById(R.id.nets);
        fsHeader = (TextView) view.findViewById(R.id.fsHeader);
        fileSystems = (TextView) view.findViewById(R.id.fileSystems);
        diskIOHeader = (TextView) view.findViewById(R.id.diskIOHeader);
        diskIO = (TextView) view.findViewById(R.id.diskIO);
        procHeader = (TextView) view.findViewById(R.id.procHeader);
        processes = (TextView) view.findViewById(R.id.processes);
		monitoredProcessHeader = (TextView) view.findViewById(R.id.monProcHeader);
		monitoredProcessText = (TextView) view.findViewById(R.id.monProcText);
        hddTempHeader = (TextView) view.findViewById(R.id.hddTempHeader);
        hddTemp = (TextView) view.findViewById(R.id.hddTemp);
        sensorsHeader = (TextView) view.findViewById(R.id.sensorsHeader);
        sensors = (TextView) view.findViewById(R.id.sensors);

        allHeaders = new TextView[]{nameText, cpuHeader, memoryHeader, swapHeader, netHeader, fsHeader, diskIOHeader,
                     procHeader, hddTempHeader, sensorsHeader, batteryHeader, monitoredProcessHeader};
        allTexts = new TextView[]{updateTimeText, systemText, cpuUsage, cpuLoad, memory, swap, nets, diskIO, fileSystems,
                   hddTemp, sensors, processes, batteryText, monitoredProcessText};
        return view;
    }

    public void resetUpdateTime() {
        lastUpdateTime = 0;
    }

    /**
     * called from UserSettings when user changes update interval preference
     * @param newInterval
     */
    public static void setUpdateInterval(int newInterval) {
        for (GlancesInstance server: allGlances) {
            server.setUpdateInterval(newInterval);
        }
        resetUpdateTimers();
    }

    public void clearTextValues() {
        for (TextView tv : allTexts) {
            if (tv != null) {
                tv.setText("");
            }
        }
    }

    public void clearHeaderValues() {
        for (TextView tv: allHeaders) {
            if (tv != null) {
                tv.setText("");
            }
            pgMemory.setVisibility(View.INVISIBLE);
        }
    }

    public List<GlancesInstance> getAllGlancesServers() {
        return allGlances;
    }

    public String[] getServerNames() {
        String[] result = new String[allGlances.size()];
        int i = 0;
        for (GlancesInstance server : allGlances) {
            result[i++] = server.nickName;
        }
        return result;
    }

    /**
     * Returns server that was added
     * @param urltext
     * @param nickName
     * @param password
     * @return
     */
    public GlancesInstance addServerToList(String urltext, String nickName, String password) {
        URL url = null;
        GlancesInstance newServer = null;
        try {
            url = new URL(urltext);
            Log.v(TAG, "Adding Glances server to list: " + urltext + " - " + nickName);
            newServer = new GlancesInstance(url, nickName, password);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString());

            Toast.makeText(getActivity(), String.format(getString(R.string.invalid_url), urltext), Toast.LENGTH_LONG).show();
            return null;
        }
        newServer.setTimeout(15); // timeout after n seconds
        int updateInterval = UserSettings.getUpdateIntervalFromSettings(PreferenceManager.getDefaultSharedPreferences(getActivity()));
        newServer.setUpdateInterval(updateInterval);
        allGlances.add(newServer);
        redrawActionBar();
        return newServer;
    }

    void redrawActionBar() {
        //Log.v(TAG, "Menu item changed - invalidating action bar to cause redraw");
        getActivity().supportInvalidateOptionsMenu();
    }

    /**
     * return true if removed
     */
    public boolean removeServerFromList(String nickName) {
        //TODO refactor this with getServerByName method
        for (GlancesInstance server : allGlances) {
            if (server.nickName.equals(nickName)) {
                Log.i(TAG, "Removing server: " + nickName);
                Activity activity = getActivity();
                activity.getPreferences(Activity.MODE_PRIVATE).edit().remove(nickName).commit();
                activity.getSharedPreferences("serverPasswords", Activity.MODE_PRIVATE).edit().remove(nickName).commit();
                allGlances.remove(server);
                doNotMonitor();
                redrawActionBar();
                return true;
            }
        }
        return false;
    }

    public GlancesInstance getServerByName(String name) {
        for (GlancesInstance server : allGlances) {
            if (server.nickName.equals(name)) {
                return server;
            }
        }
        return null;
    }

    public void setServer(String urltext, String serverNickName) {
        hideProgressBars();
        lastUpdateTime = 0;
        GlancesInstance selection = null;
        for (GlancesInstance server : allGlances) {
            if (server.nickName.equals(serverNickName) && server.url.toString().equals(urltext)) {
                selection = server;
            }
        }
        if (selection == null) {
            Log.e(TAG, "Couldn't find server with name - " + serverNickName);
            return;
        }
        clearHeaderValues();
        clearTextValues();
        monitored = selection;
        serverAddress.setText(monitored.url.getHost() + " : " + monitored.url.getPort());
        updateAgeText.setText(getString(R.string.connect_to_server));
        connectStartTime = System.currentTimeMillis();
        nameText.setText("");
    }

    public void setServer(GlancesInstance server) {
        setServer(server.url.toString(), server.nickName);
    }

    public void doNotMonitor() {
        clearHeaderValues();
        clearTextValues();
        lastUpdateTime = 0;
        serverAddress.setText(getString(R.string.no_server_selected));
        updateAgeText.setText("");
        monitored = null;
    }

    private void update() {
        if (monitored == null) {
            hideProgressBars();
            //Log.v(TAG, "No server being monitored, nothing to do.");
            return;
        }
        monitored.update();
        if (!monitored.isUpdateWaiting()) {
            //Log.v(TAG, "No update waiting for monitored server: " + monitored.nickName);
            if (lastUpdateTime != 0) {
                long updateAge = System.currentTimeMillis() - lastUpdateTime;
                String monitorStatus = String.format(getString(R.string.monitor_status), Tools.convertToHumanTime(updateAge),
                        Tools.convertToHumanTime(monitored.getUpdateInterval()));
                updateAgeText.setText(monitorStatus);
            } else {
                long waitTime = System.currentTimeMillis() - connectStartTime;
                updateAgeText.setText(getString(R.string.connect_to_server) + " " + Tools.convertToHumanTime(waitTime));
            }
        } else {
            nameText.setText(monitored.nickName);

			versionText.setText("server glances version: " + monitored.getGlancesServerVersion());

            setNow(updateTimeText, monitored.now);

            setSystemInfo(systemText, monitored.systemInfo);

			boolean hasBatteries = setBatteries(batteryHeader, batteryText, monitored.batteries);
			if (hasBatteries) {
				addIfNotInLayout(monitorTable, batteryHeader);
				addIfNotInLayout(monitorTable, batteryText);
			} else {
				monitorTable.removeView(batteryHeader);
				monitorTable.removeView(batteryText);
			}

            setCPUHeader(cpuHeader, monitored.cores);

            setCPUUsage(cpuUsage, monitored.cpu, pgCpu);

            setCPULoad(cpuLoad, monitored.load);

            setMemory(memoryHeader, memory, monitored.memory, pgMemory);

            setSwap(swapHeader, swap, monitored.memorySwap);

            boolean hasNet = setNetworks(netHeader, nets, monitored.netInterfaces);
			if (hasNet) {
				addIfNotInLayout(monitorTable, netHeader);
				addIfNotInLayout(monitorTable, nets);
			} else {
				monitorTable.removeView(netHeader);
				monitorTable.removeView(nets);
			}

            setFileSystems(fsHeader, fileSystems, monitored.fileSystems);

            setDiskIO(diskIOHeader, diskIO, monitored.diskIO);

			boolean hasMonProcs = setMonitoredProcesses(monitoredProcessHeader, monitoredProcessText,
					monitored.monitoredProcesses, monitored.processes);
			if (hasMonProcs) {
				addIfNotInLayout(monitorTable, monitoredProcessHeader);
				addIfNotInLayout(monitorTable, monitoredProcessText);
			} else {
				monitorTable.removeView(monitoredProcessHeader);
				monitorTable.removeView(monitoredProcessText);
			}

            boolean hasProcs = setProcesses(procHeader, processes, monitored.processes);
			if (hasProcs) {
				addIfNotInLayout(monitorTable, procHeader);
				addIfNotInLayout(monitorTable, processes);
			} else {
				monitorTable.removeView(procHeader);
				monitorTable.removeView(processes);
			}

            boolean hasSensors = setSensors(sensorsHeader, sensors, monitored.sensors);
			if (hasSensors) {
				addIfNotInLayout(monitorTable, sensorsHeader);
				addIfNotInLayout(monitorTable, sensors);
			} else {
				monitorTable.removeView(sensorsHeader);
				monitorTable.removeView(sensors);
			}

            boolean hasDriveTemps = setHDDTemp(hddTempHeader, hddTemp, monitored.hddTemps);
			if (hasDriveTemps) {
				addIfNotInLayout(monitorTable, hddTempHeader);
				addIfNotInLayout(monitorTable, hddTemp);
			} else {
				monitorTable.removeView(hddTempHeader);
				monitorTable.removeView(hddTemp);
			}
            
            //Log.v(TAG, "Got update from monitored server: " + monitored.nickName + " - " + monitored.now.toString());
            lastUpdateTime = System.currentTimeMillis();
            monitored.setUpdateWaiting(false); // we processed this update already, so set false and wait for next update
        }
		for (int i = 0; i < monitorTable.getChildCount(); i++) {
			View v = monitorTable.getChildAt(i);
			if (!(v instanceof TextView)) {
				continue;
			}
			TextView tv = (TextView) v;
			if (tv.getText().toString().contains("data")) {
				Log.e(TAG, "found rogue text view: " + tv.toString() + ", " + tv.getId());
			}
		}
        handleErrors();
    }

    Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            update();
            updateHandler.postDelayed(updateTimer, checkForUpdateInterval);
        }
    };

    public void startUpdates() {
        Log.d(TAG, "startUpdates");
        updateTimer.run();
    }

    public void stopUpdates() {
        Log.d(TAG, "stopUpdates");
        updateHandler.removeCallbacks(updateTimer);
    }

    public void removeAllServers() {
        allGlances.clear();
    }

    public void handleErrors() {
        UPDATE_ERROR err = monitored.getErrorCode();
        if (err != null) {
            if (err != UPDATE_ERROR.UNDEFINED) {
                // don't clear the screen on undefined error, it could be a very small error, just keep going as usual
                clearHeaderValues();
                clearTextValues();
                hideProgressBars();
            }
            String errMsg = monitored.getErrorText(err);
            updateTimeText.setText(errMsg);
            monitored.setErrorCode(null);
        }
    }

    public GlancesInstance getMonitoredServer() {
        return monitored;
    }

    public static void resetUpdateTimers() {
        for (GlancesInstance server : allGlances) {
            server.resetUpdateTimer();
        }
    }

    public void hideProgressBars() {
        ProgressBar[] bars = {pgCpu, pgMemory};
        for (ProgressBar bar : bars) {
            bar.setVisibility(View.INVISIBLE);
        }
    }

	private static boolean hasView(LinearLayout layout, View view) {
		return layout.findViewById(view.getId()) != null;
	}

	private static void addIfNotInLayout(LinearLayout layout, View view) {
		if (!hasView(layout, view)) {
			layout.addView(view);
		}
	}
}
