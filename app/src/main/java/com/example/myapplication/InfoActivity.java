package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class InfoActivity extends AppCompatActivity {

    private Button backButton;
    private TextView titleTextView;
    private CardView whatIsDvtCard;
    private CardView symptomsCard;
    private CardView treatmentCard;
    private CardView preventionCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.btn_back);
        titleTextView = findViewById(R.id.text_title);
        whatIsDvtCard = findViewById(R.id.card_what_is_dvt);
        symptomsCard = findViewById(R.id.card_symptoms);
        treatmentCard = findViewById(R.id.card_treatment);
        preventionCard = findViewById(R.id.card_prevention);

        // Set text programmatically if needed (or use strings.xml)
        titleTextView.setText("מידע על DVT");
    }

    private void setupListeners() {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        whatIsDvtCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open detailed information about DVT
                // Intent intent = new Intent(InfoActivity.this, DetailedInfoActivity.class);
                // intent.putExtra("info_type", "what_is_dvt");
                // startActivity(intent);
            }
        });

        symptomsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open symptoms information
                // Intent intent = new Intent(InfoActivity.this, DetailedInfoActivity.class);
                // intent.putExtra("info_type", "symptoms");
                // startActivity(intent);
            }
        });

        treatmentCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open treatment information
                // Intent intent = new Intent(InfoActivity.this, DetailedInfoActivity.class);
                // intent.putExtra("info_type", "treatment");
                // startActivity(intent);
            }
        });

        preventionCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open prevention information
                // Intent intent = new Intent(InfoActivity.this, DetailedInfoActivity.class);
                // intent.putExtra("info_type", "prevention");
                // startActivity(intent);
            }
        });
    }
}