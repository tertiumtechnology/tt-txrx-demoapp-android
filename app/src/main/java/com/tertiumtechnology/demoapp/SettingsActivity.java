package com.tertiumtechnology.demoapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.tertiumtechnology.demoapp.util.Preferences;
import com.tertiumtechnology.txrxlib.rw.TxRxTimeouts;
import com.tertiumtechnology.txrxlib.util.TxRxPreferences;

import java.net.InetAddress;
import java.util.HashMap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private class IncrDecrUpdater implements Runnable {

        private EditText timeout;

        IncrDecrUpdater(EditText timeout) {
            this.timeout = timeout;
        }

        @Override
        public void run() {
            if (autoIncrements.get(timeout)) {
                incrementTimeout(timeout);
                incrDecrHandler.postDelayed(new IncrDecrUpdater(timeout), 50);
            }
            else if (autoDecrements.get(timeout)) {
                decrementTimeout(timeout);
                incrDecrHandler.postDelayed(new IncrDecrUpdater(timeout), 50);
            }
        }
    }

    private EditText connectTimeout;
    private EditText writeTimeout;
    private EditText firstReadTimeout;
    private EditText laterReadTimeout;

    private SwitchCompat externalServerEnabled;
    private TextView externalServerWifiAddress;
    private EditText externalReadNotifyServerPort;
    private EditText externalEventServerPort;

    private HashMap<EditText, Boolean> autoIncrements;
    private HashMap<EditText, Boolean> autoDecrements;

    private Handler incrDecrHandler;

    @Override
    public void onBackPressed() {
        incrDecrHandler.removeCallbacksAndMessages(null);

        TxRxTimeouts newTimeouts = new TxRxTimeouts(
                TextUtils.isEmpty(connectTimeout.getText()) ? 0 : Long.parseLong(connectTimeout.getText().toString()),
                TextUtils.isEmpty(writeTimeout.getText()) ? 0 : Long.parseLong(writeTimeout.getText().toString()),
                TextUtils.isEmpty(firstReadTimeout.getText()) ? 0 : Long.parseLong(firstReadTimeout.getText()
                        .toString()),
                TextUtils.isEmpty(laterReadTimeout.getText()) ? 0 : Long.parseLong(laterReadTimeout.getText()
                        .toString())
        );

        TxRxPreferences.saveTimeouts(this, newTimeouts);

        Preferences.saveExternalServerEnabled(this, externalServerEnabled.isChecked());

        int port = TextUtils.isEmpty(externalReadNotifyServerPort.getText()) ?
                Preferences.getExternalReadNotifyServerPort(this) :
                Integer.parseInt(externalReadNotifyServerPort.getText().toString());
        Preferences.saveExternalReadNotifyServerPort(this, port);

        int eventPort = TextUtils.isEmpty(externalEventServerPort.getText()) ?
                Preferences.getExternalEventServerPort(this) :
                Integer.parseInt(externalEventServerPort.getText().toString());
        Preferences.saveExternalEventServerPort(this, eventPort);

        super.onBackPressed();
    }

    private void decrementTimeout(EditText timeoutEditText) {
        if (timeoutEditText != null) {
            long timeoutValue = Long.parseLong(timeoutEditText.getText().toString());
            if (timeoutValue > 0) {
                timeoutEditText.setText(String.valueOf(timeoutValue - 1));
            }
        }
    }

    private void incrementTimeout(EditText timeoutEditText) {
        if (timeoutEditText != null) {
            timeoutEditText.setText(String.valueOf(Long.parseLong(timeoutEditText.getText().toString()) + 1));
        }
    }

    private void initTimeoutView(final EditText timeoutEditText, int plusButtonResId, int minusButtonResId, long
            timeoutValue) {
        timeoutEditText.setText(String.valueOf(timeoutValue));

        Button timeoutButtonPlus = (Button) findViewById(plusButtonResId);
        timeoutButtonPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incrementTimeout(timeoutEditText);
            }
        });

        autoIncrements.put(timeoutEditText, false);

        timeoutButtonPlus.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View arg0) {
                        autoIncrements.put(timeoutEditText, true);
                        incrDecrHandler.post(new IncrDecrUpdater(timeoutEditText));
                        return false;
                    }
                }
        );

        timeoutButtonPlus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                        && autoIncrements.get(timeoutEditText)) {
                    autoIncrements.put(timeoutEditText, false);
                }
                return false;
            }
        });

        Button timeoutButtonMinus = (Button) findViewById(minusButtonResId);
        timeoutButtonMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decrementTimeout(timeoutEditText);
            }
        });

        autoDecrements.put(timeoutEditText, false);

        timeoutButtonMinus.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View arg0) {
                        autoDecrements.put(timeoutEditText, true);
                        incrDecrHandler.post(new IncrDecrUpdater(timeoutEditText));
                        return false;
                    }
                }
        );

        timeoutButtonMinus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                        && autoDecrements.get(timeoutEditText)) {
                    autoDecrements.put(timeoutEditText, false);
                }
                return false;
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.device_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_activity_settings);

        autoIncrements = new HashMap<>();
        autoDecrements = new HashMap<>();

        incrDecrHandler = new Handler(Looper.getMainLooper());

        // manage timeouts
        TxRxTimeouts currentTimeouts = TxRxPreferences.getTimeouts(this);

        connectTimeout = (EditText) findViewById(R.id.connect_timeout);
        initTimeoutView(connectTimeout, R.id.connect_timeout_button_plus, R.id.connect_timeout_button_minus,
                currentTimeouts.getConnectTimeout());

        writeTimeout = (EditText) findViewById(R.id.write_timeout);
        initTimeoutView(writeTimeout, R.id.write_timeout_button_plus, R.id.write_timeout_button_minus,
                currentTimeouts.getWriteTimeout());

        firstReadTimeout = (EditText) findViewById(R.id.first_read_timeout);
        initTimeoutView(firstReadTimeout, R.id.first_read_timeout_button_plus, R.id.first_read_timeout_button_minus,
                currentTimeouts.getFirstReadTimeout());

        laterReadTimeout = (EditText) findViewById(R.id.later_read_timeout);
        initTimeoutView(laterReadTimeout, R.id.later_read_timeout_button_plus, R.id.later_read_timeout_button_minus,
                currentTimeouts.getLaterReadTimeout());

        // manage external server
        externalServerWifiAddress = (TextView) findViewById(R.id.external_server_wifi_address);

        InetAddress inetAddress = Preferences.wifiIpAddress(getApplicationContext());
        if (inetAddress != null) {
            externalServerWifiAddress.setText(inetAddress.getHostAddress());
        }

        externalServerEnabled = (SwitchCompat) findViewById(R.id.external_server_enabled);
        externalServerEnabled.setChecked(Preferences.getExternalServerEnabled(this));
        externalServerEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                externalReadNotifyServerPort.setEnabled(isChecked);
                externalEventServerPort.setEnabled(isChecked);
            }
        });

        externalReadNotifyServerPort = (EditText) findViewById(R.id.external_server_wifi_read_notify_port);
        externalReadNotifyServerPort.setText(String.valueOf(Preferences.getExternalReadNotifyServerPort(this)));
        externalReadNotifyServerPort.setEnabled(externalServerEnabled.isChecked());

        externalEventServerPort = (EditText) findViewById(R.id.external_server_wifi_event_port);
        externalEventServerPort.setText(String.valueOf(Preferences.getExternalEventServerPort(this)));
        externalEventServerPort.setEnabled(externalServerEnabled.isChecked());
    }
}