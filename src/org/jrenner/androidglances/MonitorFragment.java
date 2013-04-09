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
    private TextView swap;
    private TextView netHeader;
    private TextView nets;
    private TextView diskIO;
    private TextView fsHeader;
    private TextView fileSystems;
    private TextView hddTemp;
    private TextView sensors;
    private TextView procHeader;
    private TextView processes;
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
        addServerToList("http://home.jrenner.org:7113", "Raspberry Pi");
        addServerToList("http://192.168.173.103:61209", "Ubuntu PC");

/*        addServerToList("http://192.168.173.103:28100", "Test 0");
        addServerToList("http://192.168.173.103:28101", "Test 1");
        addServerToList("http://192.168.173.103:28102", "Test 2");
        addServerToList("http://192.168.173.103:28103", "Test 3");
        addServerToList("http://192.168.173.103:28104", "Test 4");*/

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
        netHeader = (TextView) view.findViewById(R.id.netHeader);
        nets = (TextView) view.findViewById(R.id.nets);
        fsHeader = (TextView) view.findViewById(R.id.fsHeader);
        fileSystems = (TextView) view.findViewById(R.id.fileSystems);
        procHeader = (TextView) view.findViewById(R.id.procHeader);
        processes = (TextView) view.findViewById(R.id.processes);
        return view;
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

    public void setServer(String urltext, String serverNickName) {
        GlancesInstance selection = null;
        for (GlancesInstance server : allGlances) {
            if (server.nickName.equals(serverNickName)) {
                selection = server;
            }
        }
        if (selection == null) {
            Log.e(TAG, "Couldn't find server with name - " + serverNickName);
            return;
        }
        monitored = selection;
        updateAgeText.setText("Waiting for update from server");
        nameText.setText("...");
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
            serverAddress.setText(monitored.url.getHost() + " : " + monitored.url.getPort());
            setNow(updateTimeText, monitored.now);
            setSystemInfo(systemText, monitored.systemInfo);
            setCPUHeader(cpuHeader, monitored.cores);
            setCPUUsage(cpuUsage, monitored.cpu);
            setCPULoad(cpuLoad, monitored.load);
            setMemory(memoryHeader, memory, monitored.memory);
            setNetworks(netHeader, nets, monitored.netInterfaces);
            setFileSystems(fsHeader, fileSystems, monitored.fileSystems);
            setProcesses(procHeader, processes, monitored.processes);
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

/*    private boolean isNetSignificant(NetworkInterface net) {
        long CUMUL_THRESHOLD = 1024 * 1000 * 10; // about 10 megabytes
        long cumulativeTotal = net.getCumulativeRx() + net.getCumulativeTx();
        if (cumulativeTotal > CUMUL_THRESHOLD) {
            return true;
        }
        long SPEED_THRESHOLD = 1024 * 10; // about 5 kB/s
        long currentTotal = net.getRxPerSecond() + net.getTxPerSecond();
        if (currentTotal > SPEED_THRESHOLD) {
            return true;
        }
        return false;
    }*/
}
