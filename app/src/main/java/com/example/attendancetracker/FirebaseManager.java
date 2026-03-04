package com.example.attendancetracker;

import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles all cloud-based operations for the Attendance Tracker project.
 * This class handles the connection to Firebase Realtime Database.
 */
public class FirebaseManager {

    private DatabaseReference db;

    public FirebaseManager() {
        try {
            db = FirebaseDatabase.getInstance().getReference();
        } catch (Exception e) {
            Log.e("FirebaseInit", "Error: " + e.getMessage());
        }
    }

    /**
     * Registers a student identity in the Master List.
     */
    public void registerStudent(String macAddress, String nameAndEnroll, FirebaseCallback listener) {
        // Sanitize MAC for Firebase Key (No colons allowed)
        String sanitizedMac = macAddress.replace(":", "_");
        db.child("student_directory").child(sanitizedMac).setValue(nameAndEnroll)
            .addOnSuccessListener(aVoid -> listener.onSuccess("Student registered successfully"))
            .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    /**
     * Fetches the mapping of MAC Addresses to Student Details.
     */
    public void getStudentDirectory(StudentDirectoryCallback callback) {
        db.child("student_directory").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, String> directory = new HashMap<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    directory.put(ds.getKey(), ds.getValue(String.class));
                }
                callback.onDirectoryLoaded(directory);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseDir", "Failed to load directory: " + error.getMessage());
            }
        });
    }

    /**
     * Uploads the attendance list to Firebase.
     * @param subjectId The name of the subject.
     * @param detectedStudents Map where Key = MAC address, Value = Identity (Name/Enroll).
     */
    public void uploadAttendance(String subjectId, Map<String, String> detectedStudents, FirebaseCallback listener) {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String timeSlot = new SimpleDateFormat("hh_mm_a", Locale.getDefault()).format(new Date());
        String cleanSubject = subjectId.replaceAll("[.#$\\[\\]]", "_");

        DatabaseReference sessionRef = db.child("attendance")
                .child(cleanSubject)
                .child(date)
                .child(timeSlot);

        Map<String, Object> sessionData = new HashMap<>();

        for (Map.Entry<String, String> entry : detectedStudents.entrySet()) {
            String rawMac = entry.getKey();
            String sanitizedMac = rawMac.replace(":", "_");
            String identity = entry.getValue();

            Map<String, String> details = new HashMap<>();
            details.put("name", identity);
            details.put("status", "Present");
            details.put("timestamp", new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));

            sessionData.put(sanitizedMac, details);
        }

        sessionRef.setValue(sessionData)
            .addOnSuccessListener(aVoid -> listener.onSuccess("Attendance synchronized with cloud"))
            .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void fetchFullSubjectHistory(String subjectId, HistoryCallback callback) {
        String cleanSubject = subjectId.replaceAll("[.#$\\[\\]]", "_");
        db.child("attendance").child(cleanSubject).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.onHistoryLoaded(snapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseHistory", "Failed: " + error.getMessage());
            }
        });
    }

    public interface FirebaseCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface StudentDirectoryCallback {
        void onDirectoryLoaded(Map<String, String> directory);
    }

    public interface HistoryCallback {
        void onHistoryLoaded(DataSnapshot snapshot);
    }
}
