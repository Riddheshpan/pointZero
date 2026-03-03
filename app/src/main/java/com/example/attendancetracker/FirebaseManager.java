package com.example.attendancetracker;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles all cloud-based operations for the BlueAttend project.
 * This class handles the connection to Firebase Realtime Database.
 */
public class FirebaseManager {

    private DatabaseReference db;

    public FirebaseManager() {
        // Initialize Firebase Database reference
        db = FirebaseDatabase.getInstance().getReference();
    }

    /**
     * Marks attendance for a specific subject and date in the Firebase cloud.
     * @param subjectId Name of the subject selected by the faculty.
     * @param detectedStudents Map of Student Name (Value) and MAC Address (Key).
     * @param listener Callback to notify the UI of success or failure.
     */
    public void uploadAttendance(String subjectId, Map<String, String> detectedStudents, FirebaseCallback listener) {
        // Format the current date (e.g., 2026-02-20)
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Format the current time slot (e.g., 10_30_AM)
        String timeSlot = new SimpleDateFormat("hh_mm_a", Locale.getDefault()).format(new Date());

        // Path in Firebase: /attendance/Subject_Name/Date/Time_Slot
        DatabaseReference sessionRef = db.child("attendance")
                .child(subjectId.replace(" ", "_"))
                .child(date)
                .child(timeSlot);

        Map<String, Object> sessionData = new HashMap<>();

        for (Map.Entry<String, String> entry : detectedStudents.entrySet()) {
            // Sanitize MAC address keys (Firebase does not allow ':' in keys)
            String sanitizedMac = entry.getKey().replace(":", "_");
            String studentName = entry.getValue();

            Map<String, String> details = new HashMap<>();
            details.put("name", studentName);
            details.put("status", "Present");
            details.put("timestamp", String.valueOf(System.currentTimeMillis()));

            sessionData.put(sanitizedMac, details);
        }

        // Upload the map of students to the database
        sessionRef.setValue(sessionData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                listener.onSuccess("Attendance successfully uploaded to Firebase.");
            } else {
                listener.onFailure(task.getException() != null ? task.getException().getMessage() : "Upload failed.");
            }
        });
    }

    public void fetchFullSubjectHistory(String subjectName, Object o) {
    }

    /**
     * Interface to communicate results back to the Activity (e.g., ScanActivity).
     */
    public interface FirebaseCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }
}