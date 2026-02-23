package com.example.attendancetracker;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.attendancetracker.FirebaseManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Optimized ScanActivity to handle Firebase connectivity,
 * device name resolution, and scan stability.
 */
public class ScanActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> deviceList = new ArrayList<>();
    private Map<String, String> discoveredDevicesMap = new HashMap<>();
    private ArrayAdapter<String> adapter;
    private String subjectName;
    private ListView listView;
    private ProgressBar progressBar;
    private TextView statusTv;
    private boolean isReceiverRegistered = false;

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int REQUEST_ENABLE_BT = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity);

        subjectName = getIntent().getStringExtra("SUBJECT_NAME");
        if (subjectName == null || subjectName.isEmpty()) subjectName = "Unspecified_Subject";

        TextView titleTv = findViewById(R.id.textViewSubjectTitle);
        titleTv.setText("Lecture: " + subjectName);

        statusTv = findViewById(R.id.textViewStatus); // Ensure this ID exists in your XML
        progressBar = findViewById(R.id.progressBarScan); // Ensure this ID exists in your XML

        listView = findViewById(R.id.listViewDevices);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, deviceList);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Button btnScan = findViewById(R.id.btnStartScan);
        Button btnSelectAll = findViewById(R.id.btnSelectAll);
        Button btnSubmit = findViewById(R.id.btnSubmitAttendance);

        btnScan.setOnClickListener(v -> checkPermissionsAndScan());

        btnSelectAll.setOnClickListener(v -> {
            for (int i = 0; i < listView.getCount(); i++) {
                listView.setItemChecked(i, true);
            }
        });

        btnSubmit.setOnClickListener(v -> {
            HashMap<String, String> selectedStudents = new HashMap<>();
            for (int i = 0; i < listView.getCount(); i++) {
                if (listView.isItemChecked(i)) {
                    String info = deviceList.get(i);
                    String[] parts = info.split("\n");
                    if (parts.length >= 2) {
                        selectedStudents.put(parts[1], parts[0]);
                    }
                }
            }

            if (selectedStudents.isEmpty()) {
                Toast.makeText(this, "Please select at least one student", Toast.LENGTH_SHORT).show();
                return;
            }

            // Problem 1 Fix: Detailed callback for Firebase troubleshooting
            FirebaseManager fbManager = new FirebaseManager();
            fbManager.uploadAttendance(subjectName, selectedStudents, new FirebaseManager.FirebaseCallback() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(ScanActivity.this, "Success: Data Saved to Cloud", Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    // Log the actual Firebase error for debugging
                    Log.e("FirebaseError", error);
                    Toast.makeText(ScanActivity.this, "Firebase Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void checkPermissionsAndScan() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Hardware doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                requestBluetoothPermissions();
            }
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please turn on GPS/Location to discover devices", Toast.LENGTH_LONG).show();
            return;
        }

        requestBluetoothPermissions();
    }

    private void requestBluetoothPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        ArrayList<String> needed = new ArrayList<>();
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            startDiscovery();
        }
    }

    private void startDiscovery() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return;
            }

            deviceList.clear();
            discoveredDevicesMap.clear();
            adapter.notifyDataSetChanged();

            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }

            if (!isReceiverRegistered) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED); // Listen for name resolution
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                registerReceiver(receiver, filter);
                isReceiverRegistered = true;
            }

            bluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            Log.e("ScanActivity", "Permission mismatch: " + e.getMessage());
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                if (statusTv != null) statusTv.setText("Scanning for students...");
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (statusTv != null) statusTv.setText("Scan Complete. Select students below.");
            }
            // Problem 2 & 3 Fix: Handle Found and Name Changed events
            else if (BluetoothDevice.ACTION_FOUND.equals(action) || BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }

                        String name = device.getName();
                        String address = device.getAddress();

                        // If name is still null, we check if we already have it
                        if (name == null || name.isEmpty()) {
                            name = discoveredDevicesMap.containsKey(address) ? discoveredDevicesMap.get(address) : "Unnamed Device";
                        }

                        // Update Logic: If it's a new device or an update to an "Unnamed" one
                        if (!discoveredDevicesMap.containsKey(address) || (discoveredDevicesMap.get(address).equals("Unnamed Device") && !name.equals("Unnamed Device"))) {
                            discoveredDevicesMap.put(address, name);

                            // Remove old "Unnamed" entry if it exists to prevent duplicates
                            for (int i = 0; i < deviceList.size(); i++) {
                                if (deviceList.get(i).contains(address)) {
                                    deviceList.remove(i);
                                    break;
                                }
                            }

                            deviceList.add(name + "\n" + address);
                            adapter.notifyDataSetChanged();
                        }
                    } catch (SecurityException ignored) {}
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered) {
            unregisterReceiver(receiver);
            isReceiverRegistered = false;
        }
    }
}