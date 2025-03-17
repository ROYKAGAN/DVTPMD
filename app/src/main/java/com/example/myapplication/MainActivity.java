package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private TextView statusTextView;

    private TextView bluetoothStatusTextView;
    private View statusIndicator;
    private Button startTestButton;
    private Button changeNeedleButton;

    private boolean needToChangeNeedle = false;
    private BluetoothManager bluetoothManager_filterEsp;
    private BluetoothManager bluetoothManager_needleEsp;
    private boolean isMonitoring = false;
    private boolean isFilterEspConnected = false;
    private boolean isNeedleEspConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        setupToolbarAndDrawer();
        initializeComponents();

        // Initialize Bluetooth managers - make sure we're maintaining the existing functionality
        // First BluetoothManager for the filter ESP
        bluetoothManager_filterEsp = new BluetoothManager(this);
        bluetoothManager_filterEsp.setOnDataReceivedListener(this::handleReceivedData_FilterEsp);
        bluetoothManager_filterEsp.setOnConnectionStatusChangeListener(isConnected -> {
            isFilterEspConnected = isConnected;
            updateConnectionStatus();
        });
        bluetoothManager_filterEsp.initialize();

        // Second BluetoothManager for the needle ESP
        bluetoothManager_needleEsp = new BluetoothManager(this);
        bluetoothManager_needleEsp.setOnDataReceivedListener(this::handleReceivedData_NeedleEsp);
        bluetoothManager_needleEsp.setOnConnectionStatusChangeListener(isConnected -> {
            isNeedleEspConnected = isConnected;
            updateConnectionStatus();
        });
        bluetoothManager_needleEsp.initialize();

        // Button click listener to start monitoring
        startTestButton.setOnClickListener(v -> {
            // Fix: Don't call onStop() here as it was in the original code
            if (!isMonitoring) {
                if (isFilterEspConnected && isNeedleEspConnected) { // Changed to OR to match original behavior
                    startMonitoring();
                } else if (!isFilterEspConnected && isNeedleEspConnected) {
                    showESPSelectionDialog_FILTER();
                } else if (isFilterEspConnected) {
                    showESPSelectionDialog_NEEDLE();
                } else {
                    showBluetoothConnectionRequiredDialog();
                }
            } else {
                // If already monitoring, this allows user to reset/restart the test
                resetMonitoring();
            }
        });

        // Change needle button - initially invisible
        changeNeedleButton.setOnClickListener(v -> {
            showNeedleChangeConfirmationDialog();
        });
    }

    private void resetMonitoring() {
        isMonitoring = false;
        startTestButton.setBackgroundResource(R.drawable.status_gray);
        startTestButton.setText(R.string.start_test);
        statusTextView.setText("");
        statusIndicator.setBackgroundResource(R.drawable.status_gray);
        changeNeedleButton.setVisibility(View.GONE);
    }

    private void showBluetoothConnectionRequiredDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.connection_required_title)
                .setMessage(R.string.connection_required_message)
                .setPositiveButton(R.string.connect_bluetooth, (dialog, which) -> {
                    // Open Bluetooth connection dialog
                    showESPSelectionDialog();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showNeedleChangeConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.needle_change_title)
                .setMessage(R.string.needle_change_confirmation)
                .setPositiveButton(R.string.yes_changed, (dialog, which) -> {
                    // Reset needle change flag and update status to normal
                    needToChangeNeedle = false;
                    changeNeedleButton.setVisibility(View.GONE);
                    updateStatus(1); // Back to green status

                    // Important: Re-enable the start test button
                    startTestButton.setEnabled(true);
                    startTestButton.setBackgroundResource(R.drawable.status_gray);
                    startTestButton.setText(R.string.start_test);
                })
                .setNegativeButton(R.string.not_yet, null)
                .show();
    }

    private void startMonitoring() {
        isMonitoring = true;

        // Change button appearance
        startTestButton.setBackgroundResource(R.drawable.button_active);
        startTestButton.setText(R.string.test_in_progress);
        // Important: Don't disable the button - just change its appearance
        // startTestButton.setEnabled(false); - removed this line

        // Update status to indicate monitoring has started
        statusTextView.setText(R.string.status_waiting);
        statusIndicator.setBackgroundResource(R.drawable.status_yellow);
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
        bluetoothStatusTextView = findViewById(R.id.connection_status);
        statusIndicator = findViewById(R.id.status_indicator);
        startTestButton = findViewById(R.id.start_test_button);
        changeNeedleButton = findViewById(R.id.change_needle_button);

        // Set initial button state
        startTestButton.setBackgroundResource(R.drawable.status_gray);

        // Initially hide the change needle button
        changeNeedleButton.setVisibility(View.GONE);

        // Clear initial status - leave it blank before test starts
        statusTextView.setText("");
        statusIndicator.setBackgroundResource(R.drawable.status_gray);
    }

    private void handleReceivedData_FilterEsp(String data) {
        // Only process data if monitoring is active
        if (!isMonitoring) return;

        // Process data from Filter ESP device
        if (data.contains("NEEDLE_OK")) {
            updateStatus(1);
        } else if (data.contains("START_INJECT")) {
            updateStatus(2);
        }
    }

    private void handleReceivedData_NeedleEsp(String data) {
        // Only process data if monitoring is active
        if (!isMonitoring) return;

        // Process data from Needle ESP device
        if (data.contains("NEEDLE_CHANGE")) {
            updateStatus(3); // Needle needs to be changed
        }
    }

    private void updateStatus(int mode) {
        runOnUiThread(() -> {
            if (mode == 1) {
                statusTextView.setText(R.string.status_ok);
                statusIndicator.setBackgroundResource(R.drawable.status_green);

                // If previously in injection mode, reset injection animation
                if (statusIndicator.getBackground() instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = (AnimationDrawable) statusIndicator.getBackground();
                    animationDrawable.stop();
                }

            } else if (mode == 2) {
                statusTextView.setText(R.string.status_injection_process);
                statusIndicator.setBackgroundResource(R.drawable.status_animation);

                // Start Animation
                AnimationDrawable animationDrawable = (AnimationDrawable) statusIndicator.getBackground();
                animationDrawable.start();

                DatabaseHelper dbHelper = new DatabaseHelper(this);
                bluetoothManager_needleEsp.inject(dbHelper);
                needToChangeNeedle = true;

                // After injection completes, automatically move to green state
                // But keep track of needle change requirement
                new Handler().postDelayed(() -> {
                    if (isMonitoring) {
                        statusTextView.setText(R.string.status_ok);
                        statusIndicator.setBackgroundResource(R.drawable.status_green);

                        // Show needle change reminder notification
                        if (needToChangeNeedle) {
                            showNeedleChangeReminderNotification();
                        }
                    }
                }, 5000); // 5 seconds delay - adjust as needed

            } else if (mode == 3) {
                // Needle change required mode
                statusTextView.setText(R.string.status_change_needle);
                statusIndicator.setBackgroundResource(R.drawable.status_yellow);

                // Show the change needle button
                changeNeedleButton.setVisibility(View.VISIBLE);

                // Show a dialog alerting the user
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.needle_change_required_title)
                        .setMessage(R.string.needle_change_required_message)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });
    }

    private void showNeedleChangeReminderNotification() {
        // Show the change needle button
        changeNeedleButton.setVisibility(View.VISIBLE);
    }

    private void updateConnectionStatus() {
        runOnUiThread(() -> {
            TextView connectionStatusText = findViewById(R.id.connection_status);

            if (isFilterEspConnected && isNeedleEspConnected) {
                // Both ESPs are connected
                connectionStatusText.setText(R.string.both_connected);
                connectionStatusText.setTextColor(getResources().getColor(R.color.colorConnected));
            }
            else if (isNeedleEspConnected) {
                // Only one ESP is connected - which is still okay based on original logic
                connectionStatusText.setText(R.string.needle_connected);
                connectionStatusText.setTextColor(getResources().getColor(R.color.colorPartialConnected));
            }
            else if (isFilterEspConnected) {
                // Only one ESP is connected - which is still okay based on original logic
                connectionStatusText.setText(R.string.filter_connected);
                connectionStatusText.setTextColor(getResources().getColor(R.color.colorPartialConnected));
            }
            else {
                // Neither ESP is connected
                connectionStatusText.setText(R.string.disconnected);
                connectionStatusText.setTextColor(getResources().getColor(R.color.colorDisconnected));

                // Reset monitoring state if connection is lost during monitoring
                if (isMonitoring) {
                    resetMonitoring();
                }
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
            // Show a dialog to select which ESP to connect to
            showESPSelectionDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showESPSelectionDialog() {
        String[] options = {
                getString(R.string.filter_esp),
                getString(R.string.needle_esp)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_esp_device)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Connect to Filter ESP
                        bluetoothManager_filterEsp.showDeviceSelectionDialog();
                    } else {
                        // Connect to Needle ESP
                        bluetoothManager_needleEsp.showDeviceSelectionDialog();
                    }
                })
                .show();
    }

    private void showESPSelectionDialog_NEEDLE() {
        String[] options = {
                getString(R.string.needle_esp)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_esp_device)
                .setItems(options, (dialog, which) -> {
                        bluetoothManager_needleEsp.showDeviceSelectionDialog();

                })
                .show();
    }
private void showESPSelectionDialog_FILTER() {
        String[] options = {
                getString(R.string.filter_esp),
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_esp_device)
                .setItems(options, (dialog, which) -> {
                    bluetoothManager_filterEsp.showDeviceSelectionDialog();

                })
                .show();
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
        bluetoothManager_needleEsp.close();
    }

    // Reset monitoring when activity is paused
    @Override
    protected void onPause() {
        super.onPause();
        if (isMonitoring) {
            resetMonitoring();
        }
    }
}