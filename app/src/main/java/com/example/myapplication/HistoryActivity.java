package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistory;
    private Button backButton;
    private TextView titleTextView;
    private TextView emptyHistoryText;

    // Example adapter - you'll need to create this class
    private MeasurementHistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initializeViews();
        setupListeners();
        loadHistoryData();
    }

    private void initializeViews() {
        recyclerViewHistory = findViewById(R.id.recycler_history);
        backButton = findViewById(R.id.btn_back);
        titleTextView = findViewById(R.id.text_title);
        emptyHistoryText = findViewById(R.id.text_empty_history);

        // Set text programmatically (or use strings.xml)
        titleTextView.setText("היסטוריית מדידות");

        // Setup recycler view
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadHistoryData() {
        // This would be replaced with data from your database
        List<MeasurementRecord> historyData = getDummyHistoryData();

        if (historyData.isEmpty()) {
            recyclerViewHistory.setVisibility(View.GONE);
            emptyHistoryText.setVisibility(View.VISIBLE);
        } else {
            recyclerViewHistory.setVisibility(View.VISIBLE);
            emptyHistoryText.setVisibility(View.GONE);

            historyAdapter = new MeasurementHistoryAdapter(historyData);
            recyclerViewHistory.setAdapter(historyAdapter);
        }
    }

    // Dummy method to simulate data - replace with actual data from your storage
    private List<MeasurementRecord> getDummyHistoryData() {
        List<MeasurementRecord> data = new ArrayList<>();

        // Sample data - remove or replace this with actual database queries
        // data.add(new MeasurementRecord(new Date(), 75, "Normal"));
        // data.add(new MeasurementRecord(new Date(System.currentTimeMillis() - 86400000), 85, "Warning"));

        return data;
    }

    // Example model class - adjust according to your actual data structure
    public static class MeasurementRecord {
        private Date timestamp;
        private int value;
        private String status;

        public MeasurementRecord(Date timestamp, int value, String status) {
            this.timestamp = timestamp;
            this.value = value;
            this.status = status;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public int getValue() {
            return value;
        }

        public String getStatus() {
            return status;
        }
    }

    // You'll need to create an adapter class like this:
    /*
    public class MeasurementHistoryAdapter extends RecyclerView.Adapter<MeasurementHistoryAdapter.ViewHolder> {
        private List<MeasurementRecord> historyData;

        // Constructor, onCreateViewHolder, onBindViewHolder, getItemCount methods
    }
    */
}