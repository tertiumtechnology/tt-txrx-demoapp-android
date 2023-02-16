package com.tertiumtechnology.demoapp;

import android.Manifest.permission;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.tertiumtechnology.demoapp.util.BleDeviceListAdapter;
import com.tertiumtechnology.txrxlib.scan.TxRxScanCallback;
import com.tertiumtechnology.txrxlib.scan.TxRxScanResult;
import com.tertiumtechnology.txrxlib.scan.TxRxScanner;
import com.tertiumtechnology.txrxlib.util.BleChecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ScanActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION = 2;

    private static final String[] filteredServiceUuids = new String[]{
            // Tertium sensor
            "f3770001-1164-49bc-8f22-0ac34292c217",
            // Zentri Ackme
            "175f8f23-a570-49bd-9627-815a6a27de2a",
            // Zhaga
            "3cc30001-cb91-4947-bd12-80d2f0535a30",
            // Tertium TxRx
            "d7080001-052c-46c4-9978-c0977bebf328",
            // Tertium Zebra
            "c1ff0001-c47e-424d-9495-fb504404b8f5"
    };

    private BleDeviceListAdapter bleDeviceListAdapter;
    private TxRxScanner txRxScanner;

    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (!txRxScanner.isScanning()) {
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
            menu.findItem(R.id.menu_refresh).setVisible(false);
        } else {
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                bleDeviceListAdapter.clear();

                checkForBluetoothPermissionAndEnabled();

                boolean permissionGranted = true;

                List<String> permissions = new ArrayList<>(Arrays.asList(permission.ACCESS_COARSE_LOCATION,
                        permission.ACCESS_FINE_LOCATION));

                if (!checkPermissions(permissions)) {

                    String[] permissionsArray = new String[permissions.size()];
                    permissionsArray = permissions.toArray(permissionsArray);

                    ActivityCompat.requestPermissions(this, permissionsArray, REQUEST_LOCATION);

                    permissionGranted = false;
                }

                if (permissionGranted) {
                    txRxScanner.startScan(Arrays.asList(filteredServiceUuids));
                    supportInvalidateOptionsMenu();
                }
                break;
            case R.id.menu_stop:
                txRxScanner.stopScan();
                supportInvalidateOptionsMenu();
                break;
            case R.id.menu_settings:

                final Intent intent = new Intent(ScanActivity.this, SettingsActivity.class);
                if (txRxScanner.isScanning()) {
                    txRxScanner.stopScan();
                }
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {

        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                txRxScanner.startScan(Arrays.asList(filteredServiceUuids));
                supportInvalidateOptionsMenu();
            }
        }

        if (requestCode == REQUEST_ENABLE_BT) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkForBluetoothEnabled();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void checkForBluetoothPermissionAndEnabled() {
        boolean permissionGranted = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            List<String> permissions = new ArrayList<>(Arrays.asList(permission.BLUETOOTH_CONNECT,
                    permission.BLUETOOTH_SCAN));

            if (!checkPermissions(permissions)) {
                String[] permissionsArray = new String[permissions.size()];
                permissionsArray = permissions.toArray(permissionsArray);

                ActivityCompat.requestPermissions(this, permissionsArray, REQUEST_ENABLE_BT);

                permissionGranted = false;
            }
        }

        if (permissionGranted) {
            checkForBluetoothEnabled();
        }
    }

    private void checkForBluetoothEnabled() {
        if (!BleChecker.isBluetoothEnabled(getApplicationContext())) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            activityResultLauncher.launch(enableBtIntent);
        }
    }

    private boolean checkPermissions(List<String> permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.device_toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_activity_main);

        if (!BleChecker.isBleSupported(getApplicationContext())) {
            Toast.makeText(this, R.string.error_ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        BluetoothAdapter bluetoothAdapter = BleChecker.getBtAdapter(getApplicationContext());
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        activityResultLauncher = registerForActivityResult(new StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        finish();
                    }
                });

        TxRxScanCallback txRxScanCallback = new TxRxScanCallback() {
            @Override
            public void afterStopScan() {
                runOnUiThread(() -> supportInvalidateOptionsMenu());
            }

            @Override
            public void onDeviceFound(final TxRxScanResult scanResult) {
                runOnUiThread(() -> bleDeviceListAdapter.addDevice(scanResult.getBluetoothDevice()));
            }
        };

        txRxScanner = new TxRxScanner(bluetoothAdapter, txRxScanCallback);

        RecyclerView recyclerView = findViewById(R.id.device_recycler_view);

        if (recyclerView != null) {
            recyclerView.setHasFixedSize(true);

            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            BleDeviceListAdapter.OnDeviceClickListener onDeviceClickListener = device -> runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(getApplicationContext(), permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                final Intent intent = new Intent(ScanActivity.this, DeviceActivity.class);
                intent.putExtra(DeviceActivity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(DeviceActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                if (txRxScanner.isScanning()) {
                    txRxScanner.stopScan();
                }
                startActivity(intent);
            });

            bleDeviceListAdapter = new BleDeviceListAdapter(onDeviceClickListener);
            recyclerView.setAdapter(bleDeviceListAdapter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (txRxScanner.isScanning()) {
            txRxScanner.stopScan();
        }
        bleDeviceListAdapter.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkForBluetoothPermissionAndEnabled();
    }
}