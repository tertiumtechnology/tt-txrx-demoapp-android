package com.tertiumtechnology.demoapp.util;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tertiumtechnology.demoapp.R;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class BleDeviceListAdapter extends RecyclerView.Adapter<BleDeviceListAdapter.ViewHolder> {

    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final AppCompatTextView deviceAddress;
        final AppCompatTextView deviceName;

        ViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceAddress = itemView.findViewById(R.id.device_address);
        }

        private void bind(final BluetoothDevice device, final OnDeviceClickListener listener) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(itemView.getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            final String deviceNameString = device.getName();
            if (deviceNameString != null && deviceNameString.length() > 0) {
                deviceName.setText(deviceNameString);
            }
            else {
                deviceName.setText(R.string.unknown_device);
            }
            deviceAddress.setText(device.getAddress());

            itemView.setOnClickListener(v -> listener.onDeviceClick(device));
        }
    }

    private final OnDeviceClickListener listener;
    private final ArrayList<BluetoothDevice> bleDevices;

    public BleDeviceListAdapter(OnDeviceClickListener listener) {
        this.bleDevices = new ArrayList<>();
        this.listener = listener;
    }

    public void addDevice(BluetoothDevice device) {
        if (!bleDevices.contains(device)) {
            bleDevices.add(device);
            notifyItemInserted(bleDevices.size() - 1);
        }
    }

    public void clear() {
        int size = bleDevices.size();
        bleDevices.clear();
        notifyItemRangeRemoved(0, size);
    }

    @Override
    public int getItemCount() {
        return bleDevices.size();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(bleDevices.get(position), listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list_item, parent, false);

        return new ViewHolder(view);
    }
}