package org.jrenner.androidglances;

import android.os.AsyncTask;
import android.util.Log;
import org.jrenner.glances.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GlancesInstance {
    private static final String TAG = "Glances-Instance";
    private static final int TIMEOUT = 10000; // milliseconds
    public URL url;
    public String name;
    private Glances glances;
    Date now;
    Cpu cpu;
    Memory memory;
    SystemInfo systemInfo;
    List<NetworkInterface> netInterfaces;
    List<FileSystem> fileSystems;
    Load load;


    public GlancesInstance(URL url, String name) {
        this.url = url;
        this.name = name;
        try {
            this.glances = new Glances(url);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString());
        }
    }

    public boolean update() {
        instanceUpdater updater = new instanceUpdater();
        updater.execute(glances);
        Boolean updateOK = false;
        try {
            updateOK = updater.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupt) {
            Log.e(TAG, interrupt.toString());
        } catch (TimeoutException timeout) {
            Log.e(TAG, timeout.toString());
        } catch (ExecutionException execution) {
            Log.e(TAG, execution.toString());
        }
        return updateOK;
    }

    private class instanceUpdater extends AsyncTask<Glances, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Glances... glancesList) {
            Boolean updateOK = true;
            int length = glancesList.length;
            for (int i = 0; i < length; i++) {
                Glances current = glancesList[i];
                try {
                    now = current.getNow();
                    cpu = current.getCpu();
                    memory = current.getMem();
                    systemInfo = current.getSystem();
                    netInterfaces = current.getNetwork();
                    fileSystems = current.getFs();
                    load = current.getLoad();
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                    updateOK = false;
                }
            }
            return updateOK;
        }
    }
}
