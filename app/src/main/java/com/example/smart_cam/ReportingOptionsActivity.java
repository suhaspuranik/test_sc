package com.example.smart_cam;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ReportingOptionsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporting_options);

        // Get reporting type from Intent
        String reportingType = getIntent().getStringExtra("REPORTING_TYPE");

        // Initialize UI elements
        TextView tvTitle = findViewById(R.id.tv_title);
        RadioGroup rgOptions = findViewById(R.id.rg_options);
        RadioButton rbElephantMovement = findViewById(R.id.rb_elephant_movement);
        RadioButton rbNoElephantMovement = findViewById(R.id.rb_no_elephant_movement);
        Button btnSubmit = findViewById(R.id.btn_submit);
        Button btnBack = findViewById(R.id.btn_back);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set title
        tvTitle.setText(reportingType != null ? reportingType : "Reporting Options");

        // Handle Back button
        btnBack.setOnClickListener(v -> finish());

        // Handle Submit button
        btnSubmit.setOnClickListener(v -> {
            int selectedId = rgOptions.getCheckedRadioButtonId();
            if (selectedId == R.id.rb_elephant_movement) {
                Intent intent = new Intent(ReportingOptionsActivity.this, VillageSelectionActivity.class);
                intent.putExtra("REPORTING_TYPE", reportingType);
                startActivity(intent);
            } else if (selectedId == R.id.rb_no_elephant_movement) {
                // Custom Toast for No Elephant Movement
                Toast toast = new Toast(this);
                View toastView = getLayoutInflater().inflate(R.layout.custom_toast, null);
                TextView toastMessage = toastView.findViewById(R.id.toast_message);
                toastMessage.setTextColor(getResources().getColor(R.color.green));
                toastMessage.setText(reportingType + ": No Elephant Movement submitted");
                toast.setView(toastView);
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                // Redirect to AddReportingActivity
                Intent intent = new Intent(ReportingOptionsActivity.this, AddReportingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            } else {
                // Custom Toast for no selection
                Toast toast = new Toast(this);
                View toastView = getLayoutInflater().inflate(R.layout.custom_toast, null);
                TextView toastMessage = toastView.findViewById(R.id.toast_message);
                toastMessage.setTextColor(getResources().getColor(R.color.green));
                toastMessage.setText("Please select an option");
                toast.setView(toastView);
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });

        // Set up bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.nav_add);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_add) {
                Intent intent = new Intent(this, AddReportingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_menu) {
                // Placeholder for menu action
                Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }
}