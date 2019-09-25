package com.bitmovin.player.integration.yospace;

import android.util.Log;

public class BitmovinLogger {

    private static boolean isDebugEnabled;

    public static void enableLogging() {
        isDebugEnabled = true;
    }

    public static void disableLogging() {
        isDebugEnabled = false;
    }

    public static void d(String tag, String message) {
        if (isDebugEnabled) {
            Log.d(tag, message);
        }
    }

    public static void e(String tag, String message) {
        if (isDebugEnabled) {
            Log.e(tag, message);
        }
    }

    public static void i(String tag, String message) {
        if (isDebugEnabled) {
            Log.i(tag, message);
        }
    }

    public static void wtf(String tag, String message) {
        if (isDebugEnabled) {
            Log.wtf(tag, message);
        }
    }

    public static void v(String tag, String message) {
        if (isDebugEnabled) {
            Log.v(tag, message);
        }
    }
}
