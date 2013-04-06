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
    private static final String TAG = "Glances-MonitorFrag";
    private GlancesInstance server;
    private Handler updateHandler;
    private int updateInterval = 3000;

    private TextView nameText;
    private TextView systemText;
    private TextView cpuIdle;
    private TextView totalMem;

    @Override
    public void onCreate(Bundle onSavedInstance) {
        super.onCreate(onSavedInstance);
        updateHandler = new Handler();
        Log.d(TAG, "monitor fragment created");
        initializeServer();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.monitor, container, false);

        // assign views
        nameText = (TextView) view.findViewById(R.id.nameText);
        systemText = (TextView) view.findViewById(R.id.systemText);
        cpuIdle = (TextView) view.findViewById(R.id.cpuIdle);
        totalMem = (TextView) view.findViewById(R.id.totalMem);
        return view;
    }

    public void initializeServer() {
        URL url = null;
        try {
            url = new URL("http://home.jrenner.org:7113");
        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString());
        }
        server = new GlancesInstance(url, "Raspberry Pi");
    }

    private void update() {
        Boolean updateOK = server.update();
        if (!updateOK) {
            Log.e(TAG, "ERROR: Couldn't update Glances instance");
            Toast.makeText(getActivity().getApplicationContext(), "Problem getting update", Toast.LENGTH_SHORT).show();
            stopUpdates();
        } else {
            try {
                nameText.setText(String.format("%s", server.name, server.url.toString()));
                systemText.setText(server.systemInfo.toString());
                cpuIdle.setText(String.format("USAGE: %3.0f%%\nLOAD (1/5/15): %3.0f%%, %3.0f%%, %3.0f%%",
                        100 - server.cpu.getIdle(),
                        server.load.getMin1() * 100, server.load.getMin5() * 100, server.load.getMin15() * 100));
                totalMem.setText("Total: " + Glances.autoUnit(server.memory.getTotal()));
            } catch (NullPointerException e) {
                Log.e(TAG, "null pointer when getting glances data: " + e.toString());
            }
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
        updateTimer.run();
    }

    public void stopUpdates() {
        Log.d(TAG, "Stopped update timer");
        updateHandler.removeCallbacks(updateTimer);
    }
}
