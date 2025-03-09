package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ConsultActivity extends AppCompatActivity {

    private CardView healthProvidersCard;
    private CardView emergencyCard;
    private CardView faqCard;
    private Button backButton;
    private TextView titleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consult);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        healthProvidersCard = findViewById(R.id.card_health_providers);
        emergencyCard = findViewById(R.id.card_emergency);
        faqCard = findViewById(R.id.card_faq);
        backButton = findViewById(R.id.btn_back);
        titleTextView = findViewById(R.id.text_title);

        // Set text programmatically if needed (or use strings.xml)
        titleTextView.setText("ייעוץ והכוונה");
    }

    private void setupListeners() {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        healthProvidersCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open health providers list or contact information
                // Intent intent = new Intent(ConsultActivity.this, HealthProvidersActivity.class);
                // startActivity(intent);
            }
        });

        emergencyCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show emergency information or dial emergency number
                // Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:101"));
                // startActivity(intent);
            }
        });

        faqCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open FAQ section
                // Intent intent = new Intent(ConsultActivity.this, FAQActivity.class);
                // startActivity(intent);
            }
        });
    }
}