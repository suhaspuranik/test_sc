package com.example.smart_cam;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class VillageSelectionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_village_selection);

        // Get reporting type from Intent
        String reportingType = getIntent().getStringExtra("REPORTING_TYPE");

        // Initialize UI elements
        TextView tvTitle = findViewById(R.id.tv_title);
        ListView lvVillages = findViewById(R.id.lv_villages);
        Button btnNext = findViewById(R.id.btn_next);
        Button btnBack = findViewById(R.id.btn_back);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set title
        tvTitle.setText(reportingType != null ? reportingType + ": Elephant Found in areas" : "Elephant Found in areas");

        // Set up village list
        String[] villages = {
                "Siddapura", "Panjarpet", "Ammathi", "Heroor Estate", "Kattepura Reserve Forest", "Chembe Bellur",
                "Kudluru-Chettalli", "Ponnampet", "School Region Warnady Estate", "Gonikoppal",
                "Bhuvangala Reserve Forest", "Srimangala", "Dechalla estate", "Kirgoor", "Puliyeri-Injalagere",
                "Kanoor", "Shanthalli", "Beekanakadu Estate", "Madhapur", "Kushalnagar", "Harangi", "Kodlipet",
                "Alur", "Attur", "Green Field Estate", "A J Estate", "Chennangi Village boundry",
                "Hundi Villages", "Hebbale", "Valnur", "Gonimarur", "Others"
        };

        // Custom adapter for checkboxes
        VillageAdapter adapter = new VillageAdapter(this, villages);
        lvVillages.setAdapter(adapter);

        // Handle Back button
        btnBack.setOnClickListener(v -> finish());

        // Handle Next button
        btnNext.setOnClickListener(v -> {
            ArrayList<String> selectedVillages = adapter.getSelectedVillages();
            if (selectedVillages.isEmpty()) {
                Toast toast = new Toast(this);
                View toastView = getLayoutInflater().inflate(R.layout.custom_toast, null);
                TextView toastMessage = toastView.findViewById(R.id.toast_message);
                toastMessage.setTextColor(getResources().getColor(R.color.green));
                toastMessage.setText("Please select at least one village");
                toast.setView(toastView);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else {
                Intent intent = new Intent(VillageSelectionActivity.this, ReportVillageActivity.class);
                intent.putExtra("REPORTING_TYPE", reportingType);
                intent.putStringArrayListExtra("SELECTED_VILLAGES", selectedVillages);
                startActivity(intent);
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
                startActivity(new Intent(this, AddReportingActivity.class));
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

    // Custom adapter for ListView with checkboxes
    private static class VillageAdapter extends ArrayAdapter<String> {
        private final ArrayList<String> selectedVillages = new ArrayList<>();

        public VillageAdapter(Context context, String[] villages) {
            super(context, 0, villages);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String village = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_village, parent, false);
            }

            CheckBox checkBox = convertView.findViewById(R.id.cb_village);
            checkBox.setText(village);
            checkBox.setTextColor(getContext().getResources().getColor(R.color.black));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedVillages.contains(village)) {
                        selectedVillages.add(village);
                    }
                } else {
                    selectedVillages.remove(village);
                }
            });

            // Restore checked state if previously selected
            checkBox.setChecked(selectedVillages.contains(village));

            return convertView;
        }

        public ArrayList<String> getSelectedVillages() {
            return selectedVillages;
        }
    }
}