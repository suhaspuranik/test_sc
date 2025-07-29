package com.example.smart_cam;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AddReportingActivity extends BaseActivity {

    Button btn1stReporting, btn2ndReporting, btn3rdReporting, btnAddOther, btnSolar, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reporting);

        // Initialize UI elements
        btn1stReporting = findViewById(R.id.btn_1st_reporting);
        btn2ndReporting = findViewById(R.id.btn_2nd_reporting);
        btn3rdReporting = findViewById(R.id.btn_3rd_reporting);
        btnAddOther = findViewById(R.id.btn_add_other);
        btnSolar = findViewById(R.id.btn_solar);
        btnBack = findViewById(R.id.btn_back);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Handle Back button
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(AddReportingActivity.this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Set up button listeners
        btn1stReporting.setOnClickListener(v -> {
            Intent intent = new Intent(AddReportingActivity.this, ReportingOptionsActivity.class);
            intent.putExtra("REPORTING_TYPE", "1st Reporting");
            startActivity(intent);
        });

        btn2ndReporting.setOnClickListener(v -> {
            Intent intent = new Intent(AddReportingActivity.this, ReportingOptionsActivity.class);
            intent.putExtra("REPORTING_TYPE", "2nd Reporting");
            startActivity(intent);
        });

        btn3rdReporting.setOnClickListener(v -> {
            Intent intent = new Intent(AddReportingActivity.this, ReportingOptionsActivity.class);
            intent.putExtra("REPORTING_TYPE", "3rd Reporting");
            startActivity(intent);
        });

        btnAddOther.setOnClickListener(v -> {
            Intent intent = new Intent(AddReportingActivity.this, ReportingOptionsActivity.class);
            intent.putExtra("REPORTING_TYPE", "Other Reporting");
            startActivity(intent);
        });

        btnSolar.setOnClickListener(v -> {
            Intent intent = new Intent(AddReportingActivity.this, SolarFenceActivity.class);
            startActivity(intent);
        });

        // Set up BottomNavigationView listener
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Log.d("NAVIGATION", "Item clicked in AddReportingActivity: " + item.getItemId());
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Log.d("NAVIGATION", "Home selected in AddReportingActivity");
                Intent intent = new Intent(AddReportingActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_add) {
                Log.d("NAVIGATION", "Add selected in AddReportingActivity");
                return true;
            } else if (itemId == R.id.nav_menu) {
                Log.d("NAVIGATION", "Menu selected in AddReportingActivity");
                Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        // Highlight Add tab as selected
        bottomNavigationView.setSelectedItemId(R.id.nav_add);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AddReportingActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
        super.onBackPressed();
    }
}