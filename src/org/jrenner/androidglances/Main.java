package org.jrenner.androidglances;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import org.jrenner.glances.*;

import java.net.MalformedURLException;

import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class Main extends Activity {
    private static final String TAG = "Glances-Main";
    private TextView mainText;
    private GlancesInstance server;

    private int interval = 3000; // 3 seconds by default, can be changed later
    private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mainText = (TextView) findViewById(R.id.mainText);
        handler = new Handler();
        URL url = null;
        try {
            url = new URL("http://home.jrenner.org:7113");
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage());
        }
        server = new GlancesInstance(url, "Raspberry Pi");
        startUpdates();
    }

    Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            update();
            handler.postDelayed(updateTimer, interval);
        }
    };

    void startUpdates() {
        Log.d(TAG, "Started update timer");
        updateTimer.run();
    }

    void stopUpdates() {
        Log.d(TAG, "Stopped update timer");
        handler.removeCallbacks(updateTimer);
    }

    private void update() {
        Boolean updateOK = server.update();
        if (!updateOK) {
            Log.e(TAG, "ERROR: Couldn't update Glances instance");
        } else {
            String out = String.format("%s: %s", server.name, server.url.toString());
            if (server.now != null)
                out += "\n" + server.now.toString();
            if (server.systemInfo != null)
                out += "\n" + server.systemInfo.toString();
            if (server.cpu != null)
                out += "\n" + server.cpu.toString();
            if (server.memory != null)
                out += "\n" + server.memory.toString();
            /*for (NetworkInterface net : server.netInterfaces) {
                out += "\n" + net.toString();
            }*/
            for (FileSystem fs : server.fileSystems) {
                if (fs != null) {
                    out += "\n" + fs.toString();
                }
            }
            mainText.setText(out);
        }
    }

}
