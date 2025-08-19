package com.example.smart_cam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.smart_cam.network.VolleySingleton;
import com.example.smart_cam.utils.AppConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends BaseActivity {

    TextView tabAll, tabPending, tabAttending, tabResolved;
    RecyclerView recyclerView;
    AlertAdapter adapter;
    ArrayList<AlertItem> fullAlertList = new ArrayList<>();
    ArrayList<AlertItem> filteredAlertList = new ArrayList<>();
    String username;
    int userId;
    Button btnLogout;
    String currentFilterStatus = "pending";

    public interface AlertStatusCallback {
        void onSuccess();
        void onFailure();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Retrieve from SharedPreferences first, then Intent extras
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        username = prefs.getString("username", getIntent().getStringExtra("username"));
        userId = prefs.getInt("user_id", getIntent().getIntExtra("user_id", -1));

        // Log session data
        Log.d("DashboardActivity", "Session: username=" + username + ", user_id=" + userId);

        // Validate session
        if (username == null || username.isEmpty() || userId == -1) {
            Log.w("DashboardActivity", "Invalid session, redirecting to MainActivity");
            handleSessionExpired();
            return;
        }

        // Store in SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", username);
        editor.putInt("user_id", userId);
        editor.apply();
        Log.d("DashboardActivity", "Stored in SharedPreferences: username=" + username + ", user_id=" + userId);

        // Initialize UI components
        tabAll = findViewById(R.id.btnAll);
        tabPending = findViewById(R.id.btnPending);
        tabAttending = findViewById(R.id.btnAttending);
        tabResolved = findViewById(R.id.btnResolved);
        btnLogout = findViewById(R.id.btnLogout);
        recyclerView = findViewById(R.id.recyclerView);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set navigation item
        String selectedNav = getIntent().getStringExtra("selected_nav");
        if ("add".equals(selectedNav)) {
            bottomNavigationView.setSelectedItemId(R.id.nav_add);
        } else {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        // Initialize RecyclerView
        adapter = new AlertAdapter(this, filteredAlertList, username);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Set tab click listeners
        tabAll.setOnClickListener(v -> {
            currentFilterStatus = "all";
            filterAlerts();
            highlightSelectedTab(tabAll);
        });
        tabPending.setOnClickListener(v -> {
            currentFilterStatus = "pending";
            filterAlerts();
            highlightSelectedTab(tabPending);
        });
        tabAttending.setOnClickListener(v -> {
            currentFilterStatus = "attending";
            filterAlerts();
            highlightSelectedTab(tabAttending);
        });
        tabResolved.setOnClickListener(v -> {
            currentFilterStatus = "resolved";
            filterAlerts();
            highlightSelectedTab(tabResolved);
        });

        btnLogout.setOnClickListener(v -> logoutUser());

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Log.d("NAVIGATION", "Item clicked: " + item.getItemId());
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Log.d("NAVIGATION", "Home selected");
                return true;
            } else if (itemId == R.id.nav_add) {
                Log.d("NAVIGATION", "Add selected");
                try {
                    Intent intent = new Intent(DashboardActivity.this, AddReportingActivity.class);
                    intent.putExtra("username", username);
                    intent.putExtra("user_id", userId);
                    startActivity(intent);
                    Log.d("NAVIGATION", "AddReportingActivity started with username=" + username + ", user_id=" + userId);
                    return true;
                } catch (Exception e) {
                    Log.e("NAVIGATION", "Error starting AddReportingActivity: " + e.getMessage());
                    Toast.makeText(this, "Error opening Add page: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    return false;
                }
            } else if (itemId == R.id.nav_menu) {
                Log.d("NAVIGATION", "Menu selected");
                Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // Fetch alerts
        fetchAlerts();
        highlightSelectedTab(tabPending);
    }

    private void filterAlerts() {
        filteredAlertList.clear();
        if ("all".equals(currentFilterStatus)) {
            filteredAlertList.addAll(fullAlertList);
        } else {
            for (AlertItem alert : fullAlertList) {
                if (alert.getStatus().equalsIgnoreCase(currentFilterStatus)) {
                    filteredAlertList.add(alert);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    public void fetchAlerts() {
        Log.d("API_FETCH_ALERTS", "fetchAlerts called");
        // No need to check auth_token - backend only uses x-api-key

        fullAlertList.clear();

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("status", "all");
            jsonBody.put("stage", "dev");

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    AppConfig.ALERT_STATUS_URL, jsonBody,
                    response -> {
                        Log.d("API_FETCH_ALERTS", "Response: " + response.toString());
                        try {
                            JSONArray arr = response.getJSONArray("RESULT");
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                String attendedUser = obj.optString("attended_user", "N/A");
                                Log.d("API_FETCH_ALERTS", "Alert ID: " + obj.getInt("alert_id") +
                                        ", Attended User: " + attendedUser);
                                fullAlertList.add(new AlertItem(
                                        obj.getInt("alert_id"),
                                        obj.getString("alert_type"),
                                        obj.getString("description"),
                                        obj.getString("location"),
                                        obj.getString("date"),
                                        obj.getString("status").toLowerCase(Locale.ROOT),
                                        obj.optString("severity", "N/A"),
                                        attendedUser
                                ));
                            }
                            Collections.sort(fullAlertList, (a1, a2) -> a2.getTime().compareTo(a1.getTime()));
                            filterAlerts();
                        } catch (Exception e) {
                            Log.e("API_FETCH_ALERTS", "Parsing error: " + e.getMessage());
                            runOnUiThread(() -> Toast.makeText(this, "Error parsing alerts: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    },
                    error -> handleVolleyError(error, "Failed to fetch alerts"))
            {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("x-api-key", AppConfig.API_KEY);
                    // Remove Authorization header - backend only uses x-api-key
                    Log.d("API_FETCH_ALERTS", "Request headers: " + headers.toString());
                    return headers;
                }
            };

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (Exception e) {
            Log.e("API_FETCH_ALERTS", "Exception: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Exception occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    public void manageAlert(int alertId, String newStatus, AlertStatusCallback callback) {
        Log.d("API_MANAGE_ALERT", "manageAlert called for alert_id=" + alertId + ", status=" + newStatus);
        if (userId == -1) {
            Log.w("API_MANAGE_ALERT", "Invalid user_id, calling handleSessionExpired");
            handleSessionExpired();
            if (callback != null) callback.onFailure();
            return;
        }

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("alert_id", alertId);
            jsonBody.put("status", newStatus);
            jsonBody.put("user_id", userId);
            jsonBody.put("stage", "dev");

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    AppConfig.MANAGE_ALERTS_URL, jsonBody,
                    response -> {
                        Log.d("API_MANAGE_ALERT", "Response: " + response.toString());
                        try {
                            JSONObject result = response.getJSONObject("RESULT");
                            String flag = result.getString("p_out_mssg_flg");
                            String message = result.getString("p_out_mssg");

                            if (flag.equals("S")) {
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Alert status updated successfully", Toast.LENGTH_SHORT).show();
                                    if (callback != null) callback.onSuccess();
                                });
                                fetchAlerts(); // Refresh the list
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Failed to update alert status: " + message, Toast.LENGTH_LONG).show();
                                    if (callback != null) callback.onFailure();
                                });
                            }
                        } catch (Exception e) {
                            Log.e("API_MANAGE_ALERT", "Parsing error: " + e.getMessage());
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                if (callback != null) callback.onFailure();
                            });
                        }
                    },
                    error -> {
                        Log.e("API_MANAGE_ALERT", "Network error: " + error.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.onFailure();
                        });
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("x-api-key", AppConfig.API_KEY);
                    // Remove Authorization header - backend only uses x-api-key
                    Log.d("API_MANAGE_ALERT", "Request headers: " + headers.toString());
                    return headers;
                }
            };

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (Exception e) {
            Log.e("API_MANAGE_ALERT", "Exception: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Exception occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            if (callback != null) callback.onFailure();
        }
    }

    private void logoutUser() {
        Log.d("API_LOGOUT", "logoutUser called");
        if (username == null || username.isEmpty()) {
            Log.w("API_LOGOUT", "Invalid username, calling handleSessionExpired");
            handleSessionExpired();
            return;
        }

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("stage", "dev");

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    AppConfig.LOGOUT_URL, jsonBody,
                    response -> {
                        Log.d("API_LOGOUT", "Logout response: " + response.toString());
                        try {
                            JSONObject result = response.getJSONObject("RESULT");
                            String flag = result.getString("p_out_mssg_flg");
                            String message = result.getString("p_out_mssg");

                            if (flag.equals("S")) {
                                runOnUiThread(() -> Toast.makeText(this, "Logout successful", Toast.LENGTH_SHORT).show());
                                SharedPreferences.Editor editor = getSharedPreferences("user_session", MODE_PRIVATE).edit();
                                editor.clear().apply();
                                Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);
                                finish();
                            } else {
                                runOnUiThread(() -> Toast.makeText(this, "Logout failed: " + message, Toast.LENGTH_LONG).show());
                            }
                        } catch (Exception e) {
                            Log.e("API_LOGOUT", "Parsing error: " + e.getMessage());
                            runOnUiThread(() -> Toast.makeText(this, "Logout error: Invalid response", Toast.LENGTH_SHORT).show());
                        }
                    },
                    error -> handleVolleyError(error, "Logout failed: Network error"))
            {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("x-api-key", AppConfig.API_KEY);
                    // Remove Authorization header - backend only uses x-api-key
                    Log.d("API_LOGOUT", "Request headers: " + headers.toString());
                    return headers;
                }
            };

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (Exception e) {
            Log.e("API_LOGOUT", "Exception: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Logout failed: Exception occurred", Toast.LENGTH_SHORT).show());
        }
    }

    private void handleSessionExpired() {
        Log.d("DashboardActivity", "handleSessionExpired called");
        SharedPreferences.Editor editor = getSharedPreferences("user_session", MODE_PRIVATE).edit();
        editor.clear().apply();
        runOnUiThread(() -> {
            Toast.makeText(this, "session expires", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void handleVolleyError(com.android.volley.VolleyError error, String defaultMessage) {
        String errorMsg;
        if (error.networkResponse != null && error.networkResponse.data != null) {
            errorMsg = new String(error.networkResponse.data);
        } else {
            errorMsg = defaultMessage;
        }
        Log.e("API_ERROR", "Error: " + errorMsg + ", statusCode=" +
                (error.networkResponse != null ? error.networkResponse.statusCode : "unknown"));
        runOnUiThread(() -> {
            if (error.networkResponse != null && error.networkResponse.statusCode == 403 ||
                    errorMsg.contains("Invalid Authentication Token") ||
                    errorMsg.contains("Missing Authentication Token") ||
                    errorMsg.contains("Invalid key=value pair")) {
                handleSessionExpired();
            } else {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void highlightSelectedTab(TextView selectedTab) {
        tabAll.setBackgroundResource(R.drawable.tab_unselected);
        tabPending.setBackgroundResource(R.drawable.tab_unselected);
        tabAttending.setBackgroundResource(R.drawable.tab_unselected);
        tabResolved.setBackgroundResource(R.drawable.tab_unselected);
        selectedTab.setBackgroundResource(R.drawable.tab_selected);
    }
}