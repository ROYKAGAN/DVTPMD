package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

public class BluetoothActivity extends AppCompatActivity {
    private static final String TAG = "BluetoothActivity";
    private static final int PERMISSION_REQUEST_BLUETOOTH = 1;
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // UI Components
    private Button scanButton, sendCommandButton;
    private TextView statusTextView, dataTextView;
    private ListView deviceListView;

    // Bluetooth Components
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private DeviceListAdapter deviceListAdapter;

    // Bluetooth Connection Variables
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Handler handler;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        // Initialize UI components
        initializeComponents();

        // Check and request Bluetooth permissions
        checkBluetoothPermissions();
    }

    private void initializeComponents() {
        // Find views
        scanButton = findViewById(R.id.scanButton);
        sendCommandButton = findViewById(R.id.sendCommandButton);
        statusTextView = findViewById(R.id.statusTextView);
        dataTextView = findViewById(R.id.dataTextView);
        deviceListView = findViewById(R.id.deviceListView);

        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();

        // Setup device list adapter
        deviceListAdapter = new DeviceListAdapter(this, deviceList);
        deviceListView.setAdapter(deviceListAdapter);

        // Setup button listeners
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        // Scan for devices
        scanButton.setOnClickListener(v -> listPairedDevices());

        // Device selection
        deviceListView.setOnItemClickListener((parent, view, position, id) ->
                connectToDevice(deviceList.get(position))
        );

        // Send test command
        sendCommandButton.setOnClickListener(v -> sendTestCommand());
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        PERMISSION_REQUEST_BLUETOOTH);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void listPairedDevices() {
        // Clear previous list
        deviceList.clear();

        // Get paired devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // Add to list
        deviceList.addAll(pairedDevices);

        // Update adapter
        deviceListAdapter.notifyDataSetChanged();

        Toast.makeText(this, "Paired Devices Listed", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        executorService.execute(() -> {
            try {
                // Close existing connection if open
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }

                // Create socket
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);

                // Connect to the socket
                bluetoothSocket.connect();

                // Get streams
                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();

                // Success message on main thread
                handler.post(() -> {
                    statusTextView.setText("Connected to " + device.getName());
                    Toast.makeText(
                            BluetoothActivity.this,
                            "Connected to " + device.getName(),
                            Toast.LENGTH_SHORT
                    ).show();

                    // Update device list to show connection status
                    deviceListAdapter.notifyDataSetChanged();
                });

                // Start listening for data
                beginListenForData();

            } catch (IOException e) {
                Log.e(TAG, "Connection error", e);

                // Error message on main thread
                handler.post(() -> {
                    statusTextView.setText("Connection Failed");
                    Toast.makeText(
                            BluetoothActivity.this,
                            "Connection Failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void sendTestCommand() {
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            try {
                outputStream.write("TEST_COMMAND".getBytes());
                outputStream.flush();
                Toast.makeText(this, "Test Command Sent", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e(TAG, "Error sending command", e);
                Toast.makeText(this, "Failed to send command", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_SHORT).show();
        }
    }

    private void beginListenForData() {
        Thread thread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    // Check if input stream exists
                    if (inputStream == null) break;

                    // Read incoming bytes
                    bytes = inputStream.read(buffer);
                    if (bytes != -1) {
                        final String receivedData = new String(buffer, 0, bytes);

                        // Post to main thread
                        handler.post(() -> {
                            dataTextView.setText(receivedData);
                            Log.d(TAG, "Received: " + receivedData);
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Data reception error", e);
                    break;
                }
            }
        });
        thread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close Bluetooth connection when activity is destroyed
        try {
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
    private class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
        public DeviceListAdapter(Context context, ArrayList<BluetoothDevice> devices) {
            super(context, 0, devices);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position
            BluetoothDevice device = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.bluetooth_device_item, parent, false);
            }

            // Lookup view for data population
            TextView deviceName = convertView.findViewById(R.id.deviceName);
            TextView deviceAddress = convertView.findViewById(R.id.deviceAddress);
            ImageView connectionStatus = convertView.findViewById(R.id.connectionStatus);

            // Populate the data into the template view using the data object
            deviceName.setText(device.getName() != null ? device.getName() : "Unknown Device");
            deviceAddress.setText(device.getAddress());

            // Set connection status icon
            if (bluetoothSocket != null && bluetoothSocket.isConnected()
                    && device.getAddress().equals(bluetoothSocket.getRemoteDevice().getAddress())) {
                connectionStatus.setImageResource(android.R.drawable.presence_online);
            } else {
                connectionStatus.setImageResource(android.R.drawable.presence_offline);
            }

            // Return the completed view to render on screen
            return convertView;
        }
    }
}