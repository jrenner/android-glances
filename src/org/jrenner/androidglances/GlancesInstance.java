package org.jrenner.androidglances;

import android.os.AsyncTask;
import android.util.Log;
import org.jrenner.glances.*;
import org.jrenner.glances.Process;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
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
    InstanceUpdater instanceUpdater;



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
        if (instanceUpdater != null && instanceUpdater.getStatus() != AsyncTask.Status.FINISHED) {
            return;
        }
        instanceUpdater = new InstanceUpdater();
        instanceUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, glances);
    }

    public boolean isUpdateWaiting() {
        return updateWaiting;
    }

    public boolean isUpdateExecuting() {
        return updateExecuting;
    }

    public void setUpdateWaiting(boolean status) {
        updateWaiting = status;
        //Log.v(TAG, "updateWaiting set to: " + status);
    }

    private class InstanceUpdater extends AsyncTask<Glances, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Glances... glancesList) {
            Boolean updateOK = true;
            int length = glancesList.length;
            for (int i = 0; i < length; i++) {
                Glances current = glancesList[i];
                long updateStartTime = System.currentTimeMillis();
                try {
                    now = current.getNow();
                } catch (ParseException e) {
                    Log.w(TAG, "GetNow() - " + e.toString());
                }
                HANDLE ALL EXCEPTIONS NICELY PLEASE
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
                    float timeTaken = (float) (System.currentTimeMillis() - updateStartTime) / 1000;
                    Log.v(TAG, String.format("Fetched update for %s - took %.1fs", nickName, timeTaken));
            }
            if (updateOK) {
                setUpdateWaiting(true);
                //Log.v(TAG, "Update OK for: " + nickName + " - " + url.toString());
            } else {
                Log.w(TAG, "update failed for " + url.toString());
            }


            try {
                Thread.sleep(updateInterval);
            } catch (InterruptedException e) {
                Log.e(TAG, "updateInterval sleep interrupted: " + e.toString());
            }
            return updateOK;
        }

        protected void onPostExecute(Boolean updateOK) {
        }
    }
}
