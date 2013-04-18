package org.jrenner.androidglances;

/**
 * Created with IntelliJ IDEA.
 * User: jrenner
 * Date: 4/18/13
 * Time: 1:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserSettings {
    private static long serverUpdateInterval;

    public static void setServerUpdateInterval(long interval) {
        serverUpdateInterval = interval;
    }

    public static long getServerUpdateInterval() {
        return serverUpdateInterval;
    }
}
