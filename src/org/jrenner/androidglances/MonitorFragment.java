package org.jrenner.androidglances;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import org.jrenner.glances.FileSystem;
import org.jrenner.glances.Glances;
import org.jrenner.glances.NetworkInterface;
import org.jrenner.glances.Process;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.jrenner.androidglances.TextSetter.*;

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

    public void addServerToList(String urltext, String nickName) {
        URL url = null;
        try {
            url = new URL(urltext);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString());
        }
        Log.v(TAG, "Adding Glances server to list: " + urltext + nickName);
        allGlances.add(new GlancesInstance(url, nickName));
    }

    /**
     * return true if removed
     */
    public boolean removeServerFromList(String urltext, String  nickName) {
        for (GlancesInstance server : allGlances) {
            if (server.nickName.equals(nickName) && server.url.toString().equals(urltext)) {
                allGlances.remove(server);
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
        updateAgeText.setText("Waiting for update from server");
        nameText.setText("");
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
        for (GlancesInstance server : allGlances) {
            if (!server.isUpdateExecuting()) {
                server.update();
            }
        }
        if (monitored == null) {
            //Log.v(TAG, "No server being monitored, nothing to do.");
            return;
        }
        if (!monitored.isUpdateWaiting()) {
            //Log.v(TAG, "No update waiting for monitored server: " + monitored.nickName);
            if (lastUpdateTime != 0) {
                long monitorTime = System.currentTimeMillis() - monitored.monitorStartTime;
                long updateAge = System.currentTimeMillis() - lastUpdateTime;
                updateAgeText.setText(Tools.convertToHumanTime(updateAge) + " old, monitored for " + Tools.convertToHumanTime(monitorTime));
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
            Log.v(TAG, "Got update from monitored server: " + monitored.nickName + " - " + monitored.now.toString());
            lastUpdateTime = System.currentTimeMillis();
            monitored.setUpdateWaiting(false); // we processed this update already, so set false and wait for next update
        }
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
}
