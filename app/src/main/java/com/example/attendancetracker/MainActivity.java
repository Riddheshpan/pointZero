package com.example.attendancetracker;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.attendancetracker.SubjectAdapter;
import com.example.attendancetracker.Subject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewSubjects);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Mock data for Timetable
        List<Subject> subjects = new ArrayList<>();
        subjects.add(new Subject("Mobile App Development", "08:30 AM - 10:30 AM", "Lab 4"));
        subjects.add(new Subject("Database Systems", "10:45 AM - 11:45 AM", "Room 302"));
        subjects.add(new Subject("Software Engineering", "11:45 AM - 12:45 PM", "Room 101"));

        SubjectAdapter adapter = new SubjectAdapter(subjects, subject -> {
            // When a subject is clicked, move to the Bluetooth Scan Screen
            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
            intent.putExtra("SUBJECT_NAME", subject.getName());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }
}