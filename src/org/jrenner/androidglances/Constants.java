package org.jrenner.androidglances;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static enum UPDATE_ERROR {
        CONN_REFUSED,
        AUTH_FAILED,
        BAD_HOSTNAME,
		INVALID_PORT,
        UNDEFINED
    }

	public static Map<UPDATE_ERROR, String> errorTexts = new HashMap<>();
}