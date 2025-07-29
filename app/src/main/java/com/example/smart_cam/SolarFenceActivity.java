package com.example.smart_cam;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SolarFenceActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solar_fence);

        // Initialize UI elements
        EditText etSolarVolts = findViewById(R.id.et_solar_volts);
        Button btnSubmit = findViewById(R.id.btn_submit);
        Button btnBack = findViewById(R.id.btn_back);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Handle Back button
        btnBack.setOnClickListener(v -> finish());

        // Handle Submit button
        btnSubmit.setOnClickListener(v -> {
            String input = etSolarVolts.getText().toString().trim();
            // Create custom Toast
            Toast toast = new Toast(this);
            View toastView = getLayoutInflater().inflate(R.layout.custom_toast, null);
            TextView toastMessage = toastView.findViewById(R.id.toast_message);
            toastMessage.setTextColor(getResources().getColor(R.color.green));
            toastMessage.setText(input.isEmpty() ? "Please enter a value" : "Submitted Successfully");
            toast.setView(toastView);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            // Redirect to AddReportingActivity on successful submit
            if (!input.isEmpty()) {
                Intent intent = new Intent(SolarFenceActivity.this, AddReportingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
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