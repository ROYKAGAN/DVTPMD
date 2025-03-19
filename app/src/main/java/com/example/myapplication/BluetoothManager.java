package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothManager {
    private static final String TAG = "BluetoothManager";
    private static final int PERMISSION_REQUEST_BLUETOOTH = 1;
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Context context;

    private String latestReceivedData = ""; // 🔹 Fix: Declare this variable

    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler;
    private final ExecutorService executorService;

    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private boolean isConnected = false;

    private OnDataReceivedListener dataListener;
    private OnConnectionStatusChangeListener connectionListener;

    public interface OnDataReceivedListener {
        void onDataReceived(String data);
    }

    public interface OnConnectionStatusChangeListener {
        void onConnectionStatusChange(boolean isConnected);
    }

    public BluetoothManager(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.handler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void setOnDataReceivedListener(OnDataReceivedListener listener) {
        this.dataListener = listener;
    }

    public void setOnConnectionStatusChangeListener(OnConnectionStatusChangeListener listener) {
        this.connectionListener = listener;
    }

    public void initialize() {
        checkBluetoothPermissions();
        enableBluetooth();
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        PERMISSION_REQUEST_BLUETOOTH);
            } else {
                enableBluetooth();
            }
        } else {
            enableBluetooth();
        }
    }

    @SuppressLint("MissingPermission")
    private void enableBluetooth() {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Toast.makeText(context, R.string.enabling_bluetooth, Toast.LENGTH_SHORT).show();
            bluetoothAdapter.enable();
            //handler.postDelayed(this::attemptAutoConnect, 2000); // Give Bluetooth time to enable
        }
    }

    @SuppressLint("MissingPermission")
    private void attemptAutoConnect() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return;
        }

        executorService.execute(() -> {
            try {
                // Look for ESP device in paired devices
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                BluetoothDevice espDevice = null;

                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    if (deviceName != null && (deviceName.contains("ESP") || deviceName.contains("DVT"))) {
                        espDevice = device;
                        break;
                    }
                }

                if (espDevice != null) {
                    connectToDevice(espDevice);
                } else {
                    // If no ESP device found, show device selection dialog
                    handler.post(this::showDeviceSelectionDialog);
                }

            } catch (Exception e) {
                Log.e(TAG, "Auto-connect error", e);
            }
        });
    }

    @SuppressLint("MissingPermission")
    public void showDeviceSelectionDialog() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(context, R.string.bluetooth_not_available, Toast.LENGTH_SHORT).show();

            return;
        }

        // Get paired devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        final ArrayList<BluetoothDevice> deviceList = new ArrayList<>(pairedDevices);

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.bluetooth_device_selection, null);
        builder.setView(dialogView);
        builder.setTitle(R.string.select_device);

        // Setup device list
        final ListView deviceListView = dialogView.findViewById(R.id.device_list);
        DeviceListAdapter adapter = new DeviceListAdapter(context, deviceList);
        deviceListView.setAdapter(adapter);

        // Create and show dialog
        final AlertDialog dialog = builder.create();
        dialog.show();

        // Set item click listener
        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = deviceList.get(position);
            connectToDevice(device);
            dialog.dismiss();
        });
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        executorService.execute(() -> {
            try {
                // Close existing connection if open
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                    isConnected = false;
                    if (connectionListener != null) {
                        connectionListener.onConnectionStatusChange(false);
                    }
                }

                // Create socket
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);

                // Connect to the socket
                bluetoothSocket.connect();

                // Get streams
                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();

                // Update connection status
                isConnected = true;
                if (connectionListener != null) {
                    connectionListener.onConnectionStatusChange(true);
                }

                // Success message on main thread
//                handler.post(() -> {
//                    Toast.makeText(
//                            context,
//                            context.getString(R.string.connected_to) + " " + device.getName(),
//                            Toast.LENGTH_SHORT
//                    ).show();
//                });

                // Start listening for data
                beginListenForData();

            } catch (IOException e) {
                Log.e(TAG, "Connection error", e);
                isConnected = false;
                if (connectionListener != null) {
                    connectionListener.onConnectionStatusChange(false);
                }

                // Error message on main thread
                handler.post(() -> {
                    Toast.makeText(
                            context,
                            R.string.connection_failed,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        });
    }

    public void sendCommand(String command) {
        if (isConnected && outputStream != null) {
            executorService.execute(() -> {
                try {
                    outputStream.write(command.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    Log.e(TAG, "Error sending command", e);
                    handler.post(() -> {
                        Toast.makeText(context, R.string.send_failed, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
        }
    }

    private void beginListenForData() {
        Thread thread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (isConnected) {
                try {
                    if (inputStream == null) break;

                    bytes = inputStream.read(buffer);
                    if (bytes != -1) {
                        latestReceivedData = new String(buffer, 0, bytes).trim(); // 🔹 Store last received data

                        // Send to listener
                        if (dataListener != null) {
                            dataListener.onDataReceived(latestReceivedData);
                        }

                        Log.d(TAG, "Received: " + latestReceivedData);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Data reception error", e);
                    isConnected = false;
                    if (connectionListener != null) {
                        handler.post(() -> connectionListener.onConnectionStatusChange(false));
                    }
                    break;
                }
            }
        });
        thread.start();
    }

    public void close() {
        try {
            isConnected = false;
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing Bluetooth socket", e);
        }
    }

    // Custom Adapter for Bluetooth Devices
    private static class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
        @SuppressLint("MissingPermission")
        public DeviceListAdapter(Context context, ArrayList<BluetoothDevice> devices) {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1, devices);
        }

        @SuppressLint("MissingPermission")
        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            BluetoothDevice device = getItem(position);

            android.widget.TextView text1 = view.findViewById(android.R.id.text1);
            android.widget.TextView text2 = view.findViewById(android.R.id.text2);

            text1.setText(device.getName() != null ? device.getName() : getContext().getString(R.string.unknown_device));
            text2.setText(device.getAddress());

            return view;
        }
    }


    /**
     * Extracts the injection value from the latest received data.
     * Expected format: "INJECTION_VALUE:75"
     * @return the extracted injection value, or -1 if parsing fails.
     */
    public int getInjectionValue() {
        if (latestReceivedData.contains("INJECTION_VALUE:")) {
            try {
                String[] parts = latestReceivedData.split(":");
                return Integer.parseInt(parts[1].trim()); // Extract numeric value
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Error parsing injection value", e);
            }
        }
        return -1; // Default if no valid data is found
    }

    /**
     * Extracts the injection status from the latest received data.
     * Expected format: "STATUS:OK" or "STATUS:WARNING"
     * @return the extracted status as a string (e.g., "OK", "WARNING"), or "UNKNOWN" if parsing fails.
     */
    public String getInjectionStatus() {
        if (latestReceivedData.contains("STATUS:")) {
            try {
                String[] parts = latestReceivedData.split(":");
                return parts[1].trim(); // Extract status
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Error parsing injection status", e);
            }
        }
        return "UNKNOWN"; // Default if no valid status is found
    }

    public void inject(DatabaseHelper dbHelper) {
        if (!isConnected) {
            Log.e(TAG, "Cannot inject - not connected to device");
            Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // First reset the actuator
        sendCommand("RESET\n");

        // Add a listener for the ESP32 responses
        OnDataReceivedListener originalListener = dataListener;

        setOnDataReceivedListener(new OnDataReceivedListener() {
            @Override
            public void onDataReceived(String data) {
                if (data.contains("ACTUATOR_RESET_COMPLETE")) {
                    // Once reset is complete, send the inject command
                    sendCommand("INJECT\n");
                }
                else if (data.contains("INJECTION_COMPLETE")) {
                    // When injection is complete, get the data and store it
                    long timestamp = System.currentTimeMillis();
                    int value = getInjectionValue();
                    String status = getInjectionStatus();

                    // Store in database
                    dbHelper.insertMeasurementRecord(timestamp, value, status);

                    // Notify UI on main thread
                    handler.post(() -> {
                        Toast.makeText(context,
                                "הזרקה הסתיימה ",
                                Toast.LENGTH_SHORT).show();
                    });

                    // Restore original listener
                    setOnDataReceivedListener(originalListener);
                }
            }
        });
    }
}
