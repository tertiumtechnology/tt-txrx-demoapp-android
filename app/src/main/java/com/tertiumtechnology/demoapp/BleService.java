package com.tertiumtechnology.demoapp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.tertiumtechnology.txrxlib.rw.TxRxDeviceCallback;
import com.tertiumtechnology.txrxlib.rw.TxRxDeviceManager;
import com.tertiumtechnology.txrxlib.rw.TxRxTimeouts;
import com.tertiumtechnology.txrxlib.rw.TxRxTimestamps;
import com.tertiumtechnology.txrxlib.util.TxRxPreferences;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BleService extends Service {
    class LocalBinder extends Binder {
        BleService getService() {
            return BleService.this;
        }
    }

    public static final String DEVICE_CONNECTED = "DEVICE_CONNECTED";
    public static final String DEVICE_CONNECTION_OPERATION_FAILED = "DEVICE_CONNECTION_OPERATION_FAILED";
    public static final String DEVICE_DISCONNECTED = "DEVICE_DISCONNECTED";
    public static final String DEVICE_READ_DATA = "DEVICE_READ_DATA";
    public static final String DEVICE_READ_OPERATION_FAILED = "DEVICE_READ_OPERATION_FAILED";
    public static final String DEVICE_TX_RX_SERVICE_FOUND = "DEVICE_TX_RX_SERVICE_FOUND";
    public static final String DEVICE_TX_RX_SERVICE_NOT_FOUND = "DEVICE_TX_RX_SERVICE_NOT_FOUND";
    public static final String DEVICE_WRITE_DATA = "DEVICE_WRITE_DATA";
    public static final String DEVICE_WRITE_OPERATION_FAILED = "DEVICE_WRITE_OPERATION_FAILED";
    public static final String DEVICE_SETMODE = "DEVICE_SETMODE";
    public static final String DEVICE_SETMODE_OPERATION_FAILED = "DEVICE_SETMODE_OPERATION_FAILED";
    public static final String DEVICE_EVENT_DATA = "DEVICE_EVENT_DATA";
    public static final String INTENT_EXTRA_DATA_VALUE = "INTENT_EXTRA_DATA_VALUE";

    private final IBinder localBinder = new LocalBinder();
    private final TxRxDeviceCallback deviceCallback = new TxRxDeviceCallback() {
        @Override
        public void onConnectionError(int errorCode) {
            Intent failedIntent = createIntentWithExtraValue(DEVICE_CONNECTION_OPERATION_FAILED, getString(R.string
                    .error_unable_to_connect));
            sendMsg(failedIntent);
        }

        @Override
        public void onConnectionTimeout() {
            Intent failedIntent = createIntentWithExtraValue(DEVICE_CONNECTION_OPERATION_FAILED, getString(R.string
                    .error_connection_timeout));
            sendMsg(failedIntent);
        }

        @Override
        public void onDeviceConnected() {
            sendMsg(new Intent(DEVICE_CONNECTED));
        }

        @Override
        public void onDeviceDisconnected() {
            sendMsg(new Intent(DEVICE_DISCONNECTED));
        }

        @Override
        public void onEventData(String data) {
            Intent eventIntent = createIntentWithExtraValue(DEVICE_EVENT_DATA, data);
            sendMsg(eventIntent);
        }

        @Override
        public void onNotifyData(String data) {
            Intent readIntent = createIntentWithExtraValue(DEVICE_READ_DATA, data);
            sendMsg(readIntent);
        }

        @Override
        public void onReadData(String data) {
            Intent readIntent = createIntentWithExtraValue(DEVICE_READ_DATA, data);
            sendMsg(readIntent);
        }

        @Override
        public void onReadError(int errorCode) {
            Intent failedIntent = createIntentWithExtraValue(DEVICE_READ_OPERATION_FAILED, getString(R.string
                    .error_unable_to_read));
            sendMsg(failedIntent);
        }

        @Override
        public void onReadNotifyTimeout() {
            Intent failedIntent = createIntentWithExtraValue(DEVICE_READ_OPERATION_FAILED, getString(R.string
                    .error_read_timeout));
            sendMsg(failedIntent);
        }

        @Override
        public void onReceiveTxRxTimestampsAfterNotifyData(TxRxTimestamps txRxTimestamps) {

        }

        @Override
        public void onSetMode(int mode) {
            Intent writeIntent = createIntentWithExtraValue(DEVICE_SETMODE, mode);
            sendMsg(writeIntent);
        }

        @Override
        public void onSetModeError(int errorCode) {
            Intent failedIntent = createIntentWithExtraValue(DEVICE_SETMODE_OPERATION_FAILED, getString(R.string
                    .error_unable_to_setmode));
            sendMsg(failedIntent);
        }

        @Override
        public void onSetModeTimeout() {
            Intent failedIntent = createIntentWithExtraValue(DEVICE_SETMODE_OPERATION_FAILED, getString(R.string
                    .error_setmode_timeout));
            sendMsg(failedIntent);
        }

        @Override
        public void onTxRxServiceDiscovered() {
            sendMsg(new Intent(DEVICE_TX_RX_SERVICE_FOUND));
        }

        @Override
        public void onTxRxServiceNotFound() {
            sendMsg(new Intent(DEVICE_TX_RX_SERVICE_NOT_FOUND));
        }

        @Override
        public void onWriteData(String data) {
            Intent writeIntent = createIntentWithExtraValue(DEVICE_WRITE_DATA, data);
            sendMsg(writeIntent);
        }

        @Override
        public void onWriteError(int errorCode) {
            Intent failedIntent = createIntentWithExtraValue(DEVICE_WRITE_OPERATION_FAILED, getString(R.string
                    .error_unable_to_write));
            sendMsg(failedIntent);
        }

        @Override
        public void onWriteTimeout() {
            Intent failedIntent = createIntentWithExtraValue(DEVICE_WRITE_OPERATION_FAILED, getString(R.string
                    .error_write_timeout));
            sendMsg(failedIntent);
        }

        private Intent createIntentWithExtraValue(String gattOperation, String value) {
            Intent intent = new Intent(gattOperation);
            intent.putExtra(INTENT_EXTRA_DATA_VALUE, value);
            return intent;
        }

        private Intent createIntentWithExtraValue(String gattOperation, int value) {
            Intent intent = new Intent(gattOperation);
            intent.putExtra(INTENT_EXTRA_DATA_VALUE, value);
            return intent;
        }

        private void sendMsg(Intent intent) {
            LocalBroadcastManager.getInstance(BleService.this).sendBroadcast(intent);
        }
    };
    private TxRxDeviceManager txRxDeviceManager;

    public void close() {
        txRxDeviceManager.disconnect();

        txRxDeviceManager.close();
    }

    public boolean connect(String address) {
        return txRxDeviceManager.connect(address, getApplicationContext());
    }

    public void disconnect() {
        txRxDeviceManager.disconnect();
    }

    public void init(BluetoothAdapter bluetoothAdapter) {
        TxRxTimeouts txRxTimeouts = TxRxPreferences.getTimeouts(this);
        txRxDeviceManager = new TxRxDeviceManager(bluetoothAdapter, deviceCallback, txRxTimeouts);
    }

    public boolean isDeviceConnected(String deviceAddress) {
        if (txRxDeviceManager == null) {
            return false;
        }

        return txRxDeviceManager.isConnected(deviceAddress, getApplicationContext());
    }

    public boolean isTxRxAckme() {
        return txRxDeviceManager.isTxRxAckme();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public boolean requestReadData() {
        return txRxDeviceManager.requestReadData();
    }

    public boolean setMode(int mode) {
        return txRxDeviceManager.requestSetMode(mode);
    }

    public boolean writeData(String data) {
        return txRxDeviceManager.requestWriteData(data);
    }
}