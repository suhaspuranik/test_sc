package com.example.smart_cam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ReportingOptionsActivity extends BaseActivity {
    private static final String TAG = "ReportOptionsActivity";
    private String reportingType;
    private int reportTypeId;
    private String username;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporting_options);

        // Get extras
        reportingType = getIntent().getStringExtra("REPORTING_TYPE");
        reportTypeId = getIntent().getIntExtra("REPORT_TYPE_ID", -1);
        username = getIntent().getStringExtra("username");
        userId = getIntent().getIntExtra("user_id", -1);

        // Log SharedPreferences contents
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        Log.d(TAG, "user_session contents: " + prefs.getAll().toString());
        Log.d(TAG, "Received username: " + (username != null ? username : "null"));
        Log.d(TAG, "Received user_id: " + userId);

        // Validate extras
        if (reportTypeId == -1 || username == null || username.isEmpty() || userId == -1) {
            Log.e(TAG, "Invalid report_type_id, username, or user_id");
            showToast("Invalid session or report type");
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize UI elements
        TextView tvTitle = findViewById(R.id.tv_title);
        Button btnElephantMovement = findViewById(R.id.rb_elephant_movement);
        Button btnNoElephantMovement = findViewById(R.id.rb_no_elephant_movement);
        Button btnBack = findViewById(R.id.btn_back);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set title
        tvTitle.setText(reportingType != null ? reportingType + ": Select Option" : "Select Option");
        Log.d(TAG, "Initialized with report_type_id=" + reportTypeId + ", reporting_type=" + reportingType);

        // Handle Elephant Movement button
        btnElephantMovement.setOnClickListener(v -> {
            Log.d(TAG, "Elephant Movement clicked for report_type_id=" + reportTypeId);
            Intent intent = new Intent(ReportingOptionsActivity.this, VillageSelectionActivity.class);
            intent.putExtra("REPORTING_TYPE", reportingType);
            intent.putExtra("REPORT_TYPE_ID", reportTypeId);
            intent.putExtra("username", username);
            intent.putExtra("user_id", userId);
            try {
                startActivity(intent);
                Log.d(TAG, "Navigating to VillageSelectionActivity");
            } catch (Exception e) {
                Log.e(TAG, "Error starting VillageSelectionActivity: " + e.getMessage(), e);
                showToast("Error navigating: " + e.getMessage());
            }
        });

        // Handle No Elephant Movement button
        btnNoElephantMovement.setOnClickListener(v -> {
            Log.d(TAG, "No Elephant Movement clicked for report_type_id=" + reportTypeId);
            submitReportNoElephant();
        });

        // Handle Back button
        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            finish();
        });

        // Set up bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.nav_add);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Log.d(TAG, "Navigation item clicked: " + itemId);
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.putExtra("username", username);
                intent.putExtra("user_id", userId);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_add) {
                Intent intent = new Intent(this, AddReportingActivity.class);
                intent.putExtra("username", username);
                intent.putExtra("user_id", userId);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_menu) {
                showToast("Menu clicked");
                return true;
            }
            return false;
        });
    }

    private void submitReportNoElephant() {
        Log.d(TAG, "No Elephant Movement clicked - navigating to ReportVillageActivity");

        // Navigate to ReportVillageActivity for village selection
        Intent intent = new Intent(ReportingOptionsActivity.this, ReportVillageActivity.class);
        intent.putExtra("REPORTING_TYPE", reportingType);
        intent.putExtra("REPORT_TYPE_ID", reportTypeId);
        intent.putExtra("username", username);
        intent.putExtra("user_id", userId);
        intent.putExtra("NO_ELEPHANT_FOUND", true); // Flag to indicate no elephant found scenario
        try {
            startActivity(intent);
            Log.d(TAG, "Navigating to ReportVillageActivity for village selection");
        } catch (Exception e) {
            Log.e(TAG, "Error starting ReportVillageActivity: " + e.getMessage(), e);
            showToast("Error navigating: " + e.getMessage());
        }
    }



    private void showToast(String message) {
        Toast toast = new Toast(this);
        View toastView = getLayoutInflater().inflate(R.layout.custom_toast, null);
        TextView toastMessage = toastView.findViewById(R.id.toast_message);
        toastMessage.setTextColor(getResources().getColor(R.color.green));
        toastMessage.setText(message);
        toast.setView(toastView);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }


}