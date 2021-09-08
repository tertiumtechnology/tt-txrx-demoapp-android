package com.tertiumtechnology.demoapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;

import com.tertiumtechnology.demoapp.ExternalServerThread;
import com.tertiumtechnology.demoapp.R;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class Preferences {

    private static final String PREF_EXTERNAL_SERVER_ENABLED = "com.tertiumtechnology.demoapp" +
            ".PREF_EXTERNAL_SERVER_ENABLED";
    private static final String PREF_EXTERNAL_READ_NOTIFY_SERVER_PORT = "com.tertiumtechnology.demoapp" +
            ".PREF_EXTERNAL_READ_NOTIFY_SERVER_PORT";
    private static final String PREF_EXTERNAL_EVENT_SERVER_PORT = "com.tertiumtechnology.demoapp" +
            ".PREF_EXTERNAL_EVENT_SERVER_PORT";

    public static int getExternalEventServerPort(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), 0);
        int port = sharedPreferences.getInt(Preferences.PREF_EXTERNAL_EVENT_SERVER_PORT, ExternalServerThread
                .PORT_DEFAULT_VALUE + 1);

        return port;
    }

    public static int getExternalReadNotifyServerPort(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), 0);
        int port = sharedPreferences.getInt(Preferences.PREF_EXTERNAL_READ_NOTIFY_SERVER_PORT, ExternalServerThread
                .PORT_DEFAULT_VALUE);

        return port;
    }

    public static boolean getExternalServerEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), 0);
        boolean isEnabled = sharedPreferences.getBoolean(Preferences.PREF_EXTERNAL_SERVER_ENABLED, false);

        return isEnabled;
    }

    public static void saveExternalEventServerPort(Context context, int port) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Preferences.PREF_EXTERNAL_EVENT_SERVER_PORT, port);
        editor.apply();
    }

    public static void saveExternalReadNotifyServerPort(Context context, int port) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Preferences.PREF_EXTERNAL_READ_NOTIFY_SERVER_PORT, port);
        editor.apply();
    }

    public static void saveExternalServerEnabled(Context context, boolean isEnabled) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Preferences.PREF_EXTERNAL_SERVER_ENABLED, isEnabled);
        editor.apply();
    }

    public static InetAddress wifiIpAddress(Context context) {

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        InetAddress ipInetAddress;
        try {
            ipInetAddress = InetAddress.getByAddress(ipByteArray);
        } catch (UnknownHostException ex) {
            ipInetAddress = null;
        }

        return ipInetAddress;
    }
}