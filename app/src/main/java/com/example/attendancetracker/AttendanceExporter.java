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
        // Step 0: Inform user the process has started
        Toast.makeText(context, "Preparing export...", Toast.LENGTH_SHORT).show();

        // 1. Check if we actually have data to export
        if (historySnapshot == null || !historySnapshot.hasChildren()) {
            Log.e(TAG, "History snapshot is empty or null");
            Toast.makeText(context, "No attendance records found for this subject.", Toast.LENGTH_LONG).show();
            return;
        }

        StringBuilder csvData = new StringBuilder();

        // CSV Header
        csvData.append("Date,Time Slot,Student Name,MAC Address,Status\n");

        int recordCount = 0;

        try {
            // Iterate through Dates (e.g., 2026-02-20)
            for (DataSnapshot dateSnap : historySnapshot.getChildren()) {
                String date = dateSnap.getKey();

                // Iterate through Time Slots (e.g., 10_30_AM)
                for (DataSnapshot timeSnap : dateSnap.getChildren()) {
                    String timeSlot = timeSnap.getKey();

                    // Iterate through Student Records (MAC Addresses)
                    for (DataSnapshot studentSnap : timeSnap.getChildren()) {
                        String mac = studentSnap.getKey();
                        String name = studentSnap.child("name").getValue(String.class);
                        String status = studentSnap.child("status").getValue(String.class);

                        // Sanitize to prevent CSV formatting issues
                        if (name == null) name = "Unknown";
                        else name = name.replace(",", " ");

                        if (status == null) status = "Present";

                        // Add Row: Date, Time, Name, MAC, Status
                        csvData.append(date).append(",")
                                .append(timeSlot).append(",")
                                .append("\"").append(name).append("\",")
                                .append(mac != null ? mac : "Unknown_MAC").append(",")
                                .append(status).append("\n");
                        recordCount++;
                    }
                }
            }

            if (recordCount == 0) {
                Log.w(TAG, "Snapshot had structure but 0 records were parsed.");
                Toast.makeText(context, "No records were found in the selected history.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save file to internal cache
            String sanitizedSubject = (subjectName != null ? subjectName : "Subject").replace(" ", "_");
            String filename = sanitizedSubject + "_Attendance.csv";
            File file = new File(context.getCacheDir(), filename);

            FileOutputStream out = new FileOutputStream(file);
            out.write(csvData.toString().getBytes());
            out.close();

            Log.d(TAG, "File created: " + file.getAbsolutePath() + " with " + recordCount + " records.");

            // Share the file
            shareFile(context, file);

        } catch (IOException e) {
            Log.e(TAG, "File creation failed: " + e.getMessage());
            Toast.makeText(context, "Error creating file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage());
            Toast.makeText(context, "An error occurred during export.", Toast.LENGTH_SHORT).show();
        }
    }

    private static void shareFile(Context context, File file) {
        if (!file.exists()) {
            Toast.makeText(context, "Generated file not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Authority MUST match AndroidManifest.xml exactly: <application_id>.provider
            String authority = context.getPackageName() + ".provider";
            Uri path = FileProvider.getUriForFile(context, authority, file);

            Log.d(TAG, "Sharing file via authority: " + authority);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Attendance Report");
            intent.putExtra(Intent.EXTRA_STREAM, path);

            // Permissions for the receiving app to read the file
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // If calling from a context that isn't an Activity, this flag is required
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Create the chooser
            Intent chooser = Intent.createChooser(intent, "Share Attendance Report");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(chooser);
            Log.d(TAG, "Share chooser intent started.");

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "FileProvider error: " + e.getMessage());
            Toast.makeText(context, "Configuration error. Check Manifest for FileProvider.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Sharing failed: " + e.getMessage());
            Toast.makeText(context, "Could not open share menu.", Toast.LENGTH_SHORT).show();
        }
    }
}