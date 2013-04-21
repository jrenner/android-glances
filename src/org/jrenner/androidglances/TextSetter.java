package org.jrenner.androidglances;

import android.app.Activity;
import android.widget.TextView;
import org.jrenner.glances.*;
import org.jrenner.glances.Process;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


/**
 * activity must be set manually, probably in main activity's onCreate(), in order to get string resources
 * TODO: This shouldn't be necessary
 */
public class TextSetter {
    private static Activity activity;  // this must be set manually by outside Activity

    public static void setActivity(Activity act) {
        activity = act;
    }

    private static void handleNull(TextView tv) {
        // The data field will display "No data" because the data structure passed was null
        tv.setText(activity.getString(R.string.no_data));
    }

    public static boolean setNow(TextView tv, Date now) {
        if (now == null) {
            handleNull(tv);
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("HH:mm:ss z");
        String formattedNow = sdf.format(now);
        tv.setText(formattedNow);
        return true;
    }

    public static boolean setSystemInfo(TextView tv, SystemInfo sysInfo) {
        if (sysInfo == null) {
            handleNull(tv);
            return false;
        }
        tv.setText(sysInfo.toString());
        return true;
    }

    public static boolean setCPUHeader(TextView tv, Integer cores) {
        if (cores == null) {
            tv.setText(activity.getString(R.string.cpu));
            return false;
        }
        tv.setText(activity.getString(R.string.cpu_cores, cores));
        return true;
    }

    public static boolean setCPUUsage(TextView tv, Cpu cpu) {
        if (cpu == null) {
            handleNull(tv);
            return false;
        }
        tv.setText(String.format("Usage: %4.1f%%, User: %4.1f%%, System: %4.1f%%", 100 - cpu.getIdle(),
                cpu.getUser(), cpu.getSystem()));
        return true;
    }

    public static boolean setCPULoad(TextView tv, Load load) {
        if (load == null) {
            handleNull(tv);
            return false;
        }
        tv.setText(String.format("Load (1/5/15min): %3.2f, %3.2f, %3.2f",
                load.getMin1(), load.getMin5(), load.getMin15()));
        return true;
    }

    public static boolean setMemory(TextView header, TextView tv, Memory mem) {
        header.setText(activity.getString(R.string.memory));
        if (mem == null) {
            handleNull(tv);
            return false;
        }
        String memTotal = Glances.autoUnit(mem.getTotal());
        String memUsed = Glances.autoUnit(mem.getUsed());
        tv.setText(activity.getString(R.string.used, memUsed, memTotal));
        return true;
    }

    public static boolean setSwap(TextView header, TextView tv, MemorySwap swap) {
        header.setText(activity.getString(R.string.swap));
        if (swap == null) {
            handleNull(tv);
            return false;
        }
        String swapTotal = Glances.autoUnit(swap.getTotal());
        String swapUsed = Glances.autoUnit(swap.getUsed());
        tv.setText(activity.getString(R.string.used, swapUsed, swapTotal));
        return true;
    }

    public static boolean setNetworks(TextView header, TextView tv, List<NetworkInterface> nets) {
        header.setText(activity.getString(R.string.network_interfaces));
        if (nets == null) {
            handleNull(tv);
            return false;
        }
        String netData = "";
        String netRecv, netSend, netTotalRecv, netTotalSend;
        for (NetworkInterface net : nets) {
            if (!"".equals(netData)) {
                netData += "\n";
            }
            netRecv = Glances.autoUnit(net.getRxPerSecond());
            netSend = Glances.autoUnit(net.getTxPerSecond());
            netTotalRecv = Glances.autoUnit(net.getCumulativeRx());
            netTotalSend = Glances.autoUnit(net.getCumulativeTx());
            netData += String.format("%s - Rx: %s/s (%s), Tx: %s/s (%s)", net.getInterfaceName(), netRecv,
                    netTotalRecv, netSend, netTotalSend);
        }
        tv.setText(netData);
        return true;
    }

    public static boolean setFileSystems(TextView header, TextView tv, List<FileSystem> fileSystems) {
        header.setText(activity.getString(R.string.file_systems));
        if (fileSystems == null) {
            handleNull(tv);
            return false;
        }
        String fsData = "";
        for (FileSystem fs : fileSystems) {
            if (!"".equals(fsData)) {
                fsData += "\n";
            }
            fsData += activity.getString(R.string.available, fs.getDeviceName(), Glances.autoUnit(fs.getAvailable()),
                    Glances.autoUnit(fs.getSize()));
        }
        tv.setText(fsData);
        return true;
    }

    public static boolean setDiskIO(TextView header, TextView tv, List<DiskIO> disks) {
        header.setText(activity.getString(R.string.disk_io));
        if (disks == null) {
            handleNull(tv);
            return false;
        }
        String diskData = "";
        for (DiskIO disk : disks) {
            if (!"".equals(diskData)) {
                diskData += "\n";
            }
            diskData += activity.getString(R.string.read_write, disk.getDiskName(),
                    Glances.autoUnit(disk.getBytesReadPerSec()), Glances.autoUnit(disk.getBytesWrittenPerSec()));
        }
        tv.setText(diskData);
        return true;
    }

    static Comparator<Process> processCPUPlusMemoryComparator = new Comparator<Process>() {
        public int compare(Process p1, Process p2) {
            double p1score = p1.getCpuPercent() + p1.getMemoryPercent();
            double p2score = p2.getCpuPercent() + p2.getMemoryPercent();
            if (p1score == p2score)
                return 0;
            return p1score > p2score ? -1 : 1;
        }
    };

    public static boolean setProcesses(TextView header, TextView tv, List<Process> processes) {
        if (processes == null) {
            handleNull(tv);
            return false;
        }
        header.setText(activity.getString(R.string.textsetter_top_processes));
        if (processes.size() == 0) {
            tv.setText(activity.getString(R.string.textsetter_processes_disabled));
            return true;
        }
        String procData = "";
        Collections.sort(processes, processCPUPlusMemoryComparator);
        int count = 0;
        int numToAdd = 10;
        for (Process proc : processes) {
            if (count >= numToAdd) {
                break;
            }
            if (!"".equals(procData)) {
                procData += "\n";
            }
            String thisProc = String.format("%s CPU: %.0f%% - Mem: %.0f%% (%s)", proc.getName(), proc.getCpuPercent(),
                    proc.getMemoryPercent(), proc.getUserName());
            procData += thisProc;
            count++;
        }
        tv.setText(procData);
        return true;
    }

    public static boolean setSensors(TextView header, TextView tv, List<Sensor> sensors) {
        header.setText(activity.getString(R.string.sensors));
        if (sensors == null) {
            handleNull(tv);
            return false;
        }
        String sensorData = "";
        for (Sensor sensor : sensors) {
            if (!"".equals(sensorData)) {
                sensorData += "\n";
            }
            sensorData += sensor.toString();
        }
        if ("".equals(sensorData)) {
            sensorData = activity.getString(R.string.no_data);
        }
        tv.setText(sensorData);
        return true;
    }

    public static boolean setHDDTemp(TextView header, TextView tv, List<HardDriveTemp> hddtemps) {
        header.setText(activity.getString(R.string.hard_drive_temperature));
        if (hddtemps == null) {
            handleNull(tv);
            return false;
        }
        String hddtempData = "";
        for (HardDriveTemp temp : hddtemps) {
            if (!"".equals(hddtempData)) {
                hddtempData += "\n";
            }
            hddtempData += temp.toString();
        }
        if ("".equals(hddtempData)) {
            hddtempData = activity.getString(R.string.no_data);
        }
        tv.setText(hddtempData);
        return true;
    }
}
