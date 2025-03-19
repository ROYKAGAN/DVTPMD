package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Info
    private static final String DATABASE_NAME = "injection_history.db";
    private static final int DATABASE_VERSION = 1;

    // Table Info
    private static final String TABLE_HISTORY = "history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_VALUE = "value";
    private static final String COLUMN_STATUS = "status";

    // Create Table Query
    private static final String CREATE_TABLE_HISTORY = "CREATE TABLE " + TABLE_HISTORY + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_TIMESTAMP + " INTEGER, "
            + COLUMN_VALUE + " INTEGER, "
            + COLUMN_STATUS + " TEXT"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_HISTORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    // Method to insert a new record into the database
    public void insertMeasurementRecord(long timestamp, int value, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "INSERT INTO " + TABLE_HISTORY + " ("
                + COLUMN_TIMESTAMP + ", "
                + COLUMN_VALUE + ", "
                + COLUMN_STATUS + ") VALUES ("
                + timestamp + ", "
                + value + ", '"
                + status + "')";
        db.execSQL(query);
        db.close();
    }

    // Method to retrieve all records from the database
    public List<HistoryActivity.MeasurementRecord> getAllMeasurementRecords() {
        List<HistoryActivity.MeasurementRecord> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_HISTORY, null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") long timestamp = cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP));
                @SuppressLint("Range") int value = cursor.getInt(cursor.getColumnIndex(COLUMN_VALUE));
                @SuppressLint("Range") String status = cursor.getString(cursor.getColumnIndex(COLUMN_STATUS));
                historyList.add(new HistoryActivity.MeasurementRecord(new Date(timestamp), value, status));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return historyList;
    }
}
