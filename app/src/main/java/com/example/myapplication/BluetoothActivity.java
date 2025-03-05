package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

    // Standard SPP UUID for Serial Port Profile
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // UI Components
    private CheckBox enable_bt, visible_bt;
    private ImageView imageView_bt;
    private TextView name_bt;
    private ListView listView_bt;

    // Bluetooth Components
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();

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
        enable_bt = findViewById(R.id.enable_bt);
        visible_bt = findViewById(R.id.visible_bt);
        imageView_bt = findViewById(R.id.imageView_bt);
        name_bt = findViewById(R.id.name_bt);
        listView_bt = findViewById(R.id.listView_bt);

        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();

        // Set up Bluetooth enable checkbox
        setupBluetoothEnableCheckbox();

        // Set up Bluetooth visibility checkbox
        setupBluetoothVisibilityCheckbox();

        // Set up device list view
        setupDeviceListView();
    }

    private void checkBluetoothPermissions() {
        // Request necessary permissions
        String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
        };

        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_BLUETOOTH
            );
        }
    }

    @SuppressLint("MissingPermission")
    private void setupBluetoothEnableCheckbox() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set initial state
        enable_bt.setChecked(bluetoothAdapter.isEnabled());

        enable_bt.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                // Enable Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 0);
            } else {
                // Disable Bluetooth
                bluetoothAdapter.disable();
                Toast.makeText(this, "Bluetooth Turned Off", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void setupBluetoothVisibilityCheckbox() {
        visible_bt.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                Intent discoverableIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(
                        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300
                );
                startActivityForResult(discoverableIntent, 0);
            }
        });
    }

    private void setupDeviceListView() {
        // List devices when image is clicked
        imageView_bt.setOnClickListener(v -> listPairedDevices());

        // Connect to device when selected
        listView_bt.setOnItemClickListener((parent, view, position, id) ->
                connectToDevice(deviceList.get(position))
        );
    }

    @SuppressLint("MissingPermission")
    private void listPairedDevices() {
        // Clear previous list
        deviceList.clear();

        // Get paired devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // Create list for adapter
        ArrayList<String> deviceNames = new ArrayList<>();
        for (BluetoothDevice device : pairedDevices) {
            deviceNames.add(device.getName() + "\n" + device.getAddress());
            deviceList.add(device);
        }

        // Set adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                deviceNames
        );
        listView_bt.setAdapter(adapter);

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
                    Toast.makeText(
                            BluetoothActivity.this,
                            "Connected to " + device.getName(),
                            Toast.LENGTH_SHORT
                    ).show();
                });

                // Start listening for data
                beginListenForData();

            } catch (IOException e) {
                Log.e(TAG, "Connection error", e);

                // Error message on main thread
                handler.post(() -> {
                    Toast.makeText(
                            BluetoothActivity.this,
                            "Connection Failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
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
                            Log.d(TAG, "Received: " + receivedData);
                            Toast.makeText(
                                    BluetoothActivity.this,
                                    "Received: " + receivedData,
                                    Toast.LENGTH_SHORT
                            ).show();
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
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_BLUETOOTH) {
            // Check if all permissions are granted
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(
                        this,
                        "Bluetooth permissions are required",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Close connections
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();

            // Shutdown executor
            if (executorService != null) {
                executorService.shutdown();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing connections", e);
        }
    }
}