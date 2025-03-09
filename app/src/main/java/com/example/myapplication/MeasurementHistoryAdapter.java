package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MeasurementHistoryAdapter extends RecyclerView.Adapter<MeasurementHistoryAdapter.ViewHolder> {

    private List<HistoryActivity.MeasurementRecord> historyData;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM, yyyy", Locale.forLanguageTag("he-IL"));
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("he-IL"));

    public MeasurementHistoryAdapter(List<HistoryActivity.MeasurementRecord> historyData) {
        this.historyData = historyData;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.measurement_history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryActivity.MeasurementRecord record = historyData.get(position);

        holder.dateText.setText(dateFormat.format(record.getTimestamp()));
        holder.timeText.setText(timeFormat.format(record.getTimestamp()));
        holder.valueText.setText(String.valueOf(record.getValue()));

        // Set status text and background
        holder.statusText.setText(getStatusText(record.getStatus()));

        // You'll need to define these drawables and colors in your resources
        switch(record.getStatus().toLowerCase()) {
            case "normal":
                holder.statusText.setBackgroundResource(R.drawable.status_green);
                holder.statusText.setText("תקין");
                break;
            case "warning":
                holder.statusText.setBackgroundResource(R.drawable.status_warning);
                holder.statusText.setText("אזהרה");
                break;
            case "danger":
                holder.statusText.setBackgroundResource(R.drawable.status_red);
                holder.statusText.setText("סכנה");
                break;
            default:
                holder.statusText.setBackgroundColor(Color.GRAY);
                holder.statusText.setText("לא ידוע");
                break;
        }
    }

    @Override
    public int getItemCount() {
        return historyData.size();
    }

    private String getStatusText(String status) {
        switch(status.toLowerCase()) {
            case "normal":
                return "תקין";
            case "warning":
                return "אזהרה";
            case "danger":
                return "סכנה";
            default:
                return "לא ידוע";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;
        TextView timeText;
        TextView valueText;
        TextView statusText;

        ViewHolder(View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.text_date);
            timeText = itemView.findViewById(R.id.text_time);
            valueText = itemView.findViewById(R.id.text_value);
            statusText = itemView.findViewById(R.id.text_status);
        }
    }
}