package com.example.attendancetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity serves as the Faculty Dashboard.
 * It displays the timetable and provides options to start attendance scans
 * or export historical data to Excel/CSV.
 */
public class MainActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private RecyclerView recyclerView;
    private SubjectAdapter adapter;
    private List<Subject> subjectList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Manager
        firebaseManager = new FirebaseManager();

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerViewSubjects);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Prepare the timetable data
        prepareSubjectList();

        // Initialize adapter with listeners
        // Single click: Start Attendance Scan
        // Long click: Export Attendance History to Excel (CSV)
        adapter = new SubjectAdapter(subjectList, new SubjectAdapter.OnSubjectClickListener() {
            @Override
            public void onSubjectClick(Subject subject) {
                // Navigate to ScanActivity for the selected subject
                Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                intent.putExtra("SUBJECT_NAME", subject.getName());
                startActivity(intent);
            }

            @Override
            public void onSubjectLongClick(Subject subject) {
                // Trigger Excel export on long press
                performExport(subject.getName());
            }
        });

        recyclerView.setAdapter(adapter);
    }

    /**
     * Helper method to trigger the Excel/CSV export for a specific subject.
     */
    public void performExport(String subjectName) {
        Toast.makeText(this, "Fetching data for " + subjectName, Toast.LENGTH_SHORT).show();

        firebaseManager.fetchFullSubjectHistory(subjectName, data -> {
            if (data != null && data.exists()) {
                // Call the AttendanceExporter utility
                AttendanceExporter.exportToCSVAndShare(MainActivity.this, subjectName, data);
            } else {
                Toast.makeText(MainActivity.this, "No history found for this subject.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void prepareSubjectList() {
        subjectList = new ArrayList<>();
        subjectList.add(new Subject("Mobile App Development", "09:00 AM - 10:00 AM", "Lab 4"));
        subjectList.add(new Subject("Database Systems", "10:15 AM - 11:15 AM", "Room 302"));
        subjectList.add(new Subject("Software Engineering", "11:30 AM - 12:30 PM", "Seminar Hall"));
        subjectList.add(new Subject("Computer Networks", "01:45 PM - 02:45 PM", "Room 105"));
    }
}