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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;


public class DashboardActivity extends BaseActivity {

    TextView tabAll, tabPending, tabAttending, tabResolved;
    RecyclerView recyclerView;
    AlertAdapter adapter;
    ArrayList<AlertItem> fullAlertList = new ArrayList<>();
    ArrayList<AlertItem> filteredAlertList = new ArrayList<>();
    String username;
    String token_app;
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


        username = getIntent().getStringExtra("username");
        userId = getIntent().getIntExtra("user_id", -1);
        token_app = getIntent().getStringExtra("token_app");

        tabAll = findViewById(R.id.btnAll);
        tabPending = findViewById(R.id.btnPending);
        tabAttending = findViewById(R.id.btnAttending);
        tabResolved = findViewById(R.id.btnResolved);
        btnLogout = findViewById(R.id.btnLogout);
        recyclerView = findViewById(R.id.recyclerView);

        adapter = new AlertAdapter(this, filteredAlertList, username);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

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

        fetchAlerts();
        highlightSelectedTab(tabPending);
    }

    public void fetchAlerts() {
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
                                // Log the attended_user to debug
                                Log.d("API_FETCH_ALERTS", "Alert ID: " + obj.getInt("alert_id") + ", Attended User: " + attendedUser);
                                fullAlertList.add(new AlertItem(
                                        obj.getInt("alert_id"),
                                        obj.getString("alert_type"),
                                        obj.getString("description"),
                                        obj.getString("location"),
                                        obj.getString("date"),
                                        obj.getString("status").toLowerCase(Locale.ROOT),
                                        obj.optString("severity", "N/A"),
                                        attendedUser // Use the parsed value directly
                                ));
                            }
                            Collections.sort(fullAlertList, (a1, a2) -> a2.getTime().compareTo(a1.getTime()));

                            filterAlerts();
                        } catch (Exception e) {
                            Log.e("API_FETCH_ALERTS", "Parsing error: " + e.getMessage());
                            Toast.makeText(this, "Parsing error", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String errorMsg = new String(error.networkResponse.data);
                            Log.e("API_FETCH_ALERTS", "Error: " + errorMsg);
                        } else {
                            Log.e("API_FETCH_ALERTS", "Error: " + error.toString());
                        }
                        Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("x-api-key", AppConfig.API_KEY);
                    return headers;
                }
            };

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (Exception e) {
            Log.e("API_FETCH_ALERTS", "Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filterAlerts() {
        filteredAlertList.clear();

        if (currentFilterStatus.equals("all")) {
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

    public void manageAlert(int alertId, String newStatus, AlertStatusCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("alert_id", alertId);
            jsonBody.put("user_id", userId);
            jsonBody.put("status", newStatus);
            jsonBody.put("stage", "dev");
            if (newStatus.equalsIgnoreCase("attending")) {
                jsonBody.put("attended_user", username); // Send username when attending
            }

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    AppConfig.MANAGE_ALERTS_URL, jsonBody,
                    response -> {
                        Log.d("API_MANAGE_ALERT", "Response: " + response.toString());
                        try {
                            JSONObject result = response.getJSONObject("RESULT");
                            String flag = result.optString("p_out_mssg_flg", "E");
                            String message = result.optString("p_out_mssg", "Unknown response");

                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                            if (flag.equalsIgnoreCase("S")) {
                                // Update the alert in fullAlertList
                                for (AlertItem alert : fullAlertList) {
                                    if (alert.getId() == alertId) {
                                        alert.setStatus(newStatus.toLowerCase(Locale.ROOT));
                                        if (newStatus.equalsIgnoreCase("attending")) {
                                            alert.setAttendedUser(username); // Update attended_user locally
                                        } else if (newStatus.equalsIgnoreCase("resolved")) {
                                            // Optionally retain or clear attended_user for resolved alerts
                                            alert.setAttendedUser(alert.getAttendedUser()); // Keep existing or set to null if needed
                                        }
                                        break;
                                    }
                                }
                                // Refresh the filtered list and notify adapter
                                filterAlerts();
                                if (callback != null) callback.onSuccess();
                            } else {
                                if (callback != null) callback.onFailure();
                            }
                        } catch (Exception e) {
                            Log.e("API_MANAGE_ALERT", "Parsing error: " + e.getMessage());
                            Toast.makeText(this, "Response parsing error", Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.onFailure();
                        }
                    },
                    error -> {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String errorMsg = new String(error.networkResponse.data);
                            Log.e("API_MANAGE_ALERT", "Error: " + errorMsg);
                        } else {
                            Log.e("API_MANAGE_ALERT", "Error: " + error.toString());
                        }
                        Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
                        if (callback != null) callback.onFailure();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("x-api-key", AppConfig.API_KEY);
                    return headers;
                }
            };

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (Exception e) {
            Log.e("API_MANAGE_ALERT", "Exception: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Exception occurred", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onFailure();
        }
    }


    private void logoutUser() {
        try {
            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
            String token_app = prefs.getString("fcm_token", "");
            String username = prefs.getString("username", "");

            SharedPreferences.Editor editor = getSharedPreferences("user_session", MODE_PRIVATE).edit();
            editor.putBoolean("is_logged_in", false);
            editor.apply();
            Log.d("LOGOUT_DEBUG", "Sending logout request");
            Log.d("LOGOUT_DEBUG", "Username: " + username);
            Log.d("LOGOUT_DEBUG", "Token: " + token_app);

            if (token_app.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Invalid session. Please log in again.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                startActivity(intent);
                return;
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("token_app", token_app);
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
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                                getSharedPreferences("user_session", MODE_PRIVATE).edit().clear().apply();

                                startActivity(new Intent(DashboardActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "Logout failed: " + message, Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Logout error: Invalid response", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String errorMsg = new String(error.networkResponse.data);
                            Log.e("API_LOGOUT", "Logout error: " + errorMsg);
                        } else {
                            Log.e("API_LOGOUT", "Logout error: " + error.toString());
                        }
                        Toast.makeText(this, "Logout failed: Network error", Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("x-api-key", AppConfig.API_KEY);
                    return headers;
                }
            };

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (Exception e) {
            Log.e("API_LOGOUT", "Exception: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Logout failed: Exception occurred", Toast.LENGTH_SHORT).show();
        }
    }




    private void highlightSelectedTab(TextView selectedTab) {
        tabAll.setBackgroundResource(R.drawable.tab_unselected);
        tabPending.setBackgroundResource(R.drawable.tab_unselected);
        tabAttending.setBackgroundResource(R.drawable.tab_unselected);
        tabResolved.setBackgroundResource(R.drawable.tab_unselected);

        selectedTab.setBackgroundResource(R.drawable.tab_selected);
    }
}