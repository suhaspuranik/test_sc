package com.example.smart_cam;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import android.content.SharedPreferences;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smart_cam.ui.CheckboxListAdapter;

public class ReportVillageActivity extends BaseActivity {
    private static final String TAG = "ReportVillageActivity";
    private String reportingType;
    private int reportTypeId;
    private List<Area> areas;
    private AreaAdapter legacyAdapter;
    private ListView lvAreas; // Move to class level
    private boolean isElephantFoundScenario = false;
    private boolean isAreasSelectionPhase = false;
    private ArrayList<Integer> selectedAreaIds = new ArrayList<>();

    // New RecyclerView + Adapter
    private RecyclerView rvAreas;
    private CheckboxListAdapter adapter;

    private class Area {
        int id;
        String name;

        Area(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "Area{id=" + id + ", name='" + name + "'}";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Area other = (Area) obj;
            return this.id == other.id;
        }

        @Override
        public int hashCode() {
            return Integer.valueOf(id).hashCode();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_village);

        // Get reporting type, ID, and selected areas from Intent
        reportingType = getIntent().getStringExtra("REPORTING_TYPE");
        reportTypeId = getIntent().getIntExtra("REPORT_TYPE_ID", -1);
        ArrayList<Integer> selectedAreaIds = getIntent().getIntegerArrayListExtra("SELECTED_AREA_IDS");
        ArrayList<String> selectedAreaNames = getIntent().getStringArrayListExtra("SELECTED_AREA_NAMES");

        // Initialize UI elements
        TextView tvTitle = findViewById(R.id.tv_title);
        // Legacy ListView (no longer used after migration)
        lvAreas = null;
        rvAreas = findViewById(R.id.rv_villages);
        Button btnSendShare = findViewById(R.id.btn_send_share);
        Button btnBack = findViewById(R.id.btn_back);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (rvAreas != null) {
            rvAreas.setLayoutManager(new LinearLayoutManager(this));
        }

        // Set title based on scenario
        boolean noElephantFound = getIntent().getBooleanExtra("NO_ELEPHANT_FOUND", false);
        String titleSuffix;
        if (noElephantFound) {
            titleSuffix = ": Report to Villages (No Elephant Found)";
        } else if ("Solar Fence".equals(reportingType)) {
            titleSuffix = ": Report to Villages";
        } else {
            titleSuffix = ": Select Areas (Elephant Found)";
        }
        tvTitle.setText(reportingType != null ? reportingType + titleSuffix : "Report to Villages");

        // Check the scenario and load appropriate data
        if ("Solar Fence".equals(reportingType)) {
            // For Solar Fence: Load villages directly
            fetchUserMappedVillages();
        } else if (noElephantFound) {
            // For No Elephant Found: Load villages directly
            fetchUserMappedVillages();
        } else {
            // Elephant Found flow: if areas were passed from previous screen, we're in step 2
            ArrayList<Integer> passedAreaIds = getIntent().getIntegerArrayListExtra("SELECTED_AREA_IDS");
            ArrayList<String> passedAreaNames = getIntent().getStringArrayListExtra("SELECTED_AREA_NAMES");
            if (passedAreaIds != null && passedAreaNames != null && !passedAreaIds.isEmpty() && passedAreaIds.size() == passedAreaNames.size()) {
                isElephantFoundScenario = true;
                isAreasSelectionPhase = false; // move directly to villages phase
                selectedAreaIds = new ArrayList<>(passedAreaIds);
                Log.d(TAG, "Elephant Found step 2: received selected areas " + selectedAreaIds);
                // Load villages now
                fetchUserMappedVillages();
            } else {
                // Step 1: Load areas first
                isElephantFoundScenario = true;
                isAreasSelectionPhase = true;
                fetchUserMappedAreas();
            }
        }

        // Handle Back button
        btnBack.setOnClickListener(v -> finish());

        // Handle Send/Share button
        updateButtonText();
        btnSendShare.setOnClickListener(v -> {
            if (adapter == null) {
                showToast("Please wait while data is being loaded");
                return;
            }

            if (isElephantFoundScenario && isAreasSelectionPhase) {
                // In areas selection phase for elephant found scenario
                ArrayList<Integer> selectedAreas = adapter.getSelectedIds();
                Log.d(TAG, "Areas selection phase - selected area IDs: " + selectedAreas.toString());
                if (selectedAreas.isEmpty()) {
                    showToast("Please select at least one area");
                    return;
                }
                // Store selected areas and move to villages selection phase
                handleAreasSelectionPhase(selectedAreas);
            } else {
                // In villages selection phase or other scenarios (including Solar Fence)
                ArrayList<Integer> finalSelectedAreaIds = adapter.getSelectedIds();
                Log.d(TAG, "Areas/Villages selection phase - selected IDs: " + finalSelectedAreaIds.toString());
                if (finalSelectedAreaIds.isEmpty()) {
                    showToast("Please select at least one area/village");
                    return;
                }
                submitReport(finalSelectedAreaIds);
            }
        });

