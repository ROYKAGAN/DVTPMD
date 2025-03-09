package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREF_NAME = "DVTAppPrefs";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_AGE = "user_age";
    private static final String KEY_HEIGHT = "user_height";
    private static final String KEY_WEIGHT = "user_weight";
    private static final String KEY_MEDICAL_HISTORY = "user_medical_history";
    private static final String KEY_MEDICATIONS = "user_medications";

    private Button backButton;
    private TextView titleTextView;
    private EditText nameEditText;
    private EditText ageEditText;
    private EditText heightEditText;
    private EditText weightEditText;
    private EditText medicalHistoryEditText;
    private EditText medicationsEditText;
    private Button saveButton;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initializeViews();
        loadUserData();
        setupListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.btn_back);
        titleTextView = findViewById(R.id.text_title);
        nameEditText = findViewById(R.id.edit_name);
        ageEditText = findViewById(R.id.edit_age);
        heightEditText = findViewById(R.id.edit_height);
        weightEditText = findViewById(R.id.edit_weight);
        medicalHistoryEditText = findViewById(R.id.edit_medical_history);
        medicationsEditText = findViewById(R.id.edit_medications);
        saveButton = findViewById(R.id.btn_save);

        // Set text programmatically if needed (or use strings.xml)
        titleTextView.setText("הפרופיל שלי");
        saveButton.setText("שמור פרטים");
    }

    private void loadUserData() {
        nameEditText.setText(sharedPreferences.getString(KEY_NAME, ""));
        ageEditText.setText(sharedPreferences.getString(KEY_AGE, ""));
        heightEditText.setText(sharedPreferences.getString(KEY_HEIGHT, ""));
        weightEditText.setText(sharedPreferences.getString(KEY_WEIGHT, ""));
        medicalHistoryEditText.setText(sharedPreferences.getString(KEY_MEDICAL_HISTORY, ""));
        medicationsEditText.setText(sharedPreferences.getString(KEY_MEDICATIONS, ""));
    }

    private void setupListeners() {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserData();
            }
        });
    }

    private void saveUserData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(KEY_NAME, nameEditText.getText().toString());
        editor.putString(KEY_AGE, ageEditText.getText().toString());
        editor.putString(KEY_HEIGHT, heightEditText.getText().toString());
        editor.putString(KEY_WEIGHT, weightEditText.getText().toString());
        editor.putString(KEY_MEDICAL_HISTORY, medicalHistoryEditText.getText().toString());
        editor.putString(KEY_MEDICATIONS, medicationsEditText.getText().toString());

        editor.apply();

        Toast.makeText(this, "פרטים נשמרו בהצלחה", Toast.LENGTH_SHORT).show();
    }
}