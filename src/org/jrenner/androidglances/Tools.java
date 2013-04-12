package org.jrenner.androidglances;

public class Tools {
    private static final long SECOND = 1000;
    private static final long MINUTE = SECOND * 60;
    private static final long HOUR = MINUTE * 60;
    private static final long DAY = HOUR * 24;
    private static final long WEEK = DAY * 7;
    /** turn milliseconds into HMSDW format */
    public static String convertToHumanTime(long ms) {
        long s = 0;
        long m = 0;
        long h = 0;
        long d = 0;
        long w = 0;
        if (ms >= WEEK) {
            w = ms / WEEK;
            return w + "w";
        }
        if (ms >= DAY) {
            d = ms / DAY;
            return d + "d";
        }
        if (ms >= HOUR) {
            h = ms / HOUR;
            m = (ms % HOUR) / MINUTE;
            return h + "h" + m + "m";
        }
        if (ms >= MINUTE) {
            m = ms / MINUTE;
            return m + "m";
        }
        if (ms >= SECOND) {
            s = ms / SECOND;
            return s + "s";
        }
        return ms + "ms";
    }
}
