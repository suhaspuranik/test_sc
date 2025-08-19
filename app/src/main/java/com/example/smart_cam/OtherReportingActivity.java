package com.example.smart_cam;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.smart_cam.network.VolleySingleton;
import com.example.smart_cam.utils.AppConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OtherReportingActivity extends BaseActivity {
    private static final String TAG = "OtherReportingActivity";
    private static final int OTHER_REPORT_TYPE_ID = 4;
    private EditText etDescription;
    private Button btnSubmit, btnBack;
    private BottomNavigationView bottomNavigationView;
    private String username;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_reporting);

        etDescription = findViewById(R.id.et_description);
        btnSubmit = findViewById(R.id.btn_submit);
        btnBack = findViewById(R.id.btn_back);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        Intent incomingIntent = getIntent();
        if (incomingIntent != null) {
            username = incomingIntent.getStringExtra("username");
            userId = incomingIntent.getIntExtra("user_id", -1);
        }

        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            String description = etDescription.getText().toString().trim();
            if (description.isEmpty()) {
                showToast("Please enter a description");
                return;
            }
            // Navigate to ReportVillageActivity to select villages, with report type 4
            Intent intent = new Intent(OtherReportingActivity.this, ReportVillageActivity.class);
            intent.putExtra("REPORTING_TYPE", "Other Report");
            intent.putExtra("REPORT_TYPE_ID", OTHER_REPORT_TYPE_ID);
            intent.putExtra("REPORT_DESCRIPTION", description);
            intent.putExtra("username", username);
            intent.putExtra("user_id", userId);
            startActivity(intent);
            finish();
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_add);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent dashIntent = new Intent(this, DashboardActivity.class);
                dashIntent.putExtra("username", username);
                dashIntent.putExtra("user_id", userId);
                startActivity(dashIntent);
                finish();
                return true;
            } else if (itemId == R.id.nav_add) {
                Intent addIntent = new Intent(this, AddReportingActivity.class);
                addIntent.putExtra("username", username);
                addIntent.putExtra("user_id", userId);
                addIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(addIntent);
                finish();
                return true;
            } else if (itemId == R.id.nav_menu) {
                Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
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


