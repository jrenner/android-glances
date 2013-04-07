package org.jrenner.androidglances;

import android.os.AsyncTask;
import android.util.Log;
import org.jrenner.glances.*;
import org.jrenner.glances.Process;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GlancesInstance {
    private static final String TAG = "Glances-Instance";
    private static final long updateInterval = 5000; // milliseconds
    public URL url;
    public String nickName;
    private Glances glances;
    Date now;
    Cpu cpu;
    Memory memory;
    SystemInfo systemInfo;
    List<NetworkInterface> netInterfaces;
    List<FileSystem> fileSystems;
    Load load;
    List<DiskIO> diskIO;
    Limits limits;
    MemorySwap memorySwap;
    List<HardDriveTemp> hddTemps;
    List<Process> processes;
    ProcessCount procCount;
    List<Sensor> sensors;
    int cores;
    public boolean updateWaiting; // if true, there is new updated data for the app to process
    public boolean updateExecuting; // used to run only one update task at a time
    long monitorStartTime;



    public GlancesInstance(URL url, String nickName) {
        setNewServer(url, nickName);
    }

    public void setNewServer(URL url, String nickName) {
        Log.i(TAG, "Starting monitoring new server: " + nickName + " - " + url.toString());
        this.url = url;
        this.nickName = nickName;
        try {
            this.glances = new Glances(url);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString());
        }
        monitorStartTime = System.currentTimeMillis();
    }

    public void update() {
        InstanceUpdater instanceUpdater = new InstanceUpdater();
        instanceUpdater.execute(glances);
    }

    public boolean isUpdateWaiting() {
        return updateWaiting;
    }

    public boolean isUpdateExecuting() {
        return updateExecuting;
    }

    public void setUpdateWaiting(boolean status) {
        updateWaiting = status;
        Log.v(TAG, "updateWaiting set to: " + status);
    }

    private class InstanceUpdater extends AsyncTask<Glances, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Glances... glancesList) {
            try {
                Thread.sleep(updateInterval);
            } catch (InterruptedException e) {
                Log.e(TAG, "updateInterval sleep interrupted: " + e.toString());
            }
            Boolean updateOK = true;
            int length = glancesList.length;
            for (int i = 0; i < length; i++) {
                Glances current = glancesList[i];
                try {
                    now = current.getNow();
                    cpu = current.getCpu();
                    memory = current.getMem();
                    memorySwap = current.getMemSwap();
                    systemInfo = current.getSystem();
                    netInterfaces = current.getNetwork();
                    fileSystems = current.getFs();
                    load = current.getLoad();
                    diskIO = current.getDiskIO();
                    limits = current.getAllLimits();
                    hddTemps = current.getHardDriveTemps();
                    processes = current.getProcessList();
                    procCount = current.getProcessCount();
                    sensors = current.getSensors();
                    cores = current.getCore();
                } catch (Exception e) {
                    Log.e(TAG, "GlancesInstance update error: " + e.toString());
                    updateOK = false;
                }
            }
            return updateOK;
        }

        protected void onPostExecute(Boolean updateOK) {
            if (updateOK) {
                setUpdateWaiting(true);
            } else {
                Log.w(TAG, "update failed for " + url.toString());
            }
        }
    }
}
