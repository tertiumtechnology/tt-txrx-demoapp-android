package com.tertiumtechnology.demoapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.tertiumtechnology.demoapp.ExternalServerThread;
import com.tertiumtechnology.demoapp.R;

import java.net.InetAddress;

public class Preferences {

    private static final String PREF_EXTERNAL_SERVER_ENABLED = "com.tertiumtechnology.demoapp" +
            ".PREF_EXTERNAL_SERVER_ENABLED";
    private static final String PREF_EXTERNAL_READ_NOTIFY_SERVER_PORT = "com.tertiumtechnology.demoapp" +
            ".PREF_EXTERNAL_READ_NOTIFY_SERVER_PORT";
    private static final String PREF_EXTERNAL_EVENT_SERVER_PORT = "com.tertiumtechnology.demoapp" +
            ".PREF_EXTERNAL_EVENT_SERVER_PORT";

    public static int getExternalEventServerPort(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), 0);

        return sharedPreferences.getInt(Preferences.PREF_EXTERNAL_EVENT_SERVER_PORT, ExternalServerThread
                .PORT_DEFAULT_VALUE + 1);
    }

    public static int getExternalReadNotifyServerPort(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), 0);

        return sharedPreferences.getInt(Preferences.PREF_EXTERNAL_READ_NOTIFY_SERVER_PORT, ExternalServerThread
                .PORT_DEFAULT_VALUE);
    }

    public static boolean getExternalServerEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), 0);

        return sharedPreferences.getBoolean(Preferences.PREF_EXTERNAL_SERVER_ENABLED, false);
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

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        Network activeNetwork = connectivityManager.getActiveNetwork();

        if(activeNetwork == null){
            return null;
        }

        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);

        if(networkCapabilities == null || !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
            return null;
        }

        LinkProperties linkProperties = connectivityManager.getLinkProperties(activeNetwork);

        if(linkProperties == null){
            return null;
        }

        LinkAddress first = linkProperties.getLinkAddresses().stream()
                .filter(linkAddress -> linkAddress.getAddress().getAddress().length == 4)
                .findFirst().orElse(null);

        if(first == null)
            return null;

        return first.getAddress();
    }
}