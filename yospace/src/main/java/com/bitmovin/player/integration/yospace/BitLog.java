package com.bitmovin.player.integration.yospace;

import android.util.Log;

public class BitLog {

    private static final String TAG = BitLog.class.getSimpleName();
    private static boolean isLogEnabled;

    public static void enableLogging() {
        isLogEnabled = true;
    }

    public static void disableLogging() {
        isLogEnabled = false;
    }

    public static void d(String message) {
        if (isLogEnabled) {
            Log.d(TAG, "[DEBUG]" + getStackInfo() + " -> " + message);
        }
    }

    public static void e(String message) {
        if (isLogEnabled) {
            Log.e(TAG, "[ERROR]" + getStackInfo() + " -> " + message);
        }
    }

    public static void i(String message) {
        if (isLogEnabled) {
            Log.i(TAG, "[INFO]" + getStackInfo() + " -> " + message);
        }
    }

    public static void wtf(String message) {
        if (isLogEnabled) {
            Log.wtf(TAG, "[WTF]" + getStackInfo() + " -> " + message);
        }
    }

    public static void v(String message) {
        if (isLogEnabled) {
            Log.v(TAG, "[VERBOSE]" + getStackInfo() + " -> " + message);
        }
    }

    private static String getStackInfo() {
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
