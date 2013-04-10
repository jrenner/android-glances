package org.jrenner.androidglances;

import android.widget.TextView;
import org.jrenner.glances.*;
import org.jrenner.glances.Process;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class TextSetter {
    private static final String NO_DATA = "No data";

    private static void handleNull(TextView tv) {
        tv.setText(NO_DATA);
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
            handleNull(tv);
            return false;
        }
        tv.setText(String.format("CPU (%d cores)", cores));
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
        header.setText("Memory");
        if (mem == null) {
            handleNull(tv);
            return false;
        }
        tv.setText(mem.toString());
        return true;
    }

    public static boolean setNetworks(TextView header, TextView tv, List<NetworkInterface> nets) {
        header.setText("Network Interfaces");
        if (nets == null) {
            handleNull(tv);
            return false;
        }
        String netData = "";
        for (NetworkInterface net : nets) {
            if (!"".equals(netData)) {
                netData += "\n";
            }
            netData += net.toString();
        }
        tv.setText(netData);
        return true;
    }

    public static boolean setFileSystems(TextView header, TextView tv, List<FileSystem> fileSystems) {
        header.setText("File Systems");
        if (fileSystems == null) {
            handleNull(tv);
            return false;
        }
        String fsData = "";
        for (FileSystem fs : fileSystems) {
            if (!"".equals(fsData)) {
                fsData += "\n";
            }
            fsData += String.format("%s - (%s of %s available)", fs.getMountPoint(), Glances.autoUnit(fs.getAvailable()),
                    Glances.autoUnit(fs.getSize()));
        }
        tv.setText(fsData);
        return true;
    }

    static Comparator<Process> processCPUComparator = new Comparator<Process>() {
        public int compare(Process p1, Process p2) {
            if (p1.getCpuPercent() == p2.getCpuPercent())
                return 0;
            return p1.getCpuPercent() > p2.getCpuPercent() ? -1 : 1;
        }
    };

    public static boolean setProcesses(TextView header, TextView tv, List<Process> processes) {
        if (processes == null) {
            handleNull(tv);
            return false;
        }
        header.setText("Top 5 Processes by CPU");
        if (processes.size() == 0) {
            tv.setText("Process data disabled by server");
            return true;
        }
        String procData = "";
        Collections.sort(processes, processCPUComparator);
        int count = 0;
        int numToAdd = 5;
        for (Process proc : processes) {
            if (count >= numToAdd) {
                break;
            }
            if (!"".equals(procData)) {
                procData += "\n";
            }
            String thisProc = proc.getName() + " (" + proc.getCpuPercent() + "%) User: " + proc.getUserName();
            procData += thisProc;
            count++;
        }
        tv.setText(procData);
        return true;
    }

    public static boolean setSensors(TextView header, TextView tv, List<Sensor> sensors) {
        header.setText("Sensors");
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
        tv.setText(sensorData);
        return true;
    }

    public static boolean setHDDTemp(TextView header, TextView tv, List<HardDriveTemp> hddtemps) {
        header.setText("Hard Drive Temperatures");
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
        tv.setText(hddtempData);
        return true;
    }
}
