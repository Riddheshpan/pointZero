package com.example.attendancetracker;

/**
 * Subject model to hold timetable information.
 */
public class Subject {
    private String name;
    private String time;
    private String classroom;

    public Subject(String name, String time, String classroom) {
        this.name = name;
        this.time = time;
        this.classroom = classroom;
    }

    public String getName() { return name; }
    public String getTime() { return time; }
    public String getClassroom() { return classroom; }
}