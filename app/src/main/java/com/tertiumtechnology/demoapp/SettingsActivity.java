package com.tertiumtechnology.demoapp;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
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
    private EditText externalServerPort;

    private HashMap<EditText, Boolean> autoIncrements;
    private HashMap<EditText, Boolean> autoDecrements;

    private Handler incrDecrHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.device_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_activity_settings);

        autoIncrements = new HashMap<>();
        autoDecrements = new HashMap<>();

        incrDecrHandler = new Handler();

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
                externalServerPort.setEnabled(isChecked);
            }
        });

        externalServerPort = (EditText) findViewById(R.id.external_server_wifi_port);
        externalServerPort.setText(String.valueOf(Preferences.getExternalServerPort(this)));
        externalServerPort.setEnabled(externalServerEnabled.isChecked());
    }

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

        int port = TextUtils.isEmpty(externalServerPort.getText()) ? Preferences.getExternalServerPort(this) :
                Integer.parseInt(externalServerPort.getText().toString());
        Preferences.saveExternalServerPort(this, port);

        super.onBackPressed();
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

    private void incrementTimeout(EditText timeoutEditText) {
        if (timeoutEditText != null) {
            timeoutEditText.setText(String.valueOf(Long.parseLong(timeoutEditText.getText().toString()) + 1));
        }
    }

    private void decrementTimeout(EditText timeoutEditText) {
        if (timeoutEditText != null) {
            long timeoutValue = Long.parseLong(timeoutEditText.getText().toString());
            if (timeoutValue > 0) {
                timeoutEditText.setText(String.valueOf(timeoutValue - 1));
            }
        }
    }
}