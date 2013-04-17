package org.jrenner.androidglances;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockActivity;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jrenner.androidglances.TextSetter.*;
import static org.jrenner.androidglances.Constants.*;

public class MonitorFragment extends Fragment {
    private static MonitorFragment instance;
    private static final String TAG = "Glances-MonitorFrag";
    private GlancesInstance monitored;
    private List<GlancesInstance> allGlances;
    private Handler updateHandler;
    private int updateInterval = 1000;

    private TextView nameText;
    private TextView serverAddress;
    private TextView updateTimeText;
    private TextView updateAgeText;
    private TextView systemText;
    private TextView cpuHeader;
    private TextView cpuUsage;
    private TextView cpuLoad;
    private TextView memoryHeader;
    private TextView memory;
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

    private TextView[] allHeaders;
    private TextView[] allTexts;
    private Toast startUpdatesToast;
    private Toast stopUpdatesToast;
    private long lastUpdateTime;
    private long connectStartTime; // time of trying to get first update from a server after start of monitoring
    private static final String connectText = "Trying to connect to server";

    private MonitorFragment() {

    }

    public static MonitorFragment getInstance() {
        if (instance == null) {
            instance = new MonitorFragment();
        }
        return instance;
    }

    @Override
    public void onCreate(Bundle onSavedInstance) {
        super.onCreate(onSavedInstance);
        updateHandler = new Handler();
        Log.d(TAG, "monitor fragment created");
        startUpdatesToast = Toast.makeText(getActivity().getApplicationContext(), "Started updates", Toast.LENGTH_SHORT);
        stopUpdatesToast = Toast.makeText(getActivity().getApplicationContext(), "Stopped updates", Toast.LENGTH_SHORT);
        allGlances = new ArrayList<GlancesInstance>();

        startUpdates();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.monitor, container, false);

        // assign views
        nameText = (TextView) view.findViewById(R.id.nameText);
        serverAddress = (TextView) view.findViewById(R.id.serverAddress);
        updateTimeText = (TextView) view.findViewById(R.id.updateTimeText);
        updateAgeText = (TextView) view.findViewById(R.id.updateAgeText);
        systemText = (TextView) view.findViewById(R.id.systemText);
        cpuHeader = (TextView) view.findViewById(R.id.CPUHeader);
        cpuUsage = (TextView) view.findViewById(R.id.cpuUsage);
        cpuLoad = (TextView) view.findViewById(R.id.cpuLoad);
        memoryHeader = (TextView) view.findViewById(R.id.memoryHeader);
        memory = (TextView) view.findViewById(R.id.memory);
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
        hddTempHeader = (TextView) view.findViewById(R.id.hddTempHeader);
        hddTemp = (TextView) view.findViewById(R.id.hddTemp);
        sensorsHeader = (TextView) view.findViewById(R.id.sensorsHeader);
        sensors = (TextView) view.findViewById(R.id.sensors);

