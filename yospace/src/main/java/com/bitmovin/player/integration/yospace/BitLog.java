package com.bitmovin.player.integration.yospace;

import android.util.Log;

public class BitLog {

    private static final String TAG = BitLog.class.getSimpleName();
    private static boolean isEnabled;

    public static void enable() {
        isEnabled = true;
    }

    public static void disable() {
        isEnabled = false;
    }

    public static void d(String message) {
        if (isEnabled) {
            Log.d(TAG, getStackTraceInfo() + " -> " + message);
        }
    }

    public static void e(String message) {
        if (isEnabled) {
            Log.e(TAG, getStackTraceInfo() + " -> " + message);
        }
    }

    public static void i(String message) {
        if (isEnabled) {
            Log.i(TAG, getStackTraceInfo() + " -> " + message);
        }
    }

    public static void wtf(String message) {
        if (isEnabled) {
            Log.wtf(TAG, getStackTraceInfo() + " -> " + message);
        }
    }

    public static void v(String message) {
        if (isEnabled) {
            Log.v(TAG, getStackTraceInfo() + " -> " + message);
        }
    }

    private static String getStackTraceInfo() {
        StackTraceElement[] trace = new Exception().getStackTrace();
        String info = "";
        // Trace index is 2 levels up
        int traceIndex = 2;
        if (trace.length > traceIndex) {
            String classPath = trace[traceIndex].getClassName();
            // Get class name from full path
            int startIndex = classPath.lastIndexOf(".");
            String className = classPath.substring(startIndex + 1).trim();
            // Remove appended anonymous class name (e.g. $2) if present
            if (className.contains("$")) {
                className = className.substring(0, className.indexOf("$"));
            }
            info = "[" + className + ":" + trace[traceIndex].getMethodName() + ":" + trace[traceIndex].getLineNumber() + "]";
        }
        return info;
    }
}
