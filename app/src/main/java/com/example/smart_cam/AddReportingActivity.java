package com.example.smart_cam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
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
import java.util.List;
import java.util.Map;

public class AddReportingActivity extends BaseActivity {
    private static final String TAG = "AddReportingActivity";
    private LinearLayout buttonContainer;
    private Button btnSolar, btnBack, btnFirst, btnSecond, btnThird, btnOther;
    private ArrayList<ReportType> reportTypes;
    private String username;
    private int userId;
    private boolean isSessionValid = false;

    private class ReportType {
        int id;
        String name;

        ReportType(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reporting);

        // Prefer SharedPreferences for session, fall back to Intent extras
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String storedUsername = prefs.getString("username", null);
        int storedUserId = prefs.getInt("user_id", -1);

        String intentUsername = getIntent().getStringExtra("username");
        int intentUserId = getIntent().getIntExtra("user_id", -1);

        // Use stored values if present, otherwise use intent values
        username = (storedUsername != null && !storedUsername.isEmpty()) ? storedUsername : intentUsername;
        userId = (storedUserId != -1) ? storedUserId : intentUserId;

        Log.d(TAG, "Resolved session - username: '" + (username != null ? username : "null") + "', user_id: " + userId);

        // Validate final resolved session
        if (username == null || username.isEmpty() || userId == -1) {
            Log.e(TAG, "No valid session available; redirecting to login");
            handleSessionExpiry();
            return;
        }

        isSessionValid = true;

        // Log SharedPreferences contents
        SharedPreferences prefsSession = getSharedPreferences("user_session", MODE_PRIVATE);
        Log.d(TAG, "user_session contents: " + prefsSession.getAll().toString());

        // Initialize UI elements
        buttonContainer = findViewById(R.id.button_container);
        btnSolar = findViewById(R.id.btn_solar);
        btnBack = findViewById(R.id.btn_back);
        btnFirst = findViewById(R.id.btn_1st_reporting);
        btnSecond = findViewById(R.id.btn_2nd_reporting);
        btnThird = findViewById(R.id.btn_3rd_reporting);
        btnOther = findViewById(R.id.btn_add_other);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Disable buttons during API call
        btnSolar.setEnabled(false);
        btnBack.setEnabled(false);
        if (btnFirst != null)
            btnFirst.setEnabled(false);
        if (btnSecond != null)
            btnSecond.setEnabled(false);
        if (btnThird != null)
            btnThird.setEnabled(false);
        if (btnOther != null)
            btnOther.setEnabled(false);

        // Validate session and fetch report types
        validateSessionAndFetch();

        // Handle Back button
        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            Intent intent = new Intent(AddReportingActivity.this, DashboardActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("user_id", userId);
            startActivity(intent);
            finish();
        });

        // Defer setting button click listeners until report types are fetched

