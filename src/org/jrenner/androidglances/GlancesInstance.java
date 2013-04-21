package org.jrenner.androidglances;

import android.os.AsyncTask;
import android.util.Log;
import de.timroes.axmlrpc.XMLRPCException;
import org.jrenner.glances.*;
import org.jrenner.glances.Process;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static org.jrenner.androidglances.Constants.UPDATE_ERROR;

public class GlancesInstance {
    private static final String TAG = "Glances-Instance";
    private static int updateInterval = 3000; // milliseconds
    private long lastUpdateTime;
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
        lastUpdateTime = 0;
        Log.i(TAG, "Starting monitoring new server: " + nickName + " - " + url.toString());
    }

    public void setTimeout(int seconds) {
        glances.setTimeout(seconds);
    }

    public void update() {
        long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdateTime;
        if (timeSinceLastUpdate < updateInterval) {
            return;
        }
        if (instanceUpdater != null && instanceUpdater.getStatus() != AsyncTask.Status.FINISHED) {
            return;
        }
        instanceUpdater = new InstanceUpdater();
        // executes async tasks in serial
        // in the future, if we want to update concurrently, we need to use the commented out version
        // Concurrency is disabled because of many issues: battery use, unreliable updating due to network clogging, etc.
        // A round robin approach is probably best anyways
        // instanceUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, glances);
        instanceUpdater.execute(glances);
    }

    public boolean isUpdateWaiting() {
        return updateWaiting;
    }

    public boolean isUpdateExecuting() {
        return updateExecuting;
    }

    public void setUpdateWaiting(boolean status) {
        // update has been fetched and is ready to be retrieved
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
        Thread updateThread;

        @Override
        protected Void doInBackground(Glances... glancesList) {
            updateThread = Thread.currentThread();
            int length = glancesList.length;
            for (int i = 0; i < length; i++) {
                Glances current = glancesList[i];
                long updateStartTime = System.currentTimeMillis();
                /*Object[] allFields = new Object[]{now, cpu, memory, memorySwap, systemInfo, netInterfaces, fileSystems,
                                     load, diskIO, limits, hddTemps, processes, procCount, sensors, cores};*/
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
                } catch (XMLRPCException e) {
                    String error = e.toString();
                    Log.e(TAG, error);
                    if (error.contains("Authentication failed") || error.contains("challenge is null")) {
                        setErrorCode(UPDATE_ERROR.AUTH_FAILED);
                    } else if (error.contains("Connection refused")) {
                        setErrorCode(UPDATE_ERROR.CONN_REFUSED);
                    } else if (error.contains("Unable to resolve host")) {
                        setErrorCode(UPDATE_ERROR.BAD_HOSTNAME);
                    } else if (error.contains("java.io.EOFException")){
                        // do nothing - don't know why it happens, but it doesn't seem harmful!
                    } else {
                        setErrorCode(UPDATE_ERROR.UNDEFINED);
                    }
                }
                long currentTime = System.currentTimeMillis();
                float timeTaken = (float) (currentTime - updateStartTime) / 1000;
                // We use now as a very simple test to see if the update was successful
                // This test is far from perfect. i.e. now could fail and all others could succeed
                // However in practice it almost always works well
                if (now != null) {
                    Log.v(TAG, String.format("Fetched update for %s - took %.1fs", nickName, timeTaken));
                    setUpdateWaiting(true);
                }
                lastUpdateTime = currentTime;
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

    public void setUpdateInterval(int interval) {
        updateInterval = interval;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    /**
     * Stop waiting and just update ASAP
     */
    public void resetUpdateTimer() {
        lastUpdateTime = 0;
    }
}
