package org.jrenner.androidglances;

import android.os.AsyncTask;
import android.util.Log;
import org.apache.xmlrpc.XmlRpcException;
import org.jrenner.glances.*;
import org.jrenner.glances.Process;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static org.jrenner.androidglances.Constants.*;

public class GlancesInstance {
    private static final String TAG = "Glances-Instance";
    private static final long updateInterval = 3000; // milliseconds
    public URL url;
    public String nickName;
    protected String password;
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
    Integer cores;
    public boolean updateWaiting; // if true, there is new updated data for the app to process
    public boolean updateExecuting; // used to run only one update task at a time
    long monitorStartTime;
    InstanceUpdater instanceUpdater;
    private UPDATE_ERROR errorCode;

    public GlancesInstance(URL url, String nickName, String password) throws MalformedURLException {
        setNewServer(url, nickName, password);
    }

    private void setNewServer(URL url, String nickName, String password) throws MalformedURLException {
        this.url = url;
        this.nickName = nickName;
        this.password = password;
        this.glances = new Glances(url.toString(), password);
        monitorStartTime = System.currentTimeMillis();
        Log.i(TAG, "Starting monitoring new server: " + nickName + " - " + url.toString());
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

    public UPDATE_ERROR getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(UPDATE_ERROR code) {
        errorCode = code;
    }

    private class InstanceUpdater extends AsyncTask<Glances, Void, Void> {
        @Override
        protected Void doInBackground(Glances... glancesList) {
            int length = glancesList.length;
            for (int i = 0; i < length; i++) {
                Glances current = glancesList[i];
                long updateStartTime = System.currentTimeMillis();
                Object[] allFields = new Object[]{now, cpu, memory, memorySwap, systemInfo, netInterfaces, fileSystems,
                                     load, diskIO, limits, hddTemps, processes, procCount, sensors, cores};
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
                } catch (ParseException e) {
                    Log.w(TAG, "GetNow() - " + e.toString());
                } catch (XmlRpcException e) {
                    String error = e.toString();
                    Log.e(TAG, error);
                    if (error.contains("Authentication failed")) {
                        setErrorCode(UPDATE_ERROR.AUTH_FAILED);
                    } else if (error.contains("Connection refused")) {
                        setErrorCode(UPDATE_ERROR.CONN_REFUSED);
                    }
                }
                float timeTaken = (float) (System.currentTimeMillis() - updateStartTime) / 1000;
                int fieldsRetrieved = 0;
                for (Object field : allFields) {
                    if (field != null) {
                        fieldsRetrieved++;
                    }
                }
                if (now != null) {
                    Log.v(TAG, String.format("Fetched update for %s - took %.1fs", nickName, timeTaken));
                    setUpdateWaiting(true);
                } else {
                    String msg = String.format("Only retrieved %d / %d data fields from server after %.1fs",
                            fieldsRetrieved, allFields.length, timeTaken);
                    Log.e(TAG, msg);
                }
            }
            try {
                Thread.sleep(updateInterval);
            } catch (InterruptedException e) {
                Log.e(TAG, "updateInterval sleep interrupted: " + e.toString());
            }
            return null;  // there has to be a better way to do this...
        }

        protected void onPostExecute(Boolean updateOK) {
        }
    }

    @Override
    public String toString() {
        return this.nickName;
    }
}
