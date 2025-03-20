package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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

    private Context context;

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

    private boolean needleChangedAfterAlarm = true;
    private boolean startingValidMess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // אתחול רכיבי ממשק המשתמש
        setupToolbarAndDrawer();
        initializeComponents();

        // אתחול מנהלי בלוטות'
        bluetoothManager_filterEsp = new BluetoothManager(this);
        bluetoothManager_filterEsp.setOnDataReceivedListener(this::handleReceivedData_FilterEsp);
        bluetoothManager_filterEsp.setOnConnectionStatusChangeListener(isConnected -> {
            isFilterEspConnected = isConnected;
            updateConnectionStatus();
        });
        bluetoothManager_filterEsp.initialize();

        // מנהל בלוטות' שני עבור ESP המחט
        bluetoothManager_needleEsp = new BluetoothManager(this);
        bluetoothManager_needleEsp.setOnDataReceivedListener(this::handleReceivedData_NeedleEsp);
        bluetoothManager_needleEsp.setOnConnectionStatusChangeListener(isConnected -> {
            isNeedleEspConnected = isConnected;
            updateConnectionStatus();
        });
        bluetoothManager_needleEsp.initialize();

        // הגדרת מאזין לחיצה על כפתור התחלת בדיקה
        startTestButton.setOnClickListener(v -> {
            if (!isMonitoring) {
                if (isFilterEspConnected && isNeedleEspConnected) {
                    startMonitoring();
                } else if (!isFilterEspConnected && isNeedleEspConnected) {
                    showESPSelectionDialog_FILTER();
                } else if (isFilterEspConnected) {
                    showESPSelectionDialog_NEEDLE();
                } else {
                    showBluetoothConnectionRequiredDialog();
                }
            } else {
                // אם כבר מנטרים, מאפשר למשתמש לאפס/להפעיל מחדש את הבדיקה
                resetMonitoring();
            }
        });

        // כפתור החלפת מחט - מוסתר בהתחלה
        changeNeedleButton.setOnClickListener(v -> {
            showNeedleChangeConfirmationDialog();
        });
    }

    private void resetMonitoring() {
        initializeComponents();
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
                    // פתיחת דיאלוג חיבור בלוטות'
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
                    // איפוס דגל החלפת מחט ועדכון סטטוס לרגיל
                    needToChangeNeedle = false;
                    needleChangedAfterAlarm = true;
                    changeNeedleButton.setVisibility(View.GONE);
                })
                .setNegativeButton(R.string.not_yet, null)
                .show();
    }

    private void startMonitoring() {
        isMonitoring = true;

        // שינוי מראה הכפתור
        startTestButton.setBackgroundResource(R.drawable.button_active);
        startTestButton.setText(R.string.test_in_progress);

        // עדכון סטטוס המציין שהניטור התחיל
        statusTextView.setText(R.string.status_waiting);
        statusIndicator.setBackgroundResource(R.drawable.status_yellow);
    }

    private void setupToolbarAndDrawer() {
        // הגדרת סרגל כלים
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // הגדרת מגירה
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // הגדרת מתג למגירה
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

        // הגדרת מצב התחלתי של הכפתור
        startTestButton.setBackgroundResource(R.drawable.status_gray);

        // הסתרת כפתור החלפת מחט בהתחלה
        changeNeedleButton.setVisibility(View.GONE);

        // איפוס סטטוס התחלתי - השארתו ריק לפני תחילת הבדיקה
        statusTextView.setText("");
        statusIndicator.setBackgroundResource(R.drawable.status_gray);
    }

    private void handleReceivedData_FilterEsp(String data) {
        // עיבוד נתונים רק אם הניטור פעיל
        if (!isMonitoring) return;

        // עיבוד נתונים מהתקן ESP של הפילטר
        if (data.contains("NEEDLE_OK")) {
            updateStatus(1);
        } else if (data.contains("START_INJECT") && startingValidMess) {
            if (!needleChangedAfterAlarm) {
                updateStatus(2);
            } else {
                if (!needToChangeNeedle) {
                    updateStatus(2);
                }
            }
        }
    }

    private void handleReceivedData_NeedleEsp(String data) {
        // עיבוד נתונים רק אם הניטור פעיל
        if (!isMonitoring) return;
    }

    private void updateStatus(int mode) {
        runOnUiThread(() -> {
            if (mode == 1) {
                startingValidMess = true;
                statusTextView.setText(R.string.status_ok);
                statusIndicator.setBackgroundResource(R.drawable.status_green);
                needleChangedAfterAlarm = false;

                // אם היינו במצב הזרקה קודם לכן, מאפס את אנימציית ההזרקה
                if (statusIndicator.getBackground() instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = (AnimationDrawable) statusIndicator.getBackground();
                    animationDrawable.stop();
                }

            } else if (mode == 2) {

                DatabaseHelper dbHelper = new DatabaseHelper(this);
                if (needToChangeNeedle) {
                    statusTextView.setText(R.string.status_change_needle);
                    statusIndicator.setBackgroundResource(R.drawable.status_red);
                    // צריך להחליף מחט, הצגת הודעה ולא להזריק
                    showNeedleChangeReminderNotification();
                    sendNeedleChangeNotification();
                } else {
                    needleChangedAfterAlarm = true;

                    Toast.makeText(this, "זוהתה חסימה מתחיל הזרקה", Toast.LENGTH_SHORT).show();
                    statusTextView.setText(R.string.status_injection_process);
                    statusIndicator.setBackgroundResource(R.drawable.status_animation);

                    // התחלת אנימציה
                    AnimationDrawable animationDrawable = (AnimationDrawable) statusIndicator.getBackground();
                    animationDrawable.start();
                    // המחט לא צריכה החלפה, לכן מבצע הזרקה
                    bluetoothManager_needleEsp.inject(dbHelper);
                    needToChangeNeedle = true; // הגדרת דגל ל-true לאחר הזרקה

                    new Handler().postDelayed(() -> {
                        if (isMonitoring) {
                            updateStatus(3);
                        }
                    }, 5000); // 5 שניות השהייה - התאם לפי הצורך
                }

            } else if (mode == 3) {
                // מצב נדרשת החלפת מחט
                statusTextView.setText(R.string.status_needle_changed);
                statusIndicator.setBackgroundResource(R.drawable.status_yellow);
            }
        });
    }

    private void showNeedleChangeReminderNotification() {
        // הצגת כפתור החלפת מחט
        changeNeedleButton.setVisibility(View.VISIBLE);
    }

    private void sendNeedleChangeNotification() {
        // יצירת ערוץ התראות עבור אנדרואיד 8.0+
        String channelId = "needle_channel";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Needle Status",
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // בניית ההתראה
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("התראת מחט")
                .setContentText("יש צורך בביצוע הזרקה עליך להחליף מחט")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // יצירת intent כאשר המשתמש לוחץ על ההתראה
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        // הצגת ההתראה
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // בדיקת הרשאות עבור אנדרואיד 13+
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1001, builder.build());
        }
    }

    private void updateConnectionStatus() {
        runOnUiThread(() -> {
            TextView connectionStatusText = findViewById(R.id.connection_status);
            if (isMonitoring && (!isFilterEspConnected || !isNeedleEspConnected)) {
                Toast.makeText(this, "ESP נותק הרץ מחדש", Toast.LENGTH_SHORT).show();
                resetMonitoring();
            }
            if (isFilterEspConnected && isNeedleEspConnected) {
                // שני ESPs מחוברים
                connectionStatusText.setText(R.string.both_connected);
                connectionStatusText.setTextColor(getResources().getColor(R.color.colorConnected));
            }
            else if (isNeedleEspConnected) {
                // רק ESP אחד מחובר - עדיין בסדר לפי הלוגיקה המקורית
                connectionStatusText.setText(R.string.needle_connected);
                connectionStatusText.setTextColor(getResources().getColor(R.color.colorPartialConnected));
            }
            else if (isFilterEspConnected) {
                // רק ESP אחד מחובר - עדיין בסדר לפי הלוגיקה המקורית
                connectionStatusText.setText(R.string.filter_connected);
                connectionStatusText.setTextColor(getResources().getColor(R.color.colorPartialConnected));
            }
            else {
                // אף ESP לא מחובר
                connectionStatusText.setText(R.string.disconnected);
                connectionStatusText.setTextColor(getResources().getColor(R.color.colorDisconnected));

                // איפוס מצב ניטור אם החיבור אבד במהלך הניטור
                if (isMonitoring) {
                    resetMonitoring();
                }
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // טיפול בלחיצות פריטי תצוגת ניווט
        int id = item.getItemId();

        if (id == R.id.nav_main) {
            // כבר במסך הראשי, לא עושה כלום
        } else if (id == R.id.nav_history) {
            startActivity(new Intent(this, HistoryActivity.class));
        } else if (id == R.id.nav_consult) {
            startActivity(new Intent(this, ConsultActivity.class));
        } else if (id == R.id.nav_info) {
            startActivity(new Intent(this, InfoActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_bluetooth) {
            // הצגת דיאלוג לבחירת ESP להתחבר אליו
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
                        // התחברות ל-ESP של הפילטר
                        bluetoothManager_filterEsp.showDeviceSelectionDialog();
                    } else {
                        // התחברות ל-ESP של המחט
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

    @Override
    protected void onPause() {
        super.onPause();
        if (isMonitoring) {
            resetMonitoring();
        }
    }
}