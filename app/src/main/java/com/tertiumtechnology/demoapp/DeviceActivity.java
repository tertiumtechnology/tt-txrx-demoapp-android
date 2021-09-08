package com.tertiumtechnology.demoapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.tertiumtechnology.demoapp.ExternalServerThread.DeviceActivityCommand;
import com.tertiumtechnology.demoapp.ExternalServerThread.NetworkBleReceiver;
import com.tertiumtechnology.demoapp.util.Preferences;
import com.tertiumtechnology.txrxlib.util.BleChecker;

import java.net.InetAddress;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class DeviceActivity extends AppCompatActivity {

    public class BleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i(TAG, intent.getAction());

            if (BleService.DEVICE_CONNECTED.equals(intent.getAction())) {
                connectionState = ConnectionState.DISCOVERING;

                supportInvalidateOptionsMenu();
            }
            else if (BleService.DEVICE_DISCONNECTED.equals(intent.getAction())) {
                disableWriteButton();

                connectionState = ConnectionState.DISCONNECTED;
                supportInvalidateOptionsMenu();
            }
            else if (BleService.DEVICE_TX_RX_SERVICE_FOUND.equals(intent.getAction())) {
                currentMode = STREAM_MODE;

                if (bleService.isTxRxAckme()) {
                    deviceAllowSetMode = true;
                }

                connectionState = ConnectionState.CONNECTED;
                supportInvalidateOptionsMenu();

                writeButton.setEnabled(true);
            }
            else if (BleService.DEVICE_TX_RX_SERVICE_NOT_FOUND.equals(intent.getAction())) {
                disableWriteButton();
                deviceAllowSetMode = false;

                connectionState = ConnectionState.CONNECTED;
                supportInvalidateOptionsMenu();

                composeAndAppendMsg(getString(R.string.error_no_available_services), getMsgColor(R.color
                        .colorErrorText), true);
            }
            else if (BleService.DEVICE_WRITE_DATA.equals(intent.getAction())) {
                allowWriteRequests();
            }
            else if (BleService.DEVICE_READ_DATA.equals(intent.getAction())) {
                String dataRead = intent.getStringExtra(BleService.INTENT_EXTRA_DATA_VALUE);

                if (!dataRead.equals("")) {
                    composeAndAppendMsg(dataRead, getMsgColor(R.color.colorReadText), false);
                }
            }
            else if (BleService.DEVICE_EVENT_DATA.equals(intent.getAction())) {
                String eventData = intent.getStringExtra(BleService.INTENT_EXTRA_DATA_VALUE);

                if (!eventData.equals("")) {
                    composeAndAppendMsg(eventData, getMsgColor(R.color.colorEventText), false);
                }
            }
            else if (BleService.DEVICE_SETMODE.equals(intent.getAction())) {
                int setModeValue = intent.getIntExtra(BleService.INTENT_EXTRA_DATA_VALUE, -1);

                currentMode = setModeValue;
                supportInvalidateOptionsMenu();

//                composeAndAppendMsg(getString(R.string.set_mode_response, getModeString(setModeValue)),
//                        getMsgColor(R.color.colorReadText), true);
            }
            else if (BleService.DEVICE_CONNECTION_OPERATION_FAILED.equals(intent.getAction())) {
                disableWriteButton();

                connectionState = ConnectionState.DISCONNECTED;
                supportInvalidateOptionsMenu();

                composeAndAppendMsg(intent.getStringExtra(BleService.INTENT_EXTRA_DATA_VALUE), getMsgColor(R.color
                        .colorErrorText), true);
            }
            else if (BleService.DEVICE_WRITE_OPERATION_FAILED.equals(intent.getAction())) {
                composeAndAppendMsg(intent.getStringExtra(BleService.INTENT_EXTRA_DATA_VALUE), getMsgColor(R.color
                        .colorErrorText), true);

                allowWriteRequests();

                hideSoftKeyboard();
            }
            else if (BleService.DEVICE_READ_OPERATION_FAILED.equals(intent.getAction())) {
                composeAndAppendMsg(intent.getStringExtra(BleService.INTENT_EXTRA_DATA_VALUE), getMsgColor(R.color
                        .colorErrorText), true);

                hideSoftKeyboard();
            }
            else if (BleService.DEVICE_SETMODE_OPERATION_FAILED.equals(intent.getAction())) {
                composeAndAppendMsg(intent.getStringExtra(BleService.INTENT_EXTRA_DATA_VALUE), getMsgColor(R.color
                        .colorErrorText), true);

                hideSoftKeyboard();
            }
            else {
                throw new UnsupportedOperationException(getString(R.string.error_invalid_action));
            }
        }
    }

    private enum ConnectionState {
        DISCONNECTED, CONNECTING, DISCOVERING, CONNECTED
    }

    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    private static final int REQUEST_ENABLE_BT = 1;

    private static final int STREAM_MODE = 1;
    private static final int CMD_MODE = 3;

    private static final String TAG = DeviceActivity.class.getSimpleName();

    private static IntentFilter getBleIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.DEVICE_CONNECTED);
        intentFilter.addAction(BleService.DEVICE_DISCONNECTED);
        intentFilter.addAction(BleService.DEVICE_TX_RX_SERVICE_FOUND);
        intentFilter.addAction(BleService.DEVICE_TX_RX_SERVICE_NOT_FOUND);
        intentFilter.addAction(BleService.DEVICE_WRITE_DATA);
        intentFilter.addAction(BleService.DEVICE_READ_DATA);
        intentFilter.addAction(BleService.DEVICE_CONNECTION_OPERATION_FAILED);
        intentFilter.addAction(BleService.DEVICE_WRITE_OPERATION_FAILED);
        intentFilter.addAction(BleService.DEVICE_READ_OPERATION_FAILED);
        intentFilter.addAction(BleService.DEVICE_SETMODE);
        intentFilter.addAction(BleService.DEVICE_SETMODE_OPERATION_FAILED);
        intentFilter.addAction(BleService.DEVICE_EVENT_DATA);
        intentFilter.addAction(BleService.INTENT_EXTRA_DATA_VALUE);

        return intentFilter;
    }

    private BleReceiver bleReceiver;
    private BleService bleService;
    private ServiceConnection bleServiceConnection;
    private ConnectionState connectionState;
    private String deviceAddress;
    private boolean enableDisconnect;
    private ScrollView readScrollView;
    private AppCompatTextView readTextView;
    private AppCompatButton writeButton;
    private AppCompatEditText writeEditText;
    private ProgressBar writeProgressBar;

    private int currentMode;
    private boolean deviceAllowSetMode;

    private ExternalServerThread readNotifySocketThread;
    private NetworkBleReceiver readNotifyNetworkBleReceiver;

    private ExternalServerThread eventSocketThread;
    private NetworkBleReceiver eventNetworkBleReceiver;

    private AppCompatButton repeatWriteButton;
    private String lastCommand;

    private ActivityResultLauncher<Intent> activityResultLauncher;

    public void doSetModeRequest() {
        if (bleService != null) {
            int mode = currentMode == STREAM_MODE ? CMD_MODE : STREAM_MODE;

//            composeAndAppendMsg(getString(R.string.set_mode_request, getModeString(mode)),
//                    getMsgColor(R.color.colorWriteText), true);

            if (!bleService.setMode(mode)) {
                composeAndAppendMsg(getString(R.string.error_unable_to_setmode), getMsgColor(R.color.colorErrorText),
                        true);
            }
        }
    }

    public void doWriteRequest(String data) {
        if (bleService != null && data != null) {
            composeAndAppendMsg(data, getMsgColor(R.color.colorWriteText), true);

            disallowWriteRequests();

            if (!bleService.writeData(data)) {
                allowWriteRequests();

                composeAndAppendMsg(getString(R.string.error_unable_to_write), getMsgColor(R.color.colorErrorText),
                        true);
            }

            lastCommand = data;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device, menu);
        menu.findItem(R.id.menu_connect).setVisible(true);
        menu.findItem(R.id.menu_connecting).setVisible(true);
        menu.findItem(R.id.menu_discovering).setVisible(false);
        menu.findItem(R.id.menu_disconnect).setVisible(false);
        menu.findItem(R.id.menu_refresh).setVisible(false);

        menu.findItem(R.id.menu_mode_stream).setVisible(false);
        menu.findItem(R.id.menu_mode_cmd).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                if (bleService.connect(deviceAddress)) {
                    connectionState = ConnectionState.CONNECTING;
                    supportInvalidateOptionsMenu();
                }
                return true;
            case R.id.menu_disconnect:
                bleService.disconnect();
                return true;
            case R.id.menu_mode_stream:
            case R.id.menu_mode_cmd:
                doSetModeRequest();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (connectionState == ConnectionState.DISCONNECTED) {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_connecting).setVisible(false);
            menu.findItem(R.id.menu_discovering).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
            menu.findItem(R.id.menu_refresh).setVisible(false);

            menu.findItem(R.id.menu_mode_stream).setVisible(false);
            menu.findItem(R.id.menu_mode_cmd).setVisible(false);
        }
        else if (connectionState == ConnectionState.CONNECTING) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setVisible(true);
            menu.findItem(R.id.menu_discovering).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);

            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
            menu.findItem(R.id.menu_refresh).setVisible(true);

            menu.findItem(R.id.menu_mode_stream).setVisible(false);
            menu.findItem(R.id.menu_mode_cmd).setVisible(false);
        }
        else if (connectionState == ConnectionState.DISCOVERING) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setVisible(false);
            menu.findItem(R.id.menu_discovering).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);

            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
            menu.findItem(R.id.menu_refresh).setVisible(true);

            menu.findItem(R.id.menu_mode_stream).setVisible(false);
            menu.findItem(R.id.menu_mode_cmd).setVisible(false);
        }
        else if (connectionState == ConnectionState.CONNECTED) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setVisible(false);
            menu.findItem(R.id.menu_discovering).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);

            menu.findItem(R.id.menu_refresh).setActionView(null);
            menu.findItem(R.id.menu_refresh).setVisible(false);

            if (deviceAllowSetMode) {
                if (currentMode == STREAM_MODE) {
                    menu.findItem(R.id.menu_mode_stream).setVisible(true);
                    menu.findItem(R.id.menu_mode_cmd).setVisible(false);
                }
                else {
                    menu.findItem(R.id.menu_mode_stream).setVisible(false);
                    menu.findItem(R.id.menu_mode_cmd).setVisible(true);
                }
            }
            else {
                menu.findItem(R.id.menu_mode_stream).setVisible(false);
                menu.findItem(R.id.menu_mode_cmd).setVisible(false);
            }
        }

        if (menu.findItem(R.id.menu_disconnect).isVisible()) {
            if (enableDisconnect) {
                menu.findItem(R.id.menu_disconnect).setEnabled(true);
            }
            else {
                menu.findItem(R.id.menu_disconnect).setEnabled(false);
            }
        }

        return true;
    }

    private void allowDisconnect() {
        if (!enableDisconnect) {
            enableDisconnect = true;
            supportInvalidateOptionsMenu();
        }
    }

    private void allowWriteRequests() {
        writeProgressBar.setVisibility(View.INVISIBLE);
        writeButton.setEnabled(true);
        allowDisconnect();
    }

    private void composeAndAppendMsg(String textMsg, int writeColor, boolean ln) {
        textMsg += ln ? "\n" : "";
        Spannable msg = new SpannableString(textMsg);
        msg.setSpan(new ForegroundColorSpan(writeColor), 0, msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        readTextView.append(msg);
        scrollDownReadView();
    }

    private void disableWriteButton() {
        writeProgressBar.setVisibility(View.INVISIBLE);
        writeButton.setEnabled(false);
    }

    private void disallowDisconnect() {
        if (enableDisconnect) {
            enableDisconnect = false;
            supportInvalidateOptionsMenu();
        }
    }

    private void disallowWriteRequests() {
        disallowDisconnect();
        writeButton.setEnabled(false);
        writeProgressBar.setVisibility(View.VISIBLE);
    }

    private String getModeString(int mode) {
        return mode == STREAM_MODE ? getString(R.string.menu_stream_mode) :
                getString(R.string.menu_cmd_mode);
    }

    private int getMsgColor(int colorResourceId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(colorResourceId);
        }
        else {
            //noinspection deprecation
            return getResources().getColor(colorResourceId);
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Activity
                .INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
    }

    private void scrollDownReadView() {
        readScrollView.post(new Runnable() {
            @Override
            public void run() {
                readScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private ExternalServerThread startExternalServer(InetAddress address, int port,
                                                     ExternalServerThread socketThread,
                                                     String eventServerName, IntentFilter eventIntentFilter,
                                                     DeviceActivityCommand command) {

        if (socketThread != null && socketThread.getState() != Thread.State.TERMINATED) {
            try {
                socketThread.join();
            } catch (InterruptedException e) {

            }
        }

        socketThread = new ExternalServerThread(eventServerName, this, address, port, command);

        LocalBroadcastManager.getInstance(this).registerReceiver(socketThread.getNetworkBleReceiver(),
                eventIntentFilter);
        socketThread.start();

        return socketThread;
    }

    private void stopExternalServer(ExternalServerThread socketThread,
                                    NetworkBleReceiver networkBleReceiver) {
        if (socketThread != null) {
            socketThread.interrupt();
            LocalBroadcastManager.getInstance(this).unregisterReceiver(networkBleReceiver);
            networkBleReceiver = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // manage UI
        setContentView(R.layout.activity_device);
        Toolbar toolbar = (Toolbar) findViewById(R.id.device_toolbar);
        setSupportActionBar(toolbar);

        String deviceName = getIntent().getStringExtra(EXTRAS_DEVICE_NAME);
        deviceAddress = getIntent().getStringExtra(EXTRAS_DEVICE_ADDRESS);

        getSupportActionBar().setTitle(deviceName);
        getSupportActionBar().setSubtitle(deviceAddress);

        // UI write
        writeEditText = (AppCompatEditText) findViewById(R.id.write_edit_text);
        writeProgressBar = (ProgressBar) findViewById(R.id.write_progress_bar);
        writeButton = (AppCompatButton) findViewById(R.id.write_button);
        repeatWriteButton = (AppCompatButton) findViewById(R.id.repeat_write_button);

        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String editTextValue = writeEditText.getText().toString();

                writeEditText.getText().clear();
                doWriteRequest(editTextValue);
            }
        });

        repeatWriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastCommand != null) {
                    doWriteRequest(lastCommand);
                }
            }
        });

        // UI read
        readScrollView = (ScrollView) findViewById(R.id.read_scroll_view);
        readTextView = (AppCompatTextView) findViewById(R.id.read_text_view);

        disableWriteButton();
        allowDisconnect();

        currentMode = STREAM_MODE;
        deviceAllowSetMode = false;

        // init service
        final BluetoothAdapter bluetoothAdapter = BleChecker.getBtAdapter(getApplicationContext());
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        activityResultLauncher = registerForActivityResult(new StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_CANCELED) {
                            finish();
                        }
                    }
                });

        Intent bleServiceIntent = new Intent(this, BleService.class);

        bleServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                bleService = ((BleService.LocalBinder) service).getService();

                bleService.init(bluetoothAdapter);

                if (bleService.connect(deviceAddress)) {
                    connectionState = ConnectionState.CONNECTING;
                    supportInvalidateOptionsMenu();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                bleService = null;
            }
        };

        bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);

        // init broadcast receiver
        bleReceiver = new BleReceiver();

        connectionState = ConnectionState.DISCONNECTED;

        LocalBroadcastManager.getInstance(this).registerReceiver(bleReceiver, getBleIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bleReceiver);
        unbindService(bleServiceConnection);
        bleService = null;

        readNotifySocketThread = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!BleChecker.isBluetoothEnabled(getApplicationContext())) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            activityResultLauncher.launch(enableBtIntent);
        }

        if (bleService != null && connectionState == ConnectionState.CONNECTED) {
            if (!bleService.isDeviceConnected(deviceAddress)) {
                boolean result = bleService.connect(deviceAddress);

                if (!result) {
                    Log.w(TAG, getString(R.string.log_warning_unable_to_connect));
                    connectionState = ConnectionState.DISCONNECTED;
                    supportInvalidateOptionsMenu();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Preferences.getExternalServerEnabled(getApplicationContext())) {
            InetAddress address = Preferences.wifiIpAddress(getApplicationContext());

            // readNotify
            int readNotifyServerPort = Preferences.getExternalReadNotifyServerPort(getApplicationContext());
            IntentFilter readIntentFilter = new IntentFilter();
            readIntentFilter.addAction(BleService.DEVICE_READ_DATA);

            readNotifySocketThread = startExternalServer(address, readNotifyServerPort, readNotifySocketThread,
                    getString(R.string.external_server_type_read_notify),
                    readIntentFilter, new DeviceActivityCommand() {
                        @Override
                        public void excecute(DeviceActivity activity, String inputData) {
                            activity.doWriteRequest(inputData);
                        }
                    });

            readNotifyNetworkBleReceiver = readNotifySocketThread.getNetworkBleReceiver();

            // event
            int eventServerPort = Preferences.getExternalEventServerPort(getApplicationContext());
            IntentFilter eventIntentFilter = new IntentFilter();
            eventIntentFilter.addAction(BleService.DEVICE_EVENT_DATA);

            eventSocketThread = startExternalServer(address, eventServerPort, eventSocketThread,
                    getString(R.string.external_server_type_event),
                    eventIntentFilter, null);

            eventNetworkBleReceiver = eventSocketThread.getNetworkBleReceiver();
        }
    }

    @Override
    protected void onStop() {
        if (Preferences.getExternalServerEnabled(getApplicationContext())) {
            // readNotify
            stopExternalServer(readNotifySocketThread, readNotifyNetworkBleReceiver);

            // event
            stopExternalServer(eventSocketThread, eventNetworkBleReceiver);
        }

        super.onStop();
    }
}