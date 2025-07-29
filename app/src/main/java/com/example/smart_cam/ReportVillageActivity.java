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

public class ReportVillageActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_village);

        // Get reporting type and selected villages from Intent
        String reportingType = getIntent().getStringExtra("REPORTING_TYPE");
        ArrayList<String> selectedVillages = getIntent().getStringArrayListExtra("SELECTED_VILLAGES");

        // Initialize UI elements
        TextView tvTitle = findViewById(R.id.tv_title);
        ListView lvVillages = findViewById(R.id.lv_villages);
        Button btnSendShare = findViewById(R.id.btn_send_share);
        Button btnBack = findViewById(R.id.btn_back);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set title
        tvTitle.setText(reportingType != null ? reportingType + ": Report to Villages" : "Report to Villages");

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
        VillageAdapter adapter = new VillageAdapter(this, villages, selectedVillages);
        lvVillages.setAdapter(adapter);

        // Handle Back button
        btnBack.setOnClickListener(v -> finish());

        // Handle Send/Share button
        btnSendShare.setOnClickListener(v -> {
            Toast toast = new Toast(this);
            View toastView = getLayoutInflater().inflate(R.layout.custom_toast, null);
            TextView toastMessage = toastView.findViewById(R.id.toast_message);
            toastMessage.setTextColor(getResources().getColor(R.color.green));
            toastMessage.setText("Reports sent successfully");
            toast.setView(toastView);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            // Redirect to AddReportingActivity
            Intent intent = new Intent(ReportVillageActivity.this, AddReportingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
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
        private final ArrayList<String> selectedVillages;

        public VillageAdapter(Context context, String[] villages, ArrayList<String> selectedVillages) {
            super(context, 0, villages);
            this.selectedVillages = selectedVillages != null ? new ArrayList<>(selectedVillages) : new ArrayList<>();
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

            // Pre-check villages passed from previous activity
            checkBox.setChecked(selectedVillages.contains(village));

            return convertView;
        }

        public ArrayList<String> getSelectedVillages() {
            return selectedVillages;
        }
    }
}