        allHeaders = new TextView[]{nameText, cpuHeader, memoryHeader, swapHeader, netHeader, fsHeader, diskIOHeader,
                     procHeader, hddTempHeader, sensorsHeader};
        allTexts = new TextView[]{updateTimeText, systemText, cpuUsage, cpuLoad, memory, swap, nets, diskIO, fileSystems,
                   hddTemp, sensors, processes};
        return view;
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
            Toast.makeText(getActivity(), "Invalid URL: " + urltext, Toast.LENGTH_LONG).show();
            return null;
        }
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

    public void setServer(String urltext, String serverNickName) {
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
        updateAgeText.setText(connectText);
        connectStartTime = System.currentTimeMillis();
        nameText.setText("");
    }

    public void setServer(GlancesInstance server) {
        setServer(server.url.toString(), server.nickName);
    }

    public void doNotMonitor() {
        clearHeaderValues();
        clearTextValues();
        serverAddress.setText("No server selected");
        updateAgeText.setText("");
        monitored = null;
    }

    void threadReport() {
        int count = 0;
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            String text = thread.toString();
            if (text.contains("AsyncTask"))
                count++;
        }
        Log.v(TAG, "AsyncTasks: " + count);
    }

    private void update() {
        //threadReport();

        // This code is for updating multiple servers concurrently, which is currently disabled
      /*for (GlancesInstance server : allGlances) {
            if (!server.isUpdateExecuting()) {
                server.update();
            }
        }*/
        if (monitored == null) {
            //Log.v(TAG, "No server being monitored, nothing to do.");
            return;
        }
        monitored.update();
        if (!monitored.isUpdateWaiting()) {
            //Log.v(TAG, "No update waiting for monitored server: " + monitored.nickName);
            if (lastUpdateTime != 0) {
                long monitorTime = System.currentTimeMillis() - monitored.monitorStartTime;
                long updateAge = System.currentTimeMillis() - lastUpdateTime;
                updateAgeText.setText(Tools.convertToHumanTime(updateAge) + " old, monitored for " + Tools.convertToHumanTime(monitorTime));
            } else {
                long waitTime = System.currentTimeMillis() - connectStartTime;
                updateAgeText.setText(connectText + " " + Tools.convertToHumanTime(waitTime));
            }
        } else {
            nameText.setText(monitored.nickName);
            setNow(updateTimeText, monitored.now);
            setSystemInfo(systemText, monitored.systemInfo);
            setCPUHeader(cpuHeader, monitored.cores);
            setCPUUsage(cpuUsage, monitored.cpu);
            setCPULoad(cpuLoad, monitored.load);
            setMemory(memoryHeader, memory, monitored.memory);
            setSwap(swapHeader, swap, monitored.memorySwap);
            setNetworks(netHeader, nets, monitored.netInterfaces);
            setFileSystems(fsHeader, fileSystems, monitored.fileSystems);
            setDiskIO(diskIOHeader, diskIO, monitored.diskIO);
            setProcesses(procHeader, processes, monitored.processes);
            setSensors(sensorsHeader, sensors, monitored.sensors);
            setHDDTemp(hddTempHeader, hddTemp, monitored.hddTemps);
            //Log.v(TAG, "Got update from monitored server: " + monitored.nickName + " - " + monitored.now.toString());
            lastUpdateTime = System.currentTimeMillis();
            monitored.setUpdateWaiting(false); // we processed this update already, so set false and wait for next update
        }
        handleErrors();
    }

    Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            update();
            updateHandler.postDelayed(updateTimer, updateInterval);
        }
    };

    public void startUpdates() {
        Log.d(TAG, "Started update timer");
        startUpdatesToast.show();
        updateHandler.removeCallbacks(updateTimer);
        updateTimer.run();
    }

    public void stopUpdates() {
        Log.d(TAG, "Stopped update timer");
        updateHandler.removeCallbacks(updateTimer);
        stopUpdatesToast.show();
    }

    public void shutdown() {
        stopUpdates();
    }

    public void deleteAllServers() {
        doNotMonitor();
        allGlances.clear();
    }

    public void handleErrors() {
        UPDATE_ERROR err = monitored.getErrorCode();
        if (err != null) {
            String errMsg = null;
            if (err == UPDATE_ERROR.AUTH_FAILED) {
                errMsg = "Server does not require password";
                //Toast.makeText(getActivity(), errMsg, Toast.LENGTH_LONG).show();
            } else if (err == UPDATE_ERROR.CONN_REFUSED) {
                errMsg = "Connection refused";
            } else if (err == UPDATE_ERROR.SAX_PARSER_ANDROID_2_X) {
                errMsg = "Android version too low, cannot parse xml";
            } else if (err == UPDATE_ERROR.AUTH_CHALLENGE_NULL) {
                errMsg = "Wrong password";
            } else if (err == UPDATE_ERROR.BAD_HOSTNAME) {
                errMsg = "Unable to resolve host - check server url";
            } else {
                errMsg = "Undefined error";
            }
            updateTimeText.setText(errMsg);
            monitored.setErrorCode(null);
        }
    }

    public GlancesInstance getMonitoredServer() {
        return monitored;
    }
}
