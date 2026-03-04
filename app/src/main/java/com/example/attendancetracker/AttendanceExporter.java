package com.example.attendancetracker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.google.firebase.database.DataSnapshot;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility to convert Firebase data into a CSV file (Excel compatible)
 * and share it with the faculty.
 */
public class AttendanceExporter {

    private static final String TAG = "AttendanceExporter";

    public static void exportToCSVAndShare(Context context, String subjectName, DataSnapshot historySnapshot) {
        Toast.makeText(context, "Preparing export...", Toast.LENGTH_SHORT).show();

        if (historySnapshot == null || !historySnapshot.hasChildren()) {
            Toast.makeText(context, "No attendance records found.", Toast.LENGTH_LONG).show();
            return;
        }

        StringBuilder csvData = new StringBuilder();
        // Header
        csvData.append("Date,Time Slot,Student Identity,MAC Address,Status\n");

        int recordCount = 0;

        try {
            for (DataSnapshot dateSnap : historySnapshot.getChildren()) {
                String date = dateSnap.getKey();

                for (DataSnapshot timeSnap : dateSnap.getChildren()) {
                    String timeSlot = timeSnap.getKey();

                    for (DataSnapshot studentSnap : timeSnap.getChildren()) {
                        // Restore colons to the MAC address (AA_BB -> AA:BB)
                        String rawMac = studentSnap.getKey();
                        String formattedMac = (rawMac != null) ? rawMac.replace("_", ":") : "Unknown";
                        
                        String name = studentSnap.child("name").getValue(String.class);
                        String status = studentSnap.child("status").getValue(String.class);

                        // If name is generic, use the MAC address as the identity
                        if (name == null || name.isEmpty() || name.equalsIgnoreCase("Unnamed Device")) {
                            name = formattedMac;
                        } else {
                            name = name.replace(",", " "); // Sanitize for CSV
                        }

                        if (status == null) status = "Present";

                        // Append row: Date, Time, Identity (Name/Enroll), MAC, Status
                        csvData.append(date).append(",")
                                .append(timeSlot).append(",")
                                .append("\"").append(name).append("\",")
                                .append(formattedMac).append(",")
                                .append(status).append("\n");
                        recordCount++;
                    }
                }
            }

            if (recordCount == 0) {
                Toast.makeText(context, "No records parsed.", Toast.LENGTH_SHORT).show();
                return;
            }

            String filename = subjectName.replace(" ", "_") + "_Attendance.csv";
            File file = new File(context.getCacheDir(), filename);
            FileOutputStream out = new FileOutputStream(file);
            out.write(csvData.toString().getBytes());
            out.close();

            shareFile(context, file);

        } catch (Exception e) {
            Log.e(TAG, "Export failed: " + e.getMessage());
            Toast.makeText(context, "Export error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void shareFile(Context context, File file) {
        try {
            String authority = context.getPackageName() + ".provider";
            Uri path = FileProvider.getUriForFile(context, authority, file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, path);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, "Share Report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(context, "Sharing failed.", Toast.LENGTH_SHORT).show();
        }
    }
}