        // Set up BottomNavigationView listener
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Log.d(TAG, "Navigation item clicked: " + itemId);
            if (itemId == R.id.nav_home) {
                Log.d(TAG, "Home selected");
                Intent intent = new Intent(AddReportingActivity.this, DashboardActivity.class);
                intent.putExtra("username", username);
                intent.putExtra("user_id", userId);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_add) {
                Log.d(TAG, "Add selected");
                return true;
            } else if (itemId == R.id.nav_menu) {
                Log.d(TAG, "Menu selected");
                Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_add);
    }

    private void validateSessionAndFetch() {
        // Session is already validated in onCreate, just fetch report types
        fetchReportTypes();
    }

    private void fetchReportTypes() {
        Log.d(TAG, "Fetching report types for username: " + username + ", user_id: " + userId);

        // Build request body as backend expects POST with JSON
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("user_id", userId);
            jsonBody.put("stage", "dev");
        } catch (Exception e) {
            Log.e(TAG, "Error building request body: " + e.getMessage(), e);
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                AppConfig.GET_REPORT_TYPE_URL,
                jsonBody,
                response -> {
                    runOnUiThread(() -> {
                        btnSolar.setEnabled(true);
                        btnBack.setEnabled(true);
                        if (btnFirst != null)
                            btnFirst.setEnabled(true);
                        if (btnSecond != null)
                            btnSecond.setEnabled(true);
                        if (btnThird != null)
                            btnThird.setEnabled(true);
                        if (btnOther != null)
                            btnOther.setEnabled(true);
                    });
                    try {
                        Log.d(TAG, "Full response: " + response.toString());
                        // Support multiple response shapes:
                        // 1) { "RESULT": [ {report_type_id, report_type_name}, ... ] }
                        // 2) { "p_out_mssg_flg": "S", "report_types": [ ... ] }
                        JSONArray reportTypesArray = null;

                        if (response.has("report_types")) {
                            reportTypesArray = response.getJSONArray("report_types");
                        } else if (response.has("RESULT")) {
                            Object resultNode = response.get("RESULT");
                            if (resultNode instanceof JSONArray) {
                                reportTypesArray = (JSONArray) resultNode;
                            } else if (resultNode instanceof JSONObject) {
                                JSONObject resultObj = (JSONObject) resultNode;
                                if (resultObj.has("report_types")) {
                                    reportTypesArray = resultObj.getJSONArray("report_types");
                                }
                            }
                        } else if ("S".equals(response.optString("p_out_mssg_flg", ""))) {
                            reportTypesArray = response.optJSONArray("report_types");
                        }

                        if (reportTypesArray == null) {
                            String message = response.optString("p_out_mssg", "No report types in response");
                            Log.e(TAG, "No report types array found in response");
                            runOnUiThread(() -> Toast
                                    .makeText(this, "Failed to fetch report types: " + message, Toast.LENGTH_LONG)
                                    .show());
                            return;
                        }

                        reportTypes = new ArrayList<>();
                        for (int i = 0; i < reportTypesArray.length(); i++) {
                            JSONObject reportType = reportTypesArray.getJSONObject(i);
                            int id = reportType.getInt("report_type_id");
                            String name = reportType.getString("report_type_name");
                            reportTypes.add(new ReportType(id, name));
                            Log.d(TAG, "Report type added: ID=" + id + ", Name=" + name);
                        }
                        runOnUiThread(() -> {
                            populateButtons();
                            wireStaticButtons();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
                        runOnUiThread(() -> Toast
                                .makeText(this, "Error parsing report types: " + e.getMessage(), Toast.LENGTH_LONG)
                                .show());
                    }
                },
                error -> {
                    runOnUiThread(() -> {
                        btnSolar.setEnabled(true);
                        btnBack.setEnabled(true);
                    });
                    String errorMsg = "Failed to fetch report types";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            errorMsg = new String(error.networkResponse.data, "UTF-8");
                            Log.e(TAG, "fetchReportTypes error response: " + errorMsg + ", status code: "
                                    + error.networkResponse.statusCode);
                            if (error.networkResponse.statusCode == 401 || error.networkResponse.statusCode == 403) {
                                Log.e(TAG, "Authentication error: " + errorMsg);
                                runOnUiThread(() -> handleSessionExpiry());
                                return;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error decoding network response: " + e.getMessage(), e);
                        }
                    } else if (error.getMessage() != null) {
                        errorMsg = error.getMessage();
                        Log.e(TAG, "fetchReportTypes error: " + errorMsg);
                    }

                    String finalErrorMsg = errorMsg;
                    runOnUiThread(() -> Toast
                            .makeText(this, "Failed to fetch report types: " + finalErrorMsg, Toast.LENGTH_LONG)
                            .show());
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("x-api-key", AppConfig.API_KEY);
                Log.d(TAG, "Request headers: " + headers.toString());
                return headers;
            }
        };

        // Set retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Log.d(TAG, "Fetching report types from: " + AppConfig.GET_REPORT_TYPE_URL);
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void handleSessionExpiry() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear().apply();
        Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void populateButtons() {
        buttonContainer.removeAllViews();
        if (reportTypes == null || reportTypes.isEmpty()) {
            Log.w(TAG, "No report types to populate");
            runOnUiThread(() -> Toast.makeText(this, "No report types available", Toast.LENGTH_LONG).show());
            return;
        }

        for (ReportType reportType : reportTypes) {
            Button button = new Button(this);
            button.setText(reportType.name);
            button.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            button.setOnClickListener(v -> {
                Log.d(TAG, "Report type button clicked: ID=" + reportType.id + ", Name=" + reportType.name);
                Intent intent;
                if (reportType.id == 4 || reportType.id == 5) {
                    intent = new Intent(AddReportingActivity.this, SolarFenceActivity.class);
                    intent.putExtra("REPORTING_TYPE", reportType.name);
                    intent.putExtra("REPORT_TYPE_ID", reportType.id);
                    intent.putExtra("username", username);
                    intent.putExtra("user_id", userId);
                } else {
                    intent = new Intent(AddReportingActivity.this, ReportingOptionsActivity.class);
                    intent.putExtra("REPORTING_TYPE", reportType.name);
                    intent.putExtra("REPORT_TYPE_ID", reportType.id);
                    intent.putExtra("username", username);
                    intent.putExtra("user_id", userId);
                }
                try {
                    startActivity(intent);
                    Log.d(TAG, "Navigating to " + (reportType.id == 4 || reportType.id == 5 ? "SolarFenceActivity"
                            : "ReportingOptionsActivity") + " with report_type_id=" + reportType.id);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting activity: " + e.getMessage(), e);
                    Toast.makeText(this, "Error navigating: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            buttonContainer.addView(button);
            Log.d(TAG, "Button added for report type: " + reportType.name);
        }
    }

    private void wireStaticButtons() {
        if (btnFirst != null) {
            btnFirst.setOnClickListener(v -> navigateByName("First Report", 1));
        }
        if (btnSecond != null) {
            btnSecond.setOnClickListener(v -> navigateByName("Second Report", 2));
        }
        if (btnThird != null) {
            btnThird.setOnClickListener(v -> navigateByName("Third Report", 3));
        }
        if (btnOther != null) {
            btnOther.setOnClickListener(v -> navigateByName("Other Report", 4));
        }
        if (btnSolar != null) {
            btnSolar.setOnClickListener(v -> navigateByName("Solar Fence Report", 5));
        }
    }

    private void navigateByName(String defaultName, int reportTypeId) {
        // Use the reportTypes fetched to resolve correct name if present
        String resolvedName = defaultName;
        if (reportTypes != null) {
            for (ReportType type : reportTypes) {
                if (type.id == reportTypeId) {
                    resolvedName = type.name;
                    break;
                }
            }
        }
        Intent intent;
        if (reportTypeId == 4 || reportTypeId == 5) {
            intent = new Intent(AddReportingActivity.this, SolarFenceActivity.class);
            intent.putExtra("REPORTING_TYPE", resolvedName);
            intent.putExtra("REPORT_TYPE_ID", reportTypeId);
            intent.putExtra("username", username);
            intent.putExtra("user_id", userId);
        } else {
            intent = new Intent(AddReportingActivity.this, ReportingOptionsActivity.class);
            intent.putExtra("REPORTING_TYPE", resolvedName);
            intent.putExtra("REPORT_TYPE_ID", reportTypeId);
            intent.putExtra("username", username);
            intent.putExtra("user_id", userId);
        }
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back pressed");
        Intent intent = new Intent(AddReportingActivity.this, DashboardActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("user_id", userId);
        startActivity(intent);
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VolleySingleton.getInstance(this).getRequestQueue().cancelAll(TAG);
    }
}