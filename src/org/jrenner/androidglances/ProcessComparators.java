package org.jrenner.androidglances;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.jrenner.glances.Process;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ProcessComparators {
	private static Comparator<Process> activeComp;

	public static Comparator<Process> getActiveComparator() {
		if (activeComp == null) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Main.getContext());
			UserSettings.setProcessComparatorFromSettings(prefs);
		}
		return activeComp;
	}

	public static void setActiveComparator(Comparator<Process> newComp) {
		activeComp = newComp;
	}

	public static Comparator<Process> processCPUPlusMemoryComparator = new Comparator<Process>() {;
		public int compare(Process p1, Process p2) {
			double p1score = p1.getCpuPercent() + p1.getMemoryPercent();
			double p2score = p2.getCpuPercent() + p2.getMemoryPercent();
			if (p1score == p2score)
				return 0;
			return p1score > p2score ? -1 : 1;
		}
	};

	public static Comparator<Process> processCPUComparator = new Comparator<Process>() {
		public int compare(Process p1, Process p2) {
			double p1score = p1.getCpuPercent();
			double p2score = p2.getCpuPercent();
			if (p1score == p2score)
				return 0;
			return p1score > p2score ? -1 : 1;
		}
	};

	public static Comparator<Process> processMemoryComparator = new Comparator<Process>() {
		public int compare(Process p1, Process p2) {
			double p1score = p1.getMemoryPercent();
			double p2score = p2.getMemoryPercent();
			if (p1score == p2score)
				return 0;
			return p1score > p2score ? -1 : 1;
		}
	};

/*	public static Comparator<Process> processNameComparator = new Comparator<Process>() {
		public int compare(Process p1, Process p2) {
			return p1.getName().compareTo(p2.getName());
		}
	};*/

	public static Comparator<Process> processIOComparator = new Comparator<Process>() {
		public int compare(Process p1, Process p2) {
			double p1score = p1.getBytesReadSinceUpdate() + p1.getBytesWrittenSinceUpdate();
			double p2score = p2.getBytesReadSinceUpdate() + p2.getBytesWrittenSinceUpdate();
			if (p1score == p2score)
				return 0;
			return p1score > p2score ? -1 : 1;
		}
	};

	public static Map<Comparator, String> compNames;
	public static void initializeCompNames() {
		compNames = new HashMap<>();
		compNames.put(processCPUPlusMemoryComparator, Main.getContext().getString(R.string.proc_sort_cpu_and_mem));
		compNames.put(processCPUComparator, Main.getContext().getString(R.string.proc_sort_cpu));
		compNames.put(processMemoryComparator, Main.getContext().getString(R.string.proc_sort_mem));
		//compNames.put(processNameComparator, Main.getContext().getString(R.string.proc_sort_name));
		compNames.put(processIOComparator, Main.getContext().getString(R.string.proc_sort_io));
	}
}
