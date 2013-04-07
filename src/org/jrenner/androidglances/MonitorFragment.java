package org.jrenner.androidglances;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.jrenner.glances.FileSystem;
import org.jrenner.glances.Glances;
import org.jrenner.glances.NetworkInterface;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MonitorFragment extends Fragment {
    private static MonitorFragment instance;
    private static final String TAG = "Glances-MonitorFrag";
    private GlancesInstance server;
    private Handler updateHandler;
    private int updateInterval = 1000;

    private TextView nameText;
    private TextView serverAddress;
    private TextView updateTimeText;
    private TextView updateAgeText;
    private TextView systemText;
    private TextView cpuHeader;
    private TextView cpuCores;
    private TextView cpuUsage;
    private TextView cpuLoad;
    private TextView totalMem;
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
        totalMem = (TextView) view.findViewById(R.id.totalMem);
        return view;
    }

    public void setServer(String urltext, String serverNickName) {
        URL url = null;
        try {
            url = new URL(urltext);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString());
        }
        if (server == null) {
            server = new GlancesInstance(url, serverNickName);
        } else {
            server.setNewServer(url, serverNickName);
        }
        startUpdates();
        serverAddress.setText("Waiting for update from server");
        nameText.setText("...");
    }

    private void update() {
        if (server == null) {
            Log.d(TAG, "No update, server was null");
            return;
        }
        if (!server.isUpdateExecuting()) {
            server.update();
        }
        if (!server.isUpdateWaiting()) {
            Log.v(TAG, "No update waiting for server: " + server.nickName);
            if (lastUpdateTime != 0) {
                String age;
                long monitorTime = System.currentTimeMillis() - server.monitorStartTime;
                long updateAge = (System.currentTimeMillis() - lastUpdateTime) / 1000;
                if (updateAge >= 3600) {
                    age = updateAge / 3600 + "h old";
                } else if (updateAge >= 60) {
                    age = updateAge / 60 + "m old";
                } else {
                    age = updateAge + "s old";
                }
                updateAgeText.setText(age + " monitored for " + Tools.convertToHumanTime(monitorTime));
            }
        } else {
            try {
                nameText.setText(server.nickName);
                serverAddress.setText(server.url.toString());
                updateTimeText.setText(server.now.toString());
                systemText.setText(server.systemInfo.toString());
                cpuHeader.setText(String.format("CPU (%d cores)", server.cores));
                Log.i("TAG", "I see " + server.cores + " cores on " + server.nickName);
                cpuUsage.setText(String.format("Usage: %4.1f%%, User: %4.1f%%, System: %4.1f%%", 100 - server.cpu.getIdle(),
                        server.cpu.getUser(), server.cpu.getSystem()));
                cpuLoad.setText(String.format("Load (1/5/15min): %3.2f, %3.2f, %3.2f",
                        server.load.getMin1(), server.load.getMin5(), server.load.getMin15()));
                totalMem.setText("Total: " + Glances.autoUnit(server.memory.getTotal()));
            } catch (NullPointerException e) {
                Log.e(TAG, "null pointer when getting glances data: " + e.toString());
            }
            Log.v(TAG, "Got update from " + server.nickName + " at " + server.now.toString());
            lastUpdateTime = System.currentTimeMillis();
            server.setUpdateWaiting(false); // we processed this update already, so set false and wait for next update
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
}
