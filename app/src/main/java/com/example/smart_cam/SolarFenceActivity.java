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

public class SolarFenceActivity extends BaseActivity {
    private static final String TAG = "SolarFenceActivity";
    private static final int SOLAR_FENCE_REPORT_TYPE_ID = 5;
    private EditText etVoltage;
    private Button btnSubmit;
    private Button btnBack;
    private BottomNavigationView bottomNavigationView;
    private String username;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solar_fence);

        // Initialize UI elements
        etVoltage = findViewById(R.id.et_solar_volts);
        btnSubmit = findViewById(R.id.btn_submit);
        btnBack = findViewById(R.id.btn_back);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Get username and user_id from intent
        Intent incomingIntent = getIntent();
        if (incomingIntent != null) {
            username = incomingIntent.getStringExtra("username");
            userId = incomingIntent.getIntExtra("user_id", -1);
        }

        // Handle Back button
        btnBack.setOnClickListener(v -> finish());

        // Handle Submit button
        btnSubmit.setOnClickListener(v -> {
            String input = etVoltage.getText().toString().trim();
            if (input.isEmpty()) {
                showToast("Please enter solar fence details");
                return;
            }
            // Accept free text per requirement
            submitSolarFenceReport(input);
        });

        // Set up bottom navigation
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

    private void submitSolarFenceReport(String voltage) {
        // Navigate to ReportVillageActivity to select villages first; do not submit report here
        Log.d(TAG, "Proceeding to village selection for Solar Fence without submitting report");
        Intent intent = new Intent(SolarFenceActivity.this, ReportVillageActivity.class);
        intent.putExtra("REPORTING_TYPE", "Solar Fence");
        intent.putExtra("REPORT_TYPE_ID", SOLAR_FENCE_REPORT_TYPE_ID);
        intent.putExtra("REPORT_DESCRIPTION", "Solar fence voltage: " + voltage + " volts");
        intent.putExtra("username", username);
        intent.putExtra("user_id", userId);
        startActivity(intent);
        finish();
    }

    // TODO: Village mapping API is currently returning 403 Forbidden
    // This method is kept for future use when the API authentication is fixed
    private void fetchUserMappedVillages(String voltage) {
        Log.d(TAG, "Fetching user mapped villages for user_id: " + userId);

        // Prepare JSON input for village mapping
        JSONObject jsonInput = new JSONObject();
        try {
            jsonInput.put("user_id", userId);
            jsonInput.put("stage", "dev");
        } catch (Exception e) {
            Log.e(TAG, "Error building village request body: " + e.getMessage(), e);
            showToast("Error preparing request");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                AppConfig.USER_MAPPED_VILLAGE,
                jsonInput,
                response -> {
                    Log.d(TAG, "Village mapping response: " + response.toString());
                    try {
                        String flag = response.optString("p_out_mssg_flg", "");

                        if ("S".equals(flag) || response.has("RESULT")) {
                            // Extract village IDs from RESULT array
                            JSONArray villagesArray = response.optJSONArray("RESULT");
                            if (villagesArray != null && villagesArray.length() > 0) {
                                ArrayList<Integer> villageIds = new ArrayList<>();
                                for (int i = 0; i < villagesArray.length(); i++) {
                                    JSONObject village = villagesArray.getJSONObject(i);
                                    int villageId = village.getInt("village_id");
                                    String villageName = village.optString("village_name", "Unknown");
                                    villageIds.add(villageId);
                                    Log.d(TAG, "Found village: ID=" + villageId + ", Name=" + villageName);
                                }
                                Log.d(TAG, "Total villages found: " + villageIds.size());
                                // Submit report with village IDs
                                submitReportWithVillages(voltage, villageIds);
                            } else {
                                Log.w(TAG, "No villages in RESULT array");
                                showToast("No villages mapped to user. Submitting report without village mapping.");
                                submitReportWithoutVillages(voltage);
                            }
                        } else {
                            String message = response.optString("p_out_mssg", "Failed to fetch villages");
                            Log.e(TAG, "Village mapping failed: " + message);
                            showToast("Warning: " + message + ". Submitting report without village mapping.");
                            // Fallback: submit report without villages
                            submitReportWithoutVillages(voltage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing village response: " + e.getMessage(), e);
                        showToast("Warning: Error parsing villages. Submitting report without village mapping.");
                        submitReportWithoutVillages(voltage);
                    }
                },
                error -> {
                    Log.e(TAG, "Village mapping Volley error: " + error.getMessage(), error);
                    String errorMsg = "Failed to fetch villages";

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = new String(error.networkResponse.data);
                        Log.e(TAG, "Village mapping HTTP error " + statusCode + ": " + responseData);

                        if (statusCode == 401 || statusCode == 403) {
                            showToast("Authentication error: Please log in again");
                            return;
                        }
                        errorMsg = "HTTP " + statusCode + ": " + responseData;
                    }

                    showToast("Warning: " + errorMsg + ". Submitting report without village mapping.");
                    // Fallback: submit report without villages
                    submitReportWithoutVillages(voltage);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("x-api-key", AppConfig.API_KEY);
                Log.d(TAG, "Village mapping request headers: " + headers.toString());
                return headers;
            }
        };

        // Set retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                15000, // 15 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Fetching villages from: " + AppConfig.USER_MAPPED_VILLAGE);
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void submitReportWithVillages(String voltage, ArrayList<Integer> villageIds) {
        Log.d(TAG, "Submitting solar fence report with " + villageIds.size() + " villages");

        // Prepare JSON input with village IDs
        JSONObject jsonInput = new JSONObject();
        try {
            jsonInput.put("user_id", userId);
            jsonInput.put("report_type_id", SOLAR_FENCE_REPORT_TYPE_ID);
            jsonInput.put("description", "Solar fence voltage: " + voltage + " volts");
            jsonInput.put("stage", "dev");

            // Add village IDs array
            JSONArray villageIdsArray = new JSONArray(villageIds);
            jsonInput.put("village_ids", villageIdsArray);
        } catch (Exception e) {
            Log.e(TAG, "Error building report request body: " + e.getMessage(), e);
            showToast("Error preparing report request");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                AppConfig.REPORT_TYPE,
                jsonInput,
                response -> {
                    Log.d(TAG, "Report submission response: " + response.toString());
                    try {
                        // Handle both response structures:
                        // 1) Direct: {"p_out_mssg_flg": "S", "p_out_mssg": "message"}
                        // 2) Nested: {"RESULT": [{"p_out_mssg_flg": "S", "p_out_mssg": "message"}]}
                        String flag = "";
                        String message = "";

                        if (response.has("RESULT")) {
                            // Nested structure
                            JSONArray resultArray = response.getJSONArray("RESULT");
                            if (resultArray.length() > 0) {
                                JSONObject resultItem = resultArray.getJSONObject(0);
                                flag = resultItem.optString("p_out_mssg_flg", "");
                                message = resultItem.optString("p_out_mssg", "");
                            }
                        } else {
                            // Direct structure
                            flag = response.optString("p_out_mssg_flg", "");
                            message = response.optString("p_out_mssg", "");
                        }

                        Log.d(TAG, "Parsed - flag: " + flag + ", message: " + message);

                        showToast(message);
                        if ("S".equals(flag)) {
                            // Navigate to ReportVillageActivity for solar fence reporting
                            Intent intent = new Intent(SolarFenceActivity.this, ReportVillageActivity.class);
                            intent.putExtra("REPORTING_TYPE", "Solar Fence");
                            intent.putExtra("REPORT_TYPE_ID", SOLAR_FENCE_REPORT_TYPE_ID);
                            intent.putExtra("username", username);
                            intent.putExtra("user_id", userId);
                            startActivity(intent);
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing report response: " + e.getMessage(), e);
                        showToast("Error processing response: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Report submission Volley error: " + error.getMessage(), error);
                    String errorMsg = "Failed to submit report";

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = new String(error.networkResponse.data);
                        Log.e(TAG, "Report submission HTTP error " + statusCode + ": " + responseData);

                        if (statusCode == 401 || statusCode == 403) {
                            showToast("Authentication error: Please log in again");
                            return;
                        }
                        errorMsg = "HTTP " + statusCode + ": " + responseData;
                    }

                    showToast("Failed to submit report: " + errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("x-api-key", AppConfig.API_KEY);
                Log.d(TAG, "Report submission request headers: " + headers.toString());
                return headers;
            }
        };

        // Set retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Submitting report to: " + AppConfig.REPORT_TYPE);
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void submitReportWithoutVillages(String voltage) {
        Log.d(TAG, "Submitting solar fence report without village mapping");

        // Prepare JSON input without village IDs
        JSONObject jsonInput = new JSONObject();
        try {
            jsonInput.put("user_id", userId);
            jsonInput.put("report_type_id", SOLAR_FENCE_REPORT_TYPE_ID);
            jsonInput.put("description", "Solar fence voltage: " + voltage + " volts");
            jsonInput.put("stage", "dev");
            // No village_ids for fallback case
        } catch (Exception e) {
            Log.e(TAG, "Error building fallback request body: " + e.getMessage(), e);
            showToast("Error preparing fallback request");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                AppConfig.REPORT_TYPE,
                jsonInput,
                response -> {
                    Log.d(TAG, "Fallback report response: " + response.toString());
                    try {
                        // Handle both response structures:
                        // 1) Direct: {"p_out_mssg_flg": "S", "p_out_mssg": "message"}
                        // 2) Nested: {"RESULT": [{"p_out_mssg_flg": "S", "p_out_mssg": "message"}]}
                        String flag = "";
                        String message = "";

                        if (response.has("RESULT")) {
                            // Nested structure
                            JSONArray resultArray = response.getJSONArray("RESULT");
                            if (resultArray.length() > 0) {
                                JSONObject resultItem = resultArray.getJSONObject(0);
                                flag = resultItem.optString("p_out_mssg_flg", "");
                                message = resultItem.optString("p_out_mssg", "");
                            }
                        } else {
                            // Direct structure
                            flag = response.optString("p_out_mssg_flg", "");
                            message = response.optString("p_out_mssg", "");
                        }

                        Log.d(TAG, "Parsed - flag: " + flag + ", message: " + message);

                        showToast(message);
                        if ("S".equals(flag)) {
                            // Navigate to ReportVillageActivity for solar fence reporting
                            Intent intent = new Intent(SolarFenceActivity.this, ReportVillageActivity.class);
                            intent.putExtra("REPORTING_TYPE", "Solar Fence");
                            intent.putExtra("REPORT_TYPE_ID", SOLAR_FENCE_REPORT_TYPE_ID);
                            intent.putExtra("username", username);
                            intent.putExtra("user_id", userId);
                            startActivity(intent);
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing fallback response: " + e.getMessage(), e);
                        showToast("Error processing response: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Fallback report Volley error: " + error.getMessage(), error);
                    String errorMsg = "Failed to submit fallback report";

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = new String(error.networkResponse.data);
                        Log.e(TAG, "Fallback report HTTP error " + statusCode + ": " + responseData);

                        if (statusCode == 401 || statusCode == 403) {
                            showToast("Authentication error: Please log in again");
                            return;
                        }
                        errorMsg = "HTTP " + statusCode + ": " + responseData;
                    }

                    showToast("Failed to submit fallback report: " + errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("x-api-key", AppConfig.API_KEY);
                Log.d(TAG, "Fallback report request headers: " + headers.toString());
                return headers;
            }
        };

        // Set retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Submitting fallback report to: " + AppConfig.REPORT_TYPE);
        VolleySingleton.getInstance(this).addToRequestQueue(request);
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