# PointZero: Faculty-Led Proximity Attendance System

PointZero is an Android-based attendance management solution designed to simplify the roll-call process through Bluetooth technology. Unlike traditional systems that require every student to install an app, PointZero utilizes a "Faculty-Centric" model where only the teacher needs the application.

## 🚀 Key Features

-Bluetooth Proximity Discovery: Leverages the ```BluetoothAdapter``` to scan and identify nearby student smartphones via their unique MAC addresses.

-Student Identity Mapping: Automatically maps discovered MAC addresses to actual Student Names and Enrollment numbers using a cloud-based directory.

-Futuristic UI/UX: A high-tech "Cyberpunk" aesthetic featuring Glassmorphism, neon accents, and a Dark Mode interface.

-Firebase Integration: Real-time synchronization of attendance logs to the cloud.

-Excel/CSV Export: Faculty can generate and share attendance reports in a spreadsheet format (CSV) with a single long-press on any subject.

-Dynamic Timetable: Pre-configured list of subjects and time slots for organized record-keeping.

## 🛠️ Tech Stack

-Language: Java (Android)

-Database: Firebase Realtime Database

-Architecture: Component-based (Adapters, Models, Helpers)

-UI: ConstraintLayout, Material Design 3, Glassmorphism

## 📦 Installation & Setup

### 1. Clone the Repository

```bash
git clone [https://github.com/Riddheshpan/pointZero.git](https://github.com/Riddheshpan/pointZero.git)
```


### 2. Firebase Configuration

Go to the Firebase Console.

Create a new project and add an Android App ```Package name: com.example.attendanceTracker```.

Download the ```google-services.json``` file and place it in the ```app/``` directory of your project.

Enable Realtime Database and set the rules to allow public read/write for testing:

```bash
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```


### 3. Setting up the Student Directory

To map MAC addresses to student names, you must create a ```student_directory``` node in your Realtime Database:

Path: ```student_directory/```

Key: MAC Address (replace : with _). ```Example: B4_C2_E5_A1_09_FF```

Value: ```Student Name (Enrollment Number)```

## 📱 Usage Guide

Dashboard: Select a subject from the futuristic list.

Scan: Click "Start Pulse" to begin discovering nearby student devices.

Identify: The app will automatically replace "Unknown Device" with the student's name if their MAC address is in the directory.

Submit: Select the students present and click "Submit Attendance."

Export: On the Dashboard, Long-Press a subject card to generate a CSV report and share it via Email/WhatsApp.

## 🔒 Security & Git

This project includes a ```.gitignore``` to protect sensitive files. Ensure that your ```app/google-services.json``` and ```local.properties``` are never pushed to public repositories.

If you accidentally pushed sensitive files, run the following in your terminal:

```bash
git rm -r --cached .
git add .
git commit -m "Cleanup sensitive files"
git push origin master
```

### Developed as a part of the Mobile App Development Project.
