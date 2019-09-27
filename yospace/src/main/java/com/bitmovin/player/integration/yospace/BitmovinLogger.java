package com.bitmovin.player.integration.yospace;

import android.util.Log;

public class BitmovinLogger {

    private static final String TAG = BitmovinLogger.class.getSimpleName();
    private static boolean isDebugEnabled;

    public static void enableLogging() {
        isDebugEnabled = true;
    }

    public static void disableLogging() {
        isDebugEnabled = false;
    }

    public static void d(String message) {
        if (isDebugEnabled) {
            Log.d(TAG, message);
        }
    }

    public static void e(String message) {
        if (isDebugEnabled) {
            Log.e(TAG, message);
        }
    }

    public static void i(String message) {
        if (isDebugEnabled) {
            Log.i(TAG, message);
        }
    }

    public static void wtf(String message) {
        if (isDebugEnabled) {
            Log.wtf(TAG, message);
        }
    }

    public static void v(String message) {
        if (isDebugEnabled) {
            Log.v(TAG, message);
        }
    }
}
