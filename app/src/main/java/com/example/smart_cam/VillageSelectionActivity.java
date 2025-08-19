package com.example.smart_cam;

import android.annotation.SuppressLint;
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

public class VillageSelectionActivity extends BaseActivity {
    private static final String TAG = "VillageSelectAct";
    private ListView lvAreas;
    private AreaAdapter adapter;
    private List<Area> areas;
    private String reportingType;
    private int reportTypeId;
    private String username;
    private int userId;

    private class Area {
        int id;
        String name;
        String rangeName;
        String divisionName;

        Area(int id, String name, String rangeName, String divisionName) {
            this.id = id;
            this.name = name;
            this.rangeName = rangeName;
            this.divisionName = divisionName;
        }

        @Override
        public String toString() {
            return String.format("%s (%s, %s)", name, rangeName, divisionName);
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
        setContentView(R.layout.activity_village_selection);

        // Get reporting type, ID, and selected areas from Intent
        reportingType = getIntent().getStringExtra("REPORTING_TYPE");
        reportTypeId = getIntent().getIntExtra("REPORT_TYPE_ID", -1);
        username = getIntent().getStringExtra("username");
        userId = getIntent().getIntExtra("user_id", -1);

        // Initialize UI elements
        TextView tvTitle = findViewById(R.id.tv_title);
        lvAreas = findViewById(R.id.lv_villages);
        Button btnNext = findViewById(R.id.btn_next);
        Button btnBack = findViewById(R.id.btn_back);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set title
        tvTitle.setText(reportingType != null ? reportingType + ": Elephant Found in areas" : "Elephant Found in areas");

        // Fetch user-mapped areas
        fetchUserMappedAreas();

        // Add scroll listener to debug scrolling issues
        lvAreas.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE && adapter != null) {
                    // When scrolling stops, just log the state
                    Log.d(TAG, "Scroll stopped. Selected areas: " + adapter.getSelectedAreaIds().size());
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // Optional: Log scroll events for debugging
            }
        });

        // Handle Back button
        btnBack.setOnClickListener(v -> finish());

        // Handle Next button
        btnNext.setOnClickListener(v -> {
            // Validate and fix any inconsistencies before proceeding
            if (adapter != null) {
                adapter.validateAndFixInconsistencies();
            }
            ArrayList<Integer> selectedAreaIds = adapter.getSelectedAreaIds();
            ArrayList<String> selectedAreaNames = adapter.getSelectedAreaNames();
            if (selectedAreaIds.isEmpty()) {
                Toast toast = new Toast(this);
                View toastView = getLayoutInflater().inflate(R.layout.custom_toast, null);
                TextView toastMessage = toastView.findViewById(R.id.toast_message);
                toastMessage.setTextColor(getResources().getColor(R.color.green));
                toastMessage.setText("Please select at least one area");
                toast.setView(toastView);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else {
                Intent intent = new Intent(VillageSelectionActivity.this, ReportVillageActivity.class);
                intent.putExtra("REPORTING_TYPE", reportingType);
                intent.putExtra("REPORT_TYPE_ID", reportTypeId);
                intent.putExtra("username", username);
                intent.putExtra("user_id", userId);
                intent.putIntegerArrayListExtra("SELECTED_AREA_IDS", selectedAreaIds);
                intent.putStringArrayListExtra("SELECTED_AREA_NAMES", selectedAreaNames);
                startActivity(intent);
            }
        });

        // Set up bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.nav_add);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
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
                Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    @SuppressLint("LongLogTag")
    private void fetchUserMappedAreas() {
        Log.d(TAG, "Fetching user mapped areas for user_id: " + userId);

        // Prepare JSON input
        JSONObject jsonInput = new JSONObject();
        try {
            jsonInput.put("user_id", userId);
            jsonInput.put("stage", "dev");
        } catch (Exception e) {
            Log.e(TAG, "Error building request body: " + e.getMessage(), e);
            showToast("Error preparing request");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                AppConfig.GET_AREAS,
                jsonInput,
                response -> {
                    Log.d(TAG, "Areas response: " + response.toString());
                    try {
                        // Server returns RESULT array with area objects
                        if (!response.has("RESULT")) {
                            String message = response.optString("p_out_mssg", "Failed to fetch areas");
                            Log.e(TAG, "Areas fetch failed: RESULT not present. " + message);
                            runOnUiThread(() -> showToast(message));
                            return;
                        }

                        JSONArray areasArray = response.getJSONArray("RESULT");
                        areas = new ArrayList<>();

                        for (int i = 0; i < areasArray.length(); i++) {
                            JSONObject areaObj = areasArray.getJSONObject(i);
                            int id = areaObj.optInt("area_id", -1);
                            String name = areaObj.optString("area_name", "");
                            String rangeName = areaObj.optString("range_name", "");
                            String divisionName = areaObj.optString("division_name", "");
                            if (id != -1 && !name.isEmpty()) {
                                areas.add(new Area(id, name, rangeName, divisionName));
                            }
                        }

                        if (areas.isEmpty()) {
                            runOnUiThread(() -> showToast("No areas found for your user account"));
                            return;
                        }

                        // Update UI on main thread
                        runOnUiThread(() -> {
                            adapter = new AreaAdapter(this, areas);
                            lvAreas.setAdapter(adapter);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing areas response: " + e.getMessage(), e);
                        runOnUiThread(() -> showToast("Error processing areas response"));
                    }
                },
                error -> {
                    Log.e(TAG, "Areas fetch Volley error: " + error.getMessage(), error);
                    String errorMsg;

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = new String(error.networkResponse.data);
                        Log.e(TAG, "Areas fetch HTTP error " + statusCode + ": " + responseData);

                        if (statusCode == 401 || statusCode == 403) {
                            runOnUiThread(() -> showToast("Authentication error: Please log in again"));
                            return;
                        }
                        errorMsg = "HTTP " + statusCode + ": " + responseData;
                    } else {
                        errorMsg = "Failed to fetch areas";
                    }

                    runOnUiThread(() -> showToast("Error fetching areas: " + errorMsg));
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("x-api-key", AppConfig.API_KEY);
                Log.d(TAG, "Areas request headers: " + headers.toString());
                return headers;
            }
        };

        // Set retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                15000, // 15 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Fetching areas from: " + AppConfig.GET_AREAS);
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

    private class AreaAdapter extends ArrayAdapter<Area> {
        private final ArrayList<Area> selectedAreas = new ArrayList<>();
        private final HashMap<Area, Boolean> areaCheckedStates; // Track by Area object for more reliable tracking
        private boolean isUpdatingCheckbox = false; // Flag to prevent listener loops

        public AreaAdapter(Context context, List<Area> areas) {
            super(context, 0, areas);
            this.areaCheckedStates = new HashMap<>();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Area area = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_village, parent, false);
            }

            CheckBox checkBox = convertView.findViewById(R.id.cb_village);
            checkBox.setText(area.toString());
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

                    // Update our tracking
                    areaCheckedStates.put(clickedArea, newState);

                    if (newState) {
                        if (!selectedAreas.contains(clickedArea)) {
                            selectedAreas.add(clickedArea);
                        }
                    } else {
                        selectedAreas.remove(clickedArea);
                    }
                }
            });

            return convertView;
        }

        public ArrayList<Integer> getSelectedAreaIds() {
            ArrayList<Integer> selectedIds = new ArrayList<>();
            for (Area area : selectedAreas) {
                selectedIds.add(area.id);
            }
            return selectedIds;
        }

        public ArrayList<String> getSelectedAreaNames() {
            ArrayList<String> selectedNames = new ArrayList<>();
            for (Area area : selectedAreas) {
                selectedNames.add(area.name);
            }
            return selectedNames;
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
            sb.append("Selected areas: ");
            for (Area area : selectedAreas) {
                sb.append(area.name).append(", ");
            }
            return sb.toString();
        }

        // Method to force refresh views without triggering listeners
        public void forceRefreshViews() {
            // This will call getView for all visible items, properly restoring their states
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