        // Add scroll listener to debug scrolling issues (not needed for RecyclerView state, kept for parity)
        // Bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.nav_add);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, DashboardActivity.class);
                // Get username and user_id from current session
                SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
                String username = prefs.getString("username", "");
                int sessionUserId = prefs.getInt("user_id", -1);
                intent.putExtra("username", username);
                intent.putExtra("user_id", sessionUserId);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_add) {
                Intent intent = new Intent(this, AddReportingActivity.class);
                // Get username and user_id from current session
                SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
                String username = prefs.getString("username", "");
                int sessionUserId = prefs.getInt("user_id", -1);
                intent.putExtra("username", username);
                intent.putExtra("user_id", sessionUserId);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_menu) {
                Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void fetchUserMappedAreas() {
        Log.d(TAG, "Fetching user mapped areas for elephant found scenario.");

        // Validate API key first
        if (!isValidApiKey()) {
            showToast("API key is not configured properly. Please contact support.");
            showFallbackMessage();
            return;
        }

        // Get user_id from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        int sessionUserId = prefs.getInt("user_id", -1);
        String username = prefs.getString("username", "");

        if (sessionUserId == -1 || username.isEmpty()) {
            showToast("Session expired. Please log in again.");
            Log.e(TAG, "Session validation failed - user_id: " + sessionUserId + ", username: " + username);
            return;
        }

        Log.d(TAG, "Session validated - user_id: " + sessionUserId + ", username: " + username);
        Log.d(TAG, "Using user_id: " + sessionUserId + " for area mapping API call");

        // Log API configuration for debugging
        logApiConfiguration();

        // Prepare JSON input for user_mapped_areas
        JSONObject jsonInput = new JSONObject();
        try {
            jsonInput.put("user_id", sessionUserId);
            jsonInput.put("stage", "dev");
        } catch (Exception e) {
            Log.e(TAG, "Error building user_mapped_areas request body: " + e.getMessage(), e);
            showToast("Error preparing request");
            return;
        }

        Log.d(TAG, "Request body: " + jsonInput.toString());
        Log.d(TAG, "API Key being used: " + AppConfig.API_KEY);
        Log.d(TAG, "API URL: " + AppConfig.GET_AREAS);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                AppConfig.GET_AREAS,
                jsonInput,
                response -> {
                    Log.d(TAG, "User mapped areas response: " + response.toString());
                    try {
                        // Check if response has RESULT array
                        if (!response.has("RESULT")) {
                            Log.e(TAG, "Response does not contain RESULT array");
                            showToast("Invalid response format from server");
                            return;
                        }

                        JSONArray areaArray = response.getJSONArray("RESULT");
                        Log.d(TAG, "Found " + areaArray.length() + " areas in response");

                        if (areaArray.length() == 0) {
                            Log.w(TAG, "No areas returned from API");
                            showToast("No areas found for your user account");
                            return;
                        }

                        areas = new ArrayList<>();
                        for (int i = 0; i < areaArray.length(); i++) {
                            JSONObject areaObj = areaArray.getJSONObject(i);
                            Log.d(TAG, "Processing area " + i + ": " + areaObj.toString());

                            // Try different possible field names
                            int id = -1;
                            String name = "";

                            if (areaObj.has("area_id")) {
                                id = areaObj.getInt("area_id");
                            } else if (areaObj.has("id")) {
                                id = areaObj.getInt("id");
                            }

                            if (areaObj.has("area_name")) {
                                name = areaObj.getString("area_name");
                            } else if (areaObj.has("name")) {
                                name = areaObj.getString("name");
                            }

                            if (id != -1 && !name.isEmpty()) {
                                areas.add(new Area(id, name));
                                Log.d(TAG, "Added area: ID=" + id + ", Name=" + name);
                            } else {
                                Log.w(TAG, "Skipping area due to missing ID or name: " + areaObj.toString());
                            }
                        }

                        if (areas.isEmpty()) {
                            Log.e(TAG, "No valid areas could be parsed from response");
                            showToast("Error parsing area data from server");
                            return;
                        }

                        Log.d(TAG, "Successfully parsed " + areas.size() + " areas");
                        // Bind to Recycler
                        ArrayList<CheckboxListAdapter.Item> items = new ArrayList<>();
                        for (Area a : areas) {
                            items.add(new CheckboxListAdapter.Item(a.id, a.name));
                        }
                        adapter = new CheckboxListAdapter(this, items);
                        if (rvAreas != null) {
                            rvAreas.setAdapter(adapter);
                        }
                        showToast("Areas loaded successfully: " + areas.size() + " found");

                        // Update title and button text for areas selection phase
                        updateTitleForPhase();
                        updateButtonText();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing user mapped areas response: " + e.getMessage(), e);
                        showToast("Error loading areas: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "User mapped areas Volley error: " + error.getMessage(), error);
                    String errorMsg = "Failed to load areas";

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = new String(error.networkResponse.data);
                        Log.e(TAG, "User mapped areas HTTP error " + statusCode + ": " + responseData);

                        if (statusCode == 401) {
                            showToast("Authentication failed: Please log in again");
                            // Redirect to login or handle session expiry
                            return;
                        } else if (statusCode == 403) {
                            Log.e(TAG, "403 Forbidden - This suggests an API key or permission issue");

                            // Check if API key might be the issue
                            if (AppConfig.API_KEY == null || AppConfig.API_KEY.isEmpty()) {
                                showToast("API key configuration error. Please contact support.");
                                Log.e(TAG, "API key appears to be invalid: " + AppConfig.API_KEY);
                            } else {
                                showToast("Access forbidden: You don't have permission to access area data");
                            }

                            // Show fallback message and allow user to proceed
                            showFallbackMessage();
                            return;
                        } else if (statusCode == 500) {
                            showToast("Server error: Please try again later");
                            return;
                        }
                        errorMsg = "HTTP " + statusCode + ": " + responseData;
                    } else if (error instanceof com.android.volley.AuthFailureError) {
                        Log.e(TAG, "Authentication failure - API key may be invalid");
                        showToast("Authentication failed: Please check your API key");
                        return;
                    } else if (error instanceof com.android.volley.TimeoutError) {
                        showToast("Request timeout: Please check your internet connection");
                        return;
                    } else if (error instanceof com.android.volley.NoConnectionError) {
                        showToast("No internet connection: Please check your network");
                        return;
                    }

                    showToast("Failed to load areas: " + errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("x-api-key", AppConfig.API_KEY);
                Log.d(TAG, "User mapped areas request headers: " + headers.toString());
                Log.d(TAG, "API Key being used: " + AppConfig.API_KEY);
                return headers;
            }
        };

        // Set retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Submitting user mapped areas request to: " + AppConfig.GET_AREAS);
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void fetchUserMappedVillages() {
        Log.d(TAG, "Fetching user mapped villages for reporting.");

        // Validate API key first
        if (!isValidApiKey()) {
            showToast("API key is not configured properly. Please contact support.");
            showFallbackMessage();
            return;
        }

        // Get user_id from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        int sessionUserId = prefs.getInt("user_id", -1);
        String username = prefs.getString("username", "");

        if (sessionUserId == -1 || username.isEmpty()) {
            showToast("Session expired. Please log in again.");
            Log.e(TAG, "Session validation failed - user_id: " + sessionUserId + ", username: " + username);
            return;
        }

        Log.d(TAG, "Session validated - user_id: " + sessionUserId + ", username: " + username);
        Log.d(TAG, "Using user_id: " + sessionUserId + " for village mapping API call");

        // Log API configuration for debugging
        logApiConfiguration();

        // Prepare JSON input for user_mapped_villages
        JSONObject jsonInput = new JSONObject();
        try {
            jsonInput.put("user_id", sessionUserId);
            jsonInput.put("stage", "dev");
        } catch (Exception e) {
            Log.e(TAG, "Error building user_mapped_villages request body: " + e.getMessage(), e);
            showToast("Error preparing request");
            return;
        }

        Log.d(TAG, "Request body: " + jsonInput.toString());
        Log.d(TAG, "API Key being used: " + AppConfig.API_KEY);
        Log.d(TAG, "API URL: " + AppConfig.USER_MAPPED_VILLAGE);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                AppConfig.USER_MAPPED_VILLAGE,
                jsonInput,
                response -> {
                    Log.d(TAG, "User mapped villages response: " + response.toString());
                    try {
                        // Check if response has RESULT array
                        if (!response.has("RESULT")) {
                            Log.e(TAG, "Response does not contain RESULT array");
                            showToast("Invalid response format from server");
                            return;
                        }

                        JSONArray villageArray = response.getJSONArray("RESULT");
                        Log.d(TAG, "Found " + villageArray.length() + " villages in response");

                        if (villageArray.length() == 0) {
                            Log.w(TAG, "No villages returned from API");
                            showToast("No villages found for your user account");
                            return;
                        }

                        areas = new ArrayList<>();
                        for (int i = 0; i < villageArray.length(); i++) {
                            JSONObject villageObj = villageArray.getJSONObject(i);
                            Log.d(TAG, "Processing village " + i + ": " + villageObj.toString());

                            // Try different possible field names
                            int id = -1;
                            String name = "";

                            if (villageObj.has("village_id")) {
                                id = villageObj.getInt("village_id");
                            } else if (villageObj.has("id")) {
                                id = villageObj.getInt("id");
                            }

                            if (villageObj.has("village_name")) {
                                name = villageObj.getString("village_name");
                            } else if (villageObj.has("name")) {
                                name = villageObj.getString("name");
                            }

                            if (id != -1 && !name.isEmpty()) {
                                areas.add(new Area(id, name));
                                Log.d(TAG, "Added village: ID=" + id + ", Name=" + name);
                            } else {
                                Log.w(TAG, "Skipping village due to missing ID or name: " + villageObj.toString());
                            }
                        }

                        if (areas.isEmpty()) {
                            Log.e(TAG, "No valid villages could be parsed from response");
                            showToast("Error parsing village data from server");
                            return;
                        }

                        Log.d(TAG, "Successfully parsed " + areas.size() + " villages");
                        // Bind to Recycler
                        ArrayList<CheckboxListAdapter.Item> items = new ArrayList<>();
                        for (Area a : areas) {
                            items.add(new CheckboxListAdapter.Item(a.id, a.name));
                        }
                        adapter = new CheckboxListAdapter(this, items);
                        if (rvAreas != null) {
                            rvAreas.setAdapter(adapter);
                        }
                        showToast("Villages loaded successfully: " + areas.size() + " found");

                        // Update title and button text for villages selection phase
                        updateTitleForPhase();
                        updateButtonText();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing user mapped villages response: " + e.getMessage(), e);
                        showToast("Error loading villages: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "User mapped villages Volley error: " + error.getMessage(), error);
                    String errorMsg = "Failed to load villages";

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = new String(error.networkResponse.data);
                        Log.e(TAG, "User mapped villages HTTP error " + statusCode + ": " + responseData);

                        if (statusCode == 401) {
                            showToast("Authentication failed: Please log in again");
                            // Redirect to login or handle session expiry
                            return;
                        } else if (statusCode == 403) {
                            Log.e(TAG, "403 Forbidden - This suggests an API key or permission issue");

                            // Check if API key might be the issue
                            if (AppConfig.API_KEY == null || AppConfig.API_KEY.isEmpty()) {
                                showToast("API key configuration error. Please contact support.");
                                Log.e(TAG, "API key appears to be invalid: " + AppConfig.API_KEY);
                            } else {
                                showToast("Access forbidden: You don't have permission to access village data");
                            }

                            // Show fallback message and allow user to proceed
                            showFallbackMessage();
                            return;
                        } else if (statusCode == 500) {
                            showToast("Server error: Please try again later");
                            return;
                        }
                        errorMsg = "HTTP " + statusCode + ": " + responseData;
                    } else if (error instanceof com.android.volley.AuthFailureError) {
                        Log.e(TAG, "Authentication failure - API key may be invalid");
                        showToast("Authentication failed: Please check your API key");
                        return;
                    } else if (error instanceof com.android.volley.TimeoutError) {
                        showToast("Request timeout: Please check your internet connection");
                        return;
                    } else if (error instanceof com.android.volley.NoConnectionError) {
                        showToast("No internet connection: Please check your network");
                        return;
                    }

                    showToast("Failed to load villages: " + errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("x-api-key", AppConfig.API_KEY);
                Log.d(TAG, "User mapped villages request headers: " + headers.toString());
                Log.d(TAG, "API Key being used: " + AppConfig.API_KEY);
                return headers;
            }
        };

        // Set retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Submitting user mapped villages request to: " + AppConfig.USER_MAPPED_VILLAGE);
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void updateTitleForPhase() {
        TextView tvTitle = findViewById(R.id.tv_title);
        if (isElephantFoundScenario && isAreasSelectionPhase) {
            tvTitle.setText(reportingType + ": Select Areas (Elephant Found)");
        } else if (isElephantFoundScenario && !isAreasSelectionPhase) {
            tvTitle.setText(reportingType + ": Select Villages (Elephant Found)");
        } else if ("Solar Fence".equals(reportingType)) {
            tvTitle.setText(reportingType + ": Report to Villages");
        } else {
            tvTitle.setText("Report to Villages");
        }
    }

    private void handleAreasSelectionPhase(ArrayList<Integer> selectedAreas) {
        // Store selected areas and move to villages selection phase
        selectedAreaIds = selectedAreas;
        isAreasSelectionPhase = false;
        fetchUserMappedVillages();
    }

    private void updateButtonText() {
        Button btnSendShare = findViewById(R.id.btn_send_share);
        if (isElephantFoundScenario && isAreasSelectionPhase) {
            btnSendShare.setText("Next");
        } else {
            btnSendShare.setText("Share");
        }
    }

    private void submitReport(ArrayList<Integer> villageIds) {
        Log.d(TAG, "Submitting report for villages: " + villageIds.toString());

        // Get user_id from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        int sessionUserId = prefs.getInt("user_id", -1);
        String username = prefs.getString("username", "");

        if (sessionUserId == -1 || username.isEmpty()) {
            showToast("Session expired. Please log in again.");
            Log.e(TAG, "Session validation failed in submitReport - user_id: " + sessionUserId + ", username: " + username);
            return;
        }

        Log.d(TAG, "Session validated in submitReport - user_id: " + sessionUserId + ", username: " + username);

        // Validate that user has access to the selected villages
        if (!validateUserAreaAccess(villageIds)) {
            Log.e(TAG, "User does not have access to some selected villages: " + villageIds.toString());
            showToast("You don't have access to some selected villages. Please select different villages.");
            Log.e(TAG, "This is a critical error - user selected areas not in their mapped areas list");
            return;
        }

        // Check if this is a solar fence report or other report (types 4 and 5)
        if (reportTypeId == 4 || reportTypeId == 5) {
            submitSolarFenceReport(sessionUserId, villageIds);
        } else {
            submitRegularReport(sessionUserId, villageIds);
        }
    }

    private void submitSolarFenceReport(int sessionUserId, ArrayList<Integer> villageIds) {
        Log.d(TAG, "Submitting report type " + reportTypeId + " for villages: " + villageIds.toString());

        // Build payload per procedure rules for report types 4 and 5: no animal_found, no area_ids
        String passedDescription = getIntent().getStringExtra("REPORT_DESCRIPTION");
        String description = (passedDescription != null && !passedDescription.isEmpty()) ? passedDescription : "Report";
        Log.d(TAG, "Report details - description: " + description);
        Log.d(TAG, "Report type ID: " + reportTypeId);
        Log.d(TAG, "Village IDs being sent: " + villageIds.toString());

        JSONObject jsonInput = new JSONObject();
        try {
            jsonInput.put("user_id", sessionUserId);
            jsonInput.put("report_type_id", reportTypeId);
            jsonInput.put("description", description);
            // Do NOT include animal_found or area_ids for report types 4/5
            Log.d(TAG, "Excluding animal_found and area_ids for report type " + reportTypeId);
            jsonInput.put("stage", "dev");
        } catch (Exception e) {
            Log.e(TAG, "Error building solar fence request body: " + e.getMessage(), e);
            showToast("Error preparing request");
            return;
        }

        Log.d(TAG, "Report JSON request: " + jsonInput.toString());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                AppConfig.REPORT_TYPE,
                jsonInput,
                response -> {
                    Log.d(TAG, "Report submission response: " + response.toString());
                    try {
                        String flag = "";
                        String message = "";

                        if (response.has("RESULT")) {
                            JSONArray resultArray = response.getJSONArray("RESULT");
                            if (resultArray.length() > 0) {
                                JSONObject resultItem = resultArray.getJSONObject(0);
                                flag = resultItem.optString("p_out_mssg_flg", "");
                                message = resultItem.optString("p_out_mssg", "");
                            }
                        } else {
                            flag = response.optString("p_out_mssg_flg", "");
                            message = response.optString("p_out_mssg", "");
                        }

                        Log.d(TAG, "Parsed - flag: " + flag + ", message: " + message);

                        if ("S".equals(flag)) {
                            int reportId = parseReportIdFromMessage(message);
                            if (reportId != -1) {
                                Log.d(TAG, "Report submitted successfully with ID: " + reportId + ". Now calling submitReportAlert for villages: " + villageIds.toString());
                                submitReportAlert(sessionUserId, reportId, villageIds);
                            } else {
                                Log.e(TAG, "Failed to parse report ID from message: " + message);
                                showToast("Failed to parse report ID from message");
                            }
                        } else {
                            Log.e(TAG, "Report submission failed with flag: " + flag + ", message: " + message);
                            showToast(message);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
                        showToast("Error processing response: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Report Volley error: " + error.getMessage(), error);
                    String errorMsg = "Failed to submit report";

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = new String(error.networkResponse.data);
                        Log.e(TAG, "Report HTTP error " + statusCode + ": " + responseData);

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
                Log.d(TAG, "Report request headers: " + headers.toString());
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Submitting report to: " + AppConfig.REPORT_TYPE);
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void submitRegularReport(int sessionUserId, ArrayList<Integer> villageIds) {
        Log.d(TAG, "Submitting regular report for villages: " + villageIds.toString());

        boolean noElephantFound = getIntent().getBooleanExtra("NO_ELEPHANT_FOUND", false);
        String animalFound = noElephantFound ? "N" : "Y";
        String description = noElephantFound ? reportingType + " report: No elephant movement" : reportingType + " report";

        Log.d(TAG, "Regular report details - animal_found: " + animalFound + ", description: " + description + ", no_elephant_found: " + noElephantFound);
        Log.d(TAG, "Report type ID: " + reportTypeId);
        Log.d(TAG, "Village IDs being sent: " + villageIds.toString());

        JSONObject jsonInput = new JSONObject();
        try {
            jsonInput.put("user_id", sessionUserId);
            jsonInput.put("report_type_id", reportTypeId);
            jsonInput.put("description", description);

            if (reportTypeId == 1 || reportTypeId == 2 || reportTypeId == 3) {
                // Types 1-3: animal_found required; area_ids only when animal_found = 'Y'
                jsonInput.put("animal_found", animalFound);
                if (!noElephantFound) {
                    // For Elephant Found flow, area_ids must be the previously selected areas
                    if ((selectedAreaIds == null || selectedAreaIds.isEmpty())) {
                        // Try to recover from intent in case field wasn't set earlier
                        ArrayList<Integer> intentAreaIds = getIntent().getIntegerArrayListExtra("SELECTED_AREA_IDS");
                        if (intentAreaIds != null && !intentAreaIds.isEmpty()) {
                            selectedAreaIds = new ArrayList<>(intentAreaIds);
                            Log.w(TAG, "Recovered selectedAreaIds from intent: " + selectedAreaIds);
                        }
                    }

                    if (selectedAreaIds == null || selectedAreaIds.isEmpty()) {
                        Log.e(TAG, "No selected areas available for Elephant Found. Aborting to avoid sending village IDs as area_ids.");
                        showToast("Missing selected areas. Please go back and select areas again.");
                        return;
                    }

                    JSONArray areaIdsArray = new JSONArray(selectedAreaIds);
                    jsonInput.put("area_ids", areaIdsArray);
                    Log.d(TAG, "Including area_ids from selectedAreaIds for report type " + reportTypeId + ": " + selectedAreaIds);
                } else {
                    Log.d(TAG, "Excluding area_ids for animal_found = N on report type " + reportTypeId);
                }
            } else if (reportTypeId == 4 || reportTypeId == 5) {
                // Types 4-5: animal_found and area_ids must NOT be provided
                Log.d(TAG, "Excluding animal_found and area_ids for report type " + reportTypeId);
            }

            jsonInput.put("stage", "dev");
        } catch (Exception e) {
            Log.e(TAG, "Error building request body: " + e.getMessage(), e);
            showToast("Error preparing request");
            return;
        }

        Log.d(TAG, "Regular report JSON request: " + jsonInput.toString());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                AppConfig.REPORT_TYPE,
                jsonInput,
                response -> {
                    Log.d(TAG, "Report submission response: " + response.toString());
                    try {
                        String flag = "";
                        String message = "";

                        if (response.has("RESULT")) {
                            JSONArray resultArray = response.getJSONArray("RESULT");
                            if (resultArray.length() > 0) {
                                JSONObject resultItem = resultArray.getJSONObject(0);
                                flag = resultItem.optString("p_out_mssg_flg", "");
                                message = resultItem.optString("p_out_mssg", "");
                            }
                        } else {
                            flag = response.optString("p_out_mssg_flg", "");
                            message = response.optString("p_out_mssg", "");
                        }

                        Log.d(TAG, "Parsed - flag: " + flag + ", message: " + message);

                        if ("S".equals(flag)) {
                            int reportId = parseReportIdFromMessage(message);
                            if (reportId != -1) {
                                Log.d(TAG, "Report submitted successfully with ID: " + reportId + ". Now calling submitReportAlert for villages: " + villageIds.toString());
                                submitReportAlert(sessionUserId, reportId, villageIds);
                            } else {
                                Log.e(TAG, "Failed to parse report ID from message: " + message);
                                showToast("Failed to parse report ID from message");
                            }
                        } else {
                            Log.e(TAG, "Report submission failed with flag: " + flag + ", message: " + message);
                            showToast(message);
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

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Submitting report to: " + AppConfig.REPORT_TYPE);
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private int parseReportIdFromMessage(String message) {
        try {
            // Example message: "Report submitted successfully. Report ID: 31"
            String[] parts = message.split("Report ID: ");
            if (parts.length > 1) {
                return Integer.parseInt(parts[1].trim());
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing report ID: " + e.getMessage(), e);
        }
        return -1;
    }

    private void submitReportAlert(int userId, int reportId, ArrayList<Integer> villageIds) {
        Log.d(TAG, "Submitting report alert for report_id: " + reportId + ", villages: " + villageIds.toString());

        // Prepare JSON input for submit_report_alert procedure
        JSONObject jsonInput = new JSONObject();
        try {
            jsonInput.put("user_id", userId);
            jsonInput.put("report_id", reportId);
            JSONArray villageIdsArray = new JSONArray(villageIds);
            jsonInput.put("village_ids", villageIdsArray);
            jsonInput.put("stage", "dev");
        } catch (Exception e) {
            Log.e(TAG, "Error building alert request body: " + e.getMessage(), e);
            showToast("Error preparing alert request");
            return;
        }

        Log.d(TAG, "Submitting report alert request: " + jsonInput.toString());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                AppConfig.SUBMIT_REPORT,
                jsonInput,
                response -> {
                    Log.d(TAG, "Alert submission response: " + response.toString());
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
                            Intent intent = new Intent(ReportVillageActivity.this, AddReportingActivity.class);
                            // Get username and user_id from current session
                            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
                            String username = prefs.getString("username", "");
                            int sessionUserId = prefs.getInt("user_id", -1);
                            intent.putExtra("username", username);
                            intent.putExtra("user_id", sessionUserId);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing alert response: " + e.getMessage(), e);
                        showToast("Error processing alert response: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Alert submission Volley error: " + error.getMessage(), error);
                    String errorMsg = "Failed to send alerts";

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = new String(error.networkResponse.data);
                        Log.e(TAG, "Alert submission HTTP error " + statusCode + ": " + responseData);

                        if (statusCode == 401 || statusCode == 403) {
                            showToast("Authentication error: Please log in again");
                            return;
                        }
                        errorMsg = "HTTP " + statusCode + ": " + responseData;
                    }

                    showToast("Failed to send alerts: " + errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("x-api-key", AppConfig.API_KEY);
                Log.d(TAG, "Alert submission request headers: " + headers.toString());
                return headers;
            }
        };

        // Set retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Submitting alert to: " + AppConfig.SUBMIT_REPORT);
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

    private void showFallbackMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("API Connection Issue");
        builder.setMessage("Unable to connect to the server to load village data. This could be due to:\n\n" +
                "• Network connectivity issues\n" +
                "• Server maintenance\n" +
                "• API key configuration problems\n\n" +
                "You can still proceed with the report using default areas, or try again. What would you like to do?");
        builder.setPositiveButton("Continue with Default", (dialog, which) -> {
            // Create a default area list so user can proceed
            createDefaultAreaList();
        });
        builder.setNeutralButton("Try Again", (dialog, which) -> {
            // Retry the API call
            fetchUserMappedVillages();
        });
        builder.setNegativeButton("Check API Key", (dialog, which) -> {
            // Show API key information for debugging
            showApiKeyInfo();
        });
        builder.show();
    }

    private void showApiKeyInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("API Key Information");

        String apiKeyInfo = "Current API Key Status:\n\n";
        if (AppConfig.API_KEY == null) {
            apiKeyInfo += "• API Key: NULL\n";
        } else if (AppConfig.API_KEY.isEmpty()) {
            apiKeyInfo += "• API Key: EMPTY\n";
        } else {
            apiKeyInfo += "• API Key: " + AppConfig.API_KEY.substring(0, Math.min(10, AppConfig.API_KEY.length())) + "...\n";
            apiKeyInfo += "• Length: " + AppConfig.API_KEY.length() + " characters\n";
        }

        apiKeyInfo += "\nAPI URLs:\n";
        apiKeyInfo += "• Village API: " + AppConfig.USER_MAPPED_VILLAGE + "\n";
        apiKeyInfo += "• Report API: " + AppConfig.REPORT_TYPE + "\n";

        apiKeyInfo += "\nIf the API key appears to be a placeholder or default value, please contact your system administrator to update it.";

        builder.setMessage(apiKeyInfo);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void createDefaultAreaList() {
        // Create a default area list with generic options
        areas = new ArrayList<>();
        areas.add(new Area(1, "Default Area 1"));
        areas.add(new Area(2, "Default Area 2"));

        ArrayList<CheckboxListAdapter.Item> items = new ArrayList<>();
        for (Area a : areas) {
            items.add(new CheckboxListAdapter.Item(a.id, a.name));
        }
        adapter = new CheckboxListAdapter(this, items);
        if (rvAreas != null) {
            rvAreas.setAdapter(adapter);
        }
        showToast("Using default areas. You can now proceed with the report.");
    }

    private boolean isValidApiKey() {
        // Check if API key is null or empty
        if (AppConfig.API_KEY == null || AppConfig.API_KEY.isEmpty()) {
            Log.e(TAG, "API key is null or empty");
            return false;
        }

        // Check if it looks like a valid API key (should be a reasonable length)
        if (AppConfig.API_KEY.length() < 10) {
            Log.e(TAG, "API key appears to be too short: " + AppConfig.API_KEY.length() + " characters");
            return false;
        }

        Log.d(TAG, "API key validation passed");
        return true;
    }

    private void logApiConfiguration() {
        Log.d(TAG, "=== API Configuration Debug Info ===");
        Log.d(TAG, "API Key: " + (AppConfig.API_KEY != null ? AppConfig.API_KEY.substring(0, Math.min(10, AppConfig.API_KEY.length())) + "..." : "NULL"));
        Log.d(TAG, "API Key Length: " + (AppConfig.API_KEY != null ? AppConfig.API_KEY.length() : 0));
        Log.d(TAG, "GET_AREAS URL: " + AppConfig.GET_AREAS);
        Log.d(TAG, "USER_MAPPED_VILLAGE URL: " + AppConfig.USER_MAPPED_VILLAGE);
        Log.d(TAG, "REPORT_TYPE URL: " + AppConfig.REPORT_TYPE);
        Log.d(TAG, "SUBMIT_REPORT URL: " + AppConfig.SUBMIT_REPORT);
        Log.d(TAG, "=====================================");
    }

    private boolean validateUserAreaAccess(ArrayList<Integer> selectedVillageIds) {
        Log.d(TAG, "Validating user village access for selected IDs: " + selectedVillageIds.toString());

        // If areas list is null or empty, we can't validate - assume it's OK
        if (areas == null || areas.isEmpty()) {
            Log.w(TAG, "Villages list is null or empty, skipping validation");
            return true;
        }

        // Create a set of valid village IDs that the user has access to
        Set<Integer> validVillageIds = new HashSet<>();
        for (Area area : areas) {
            validVillageIds.add(area.id);
        }

        Log.d(TAG, "User has access to village IDs: " + validVillageIds.toString());
        Log.d(TAG, "Total areas loaded: " + areas.size());
        Log.d(TAG, "Areas list: " + areas.toString());

        // Check if all selected village IDs are in the valid set
        for (Integer selectedId : selectedVillageIds) {
            if (!validVillageIds.contains(selectedId)) {
                Log.e(TAG, "User does not have access to village ID: " + selectedId);
                Log.e(TAG, "Valid village IDs: " + validVillageIds.toString());
                Log.e(TAG, "Selected village IDs: " + selectedVillageIds.toString());
                Log.e(TAG, "This suggests a UI issue - user selected an area not in their mapped areas");
                return false;
            }
        }

        Log.d(TAG, "All selected village IDs are valid for this user");
        return true;
    }

    private class AreaAdapter extends ArrayAdapter<Area> {
        private final ArrayList<Area> selectedAreas;
        private final HashMap<Area, Boolean> areaCheckedStates; // Track by Area object for more reliable tracking
        private boolean isUpdatingCheckbox = false; // Flag to prevent listener loops

        public AreaAdapter(Context context, List<Area> areas) {
            super(context, 0, areas);
            this.selectedAreas = new ArrayList<>(); // Start with empty selection
            this.areaCheckedStates = new HashMap<>();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Area area = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_village, parent, false);
            }

            CheckBox checkBox = convertView.findViewById(R.id.cb_village);
            checkBox.setText(area.name);
            checkBox.setTextColor(getContext().getResources().getColor(R.color.black));

            // Store the Area object in the checkbox tag for more reliable tracking
            checkBox.setTag(area);

            // Set flag to prevent listener loops during programmatic updates
            isUpdatingCheckbox = true;

            // Remove the OnCheckedChangeListener to prevent automatic state changes
            checkBox.setOnCheckedChangeListener(null);

            // Restore the checked state from our tracking
            boolean currentCheckedState = selectedAreas.contains(area);
            checkBox.setChecked(currentCheckedState);

            // Update our tracking map
            areaCheckedStates.put(area, currentCheckedState);

            // Reset flag
            isUpdatingCheckbox = false;

            // Set our custom listener
            checkBox.setOnClickListener(v -> {
                // Prevent recursive calls during programmatic updates
                if (isUpdatingCheckbox) {
                    return;
                }

                CheckBox clickedCheckBox = (CheckBox) v;
                Area clickedArea = (Area) clickedCheckBox.getTag();

                if (clickedArea != null) {
                    // Use the actual state after click
                    boolean newState = clickedCheckBox.isChecked();

                    // Set flag to prevent listener loops
                    isUpdatingCheckbox = true;

                    // Update our tracking
                    areaCheckedStates.put(clickedArea, newState);

                    if (newState) {
                        if (!selectedAreas.contains(clickedArea)) {
                            selectedAreas.add(clickedArea);
                            Log.d(TAG, "Area selected: " + clickedArea.name + " (ID: " + clickedArea.id + ")");
                        }
                    } else {
                        selectedAreas.remove(clickedArea);
                        Log.d(TAG, "Area deselected: " + clickedArea.name + " (ID: " + clickedArea.id + ")");
                    }

                    Log.d(TAG, "Current selected areas count: " + selectedAreas.size());

                    // Reset flag
                    isUpdatingCheckbox = false;
                }
            });

            return convertView;
        }

        public ArrayList<Integer> getSelectedAreaIds() {
            ArrayList<Integer> selectedIds = new ArrayList<>();
            for (Area area : selectedAreas) {
                selectedIds.add(area.id);
            }
            Log.d(TAG, "AreaAdapter.getSelectedAreaIds() - returning " + selectedIds.size() + " area IDs: " + selectedIds.toString());
            Log.d(TAG, "Selected areas details: " + selectedAreas.toString());
            return selectedIds;
        }

        // Method to get checked state for a specific area
        public boolean isAreaChecked(Area area) {
            Boolean state = areaCheckedStates.get(area);
            return state != null ? state : false;
        }

        // Method to set checked state for a specific area
        public void setAreaChecked(Area area, boolean checked) {
            areaCheckedStates.put(area, checked);
            if (checked && !selectedAreas.contains(area)) {
                selectedAreas.add(area);
            } else if (!checked && selectedAreas.contains(area)) {
                selectedAreas.remove(area);
            }
        }

        // Method to safely update checkbox state without triggering listener
        public void updateCheckboxState(Area area, boolean checked) {
            areaCheckedStates.put(area, checked);
        }

        // Method to get all selected areas for debugging
        public String getSelectedAreasDebugInfo() {
            StringBuilder sb = new StringBuilder();
            sb.append("Selected areas count: ").append(selectedAreas.size()).append("\n");
            for (Area area : selectedAreas) {
                sb.append("- ").append(area.name).append(" (ID: ").append(area.id).append(")\n");
            }
            return sb.toString();
        }

        // Method to force refresh views without triggering listeners
        public void forceRefreshViews() {
            // This will call getView for all visible items, properly restoring their states
            Log.d(TAG, "Force refreshing all views to restore checkbox states");
            notifyDataSetChanged();
        }

        // Method to manually restore checkbox states for all visible items
        public void restoreCheckboxStates() {
            Log.d(TAG, "Restoring checkbox states. Selected areas: " + getSelectedAreasDebugInfo());

            // Force a refresh of all visible views
            for (int i = 0; i < getCount(); i++) {
                Area area = getItem(i);
                if (area != null) {
                    boolean shouldBeChecked = selectedAreas.contains(area);
                    areaCheckedStates.put(area, shouldBeChecked);
                }
            }

            // Notify the adapter to refresh the views
            notifyDataSetChanged();
        }

        // Method to validate and fix any inconsistencies
        public void validateAndFixInconsistencies() {
            Log.d(TAG, "Validating checkbox states...");

            // Check for any inconsistencies between our tracking map and selected areas list
            for (int i = 0; i < getCount(); i++) {
                Area area = getItem(i);
                if (area != null) {
                    boolean inSelectedList = selectedAreas.contains(area);
                    Boolean areaState = areaCheckedStates.get(area);

                    // If there's an inconsistency, fix it
                    if (areaState == null || areaState != inSelectedList) {
                        Log.d(TAG, "Fixing inconsistency for " + area.name + " at position " + i);
                        areaCheckedStates.put(area, inSelectedList);
                    }
                }
            }

            Log.d(TAG, "Validation complete. Selected areas: " + getSelectedAreasDebugInfo());
        }
    }
}