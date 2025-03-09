package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private TextView statusTextView;
    private View statusIndicator;
    private BluetoothManager bluetoothManager_filterEsp;
    private BluetoothManager bluetoothManager_needleEsp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        setupToolbarAndDrawer();
        initializeComponents();



        // Initialize and start Bluetooth connection
        bluetoothManager_filterEsp = new BluetoothManager(this);
        bluetoothManager_filterEsp.setOnDataReceivedListener(this::handleReceivedData_FilterEsp);
        bluetoothManager_filterEsp.setOnConnectionStatusChangeListener(this::updateConnectionStatus);
        bluetoothManager_filterEsp.initialize();

        // Start listening for data
        bluetoothManager_needleEsp = new BluetoothManager(this);
        bluetoothManager_needleEsp.setOnDataReceivedListener(this::handleReceivedData_NeedleEsp);
        bluetoothManager_needleEsp.setOnConnectionStatusChangeListener(this::updateConnectionStatus);
        bluetoothManager_needleEsp.initialize();
    }

    private void setupToolbarAndDrawer() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Setup toggle for drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void initializeComponents() {
        statusTextView = findViewById(R.id.status_text);
        statusIndicator = findViewById(R.id.status_indicator);
    }

    private void handleReceivedData_FilterEsp(String data) {
        // Process data from ESP device
        if (data.contains("NEEDLE_OK")) {
            updateStatus(1);
        } else if (data.contains("NEEDLE_CHANGE")) {
            updateStatus(2);
        }
    }
    private void handleReceivedData_NeedleEsp(String data) {
        // Process data from ESP device
        if (data.contains("NEEDLE_ACTION")) {
            updateStatus(3);
        }
    }

    private void updateStatus(int mode) {
        runOnUiThread(() -> {
            if (mode == 1) {
                statusTextView.setText(R.string.status_ok);
                statusIndicator.setBackgroundResource(R.drawable.status_green);
            } else if(mode == 2) {
                statusTextView.setText(R.string.status_change_needle);
                statusIndicator.setBackgroundResource(R.drawable.status_red);
            } else{
                statusTextView.setText(R.string.status_injection_process);
                statusIndicator.setBackgroundResource(R.drawable.status_blue);

                DatabaseHelper dbHelper = new DatabaseHelper(this);
                long timestamp = System.currentTimeMillis(); // Current time
                int value = bluetoothManager_needleEsp.getInjectionValue(); // Example measurement value (adjust as needed)
                String status = bluetoothManager_needleEsp.getInjectionStatus(); // Example status
                dbHelper.insertMeasurementRecord(timestamp, value, status);
            }
        });
    }


    private void updateConnectionStatus(boolean isConnected) {
        runOnUiThread(() -> {
            TextView connectionStatusText = findViewById(R.id.connection_status);
            if (isConnected) {
                connectionStatusText.setText(R.string.connected);
                connectionStatusText.setTextColor(getResources().getColor(R.color.colorConnected));
            } else {
                connectionStatusText.setText(R.string.disconnected);
                connectionStatusText.setTextColor(getResources().getColor(R.color.colorDisconnected));
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks
        int id = item.getItemId();

        if (id == R.id.nav_main) {
            // Already on main, do nothing
        } else if (id == R.id.nav_history) {
            startActivity(new Intent(this, HistoryActivity.class));
        } else if (id == R.id.nav_consult) {
            startActivity(new Intent(this, ConsultActivity.class));
        } else if (id == R.id.nav_info) {
            startActivity(new Intent(this, InfoActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_bluetooth) {
            bluetoothManager_filterEsp.showDeviceSelectionDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothManager_filterEsp.close();
    }
}
