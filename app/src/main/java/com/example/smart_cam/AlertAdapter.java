package com.example.smart_cam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.smart_cam.network.VolleySingleton;
import com.example.smart_cam.utils.AppConfig;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private Context context;
    private List<AlertItem> alertList;

    public AlertAdapter(Context context, List<AlertItem> alertList, int loggedInUserId) {
        this.context = context;
        this.alertList = alertList;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.alert_card, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        AlertItem alert = alertList.get(position);

        holder.title.setText(alert.getTitle());
        holder.description.setText(alert.getDescription());
        holder.location.setText(alert.getLocation());
        holder.time.setText(formatTimestamp(alert.getTime()));
        holder.severity.setText("Severity: " + alert.getSeverity());
        holder.attendedUser.setText("Attended by User: " + alert.getAttendedUser());

        String status = alert.getStatus().toLowerCase(Locale.ROOT);
        holder.status.setText(capitalizeFirst(status));

        holder.btnLocation.setOnClickListener(v -> {
            fetchLocationAndShowPopup(context, alert.getId());
        });

        switch (status) {
            case "pending":
                holder.status.setBackgroundResource(R.drawable.status_pending_background);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.brown));
                holder.btnAttend.setVisibility(View.VISIBLE);
                holder.btnResolveAlert.setVisibility(View.GONE);
                break;

            case "attending":
                holder.status.setBackgroundResource(R.drawable.status_attending_background);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.white));
                holder.btnAttend.setVisibility(View.GONE);
                holder.btnResolveAlert.setVisibility(View.VISIBLE);
                break;

            case "resolved":
                holder.status.setBackgroundResource(R.drawable.status_resolved_background);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.white));
                holder.btnAttend.setVisibility(View.GONE);
                holder.btnResolveAlert.setVisibility(View.GONE);
                break;

            default:
                holder.btnAttend.setVisibility(View.GONE);
                holder.btnResolveAlert.setVisibility(View.GONE);
                break;
        }

        holder.btnAttend.setOnClickListener(v -> {
            holder.btnAttend.setEnabled(false);
            AlertDialog loadingDialog = showLoadingDialog(context, "Attending alert...");

            if (context instanceof DashboardActivity) {
                ((DashboardActivity) context).manageAlert(alert.getId(), "attending");
                alert.setStatus("attending");
                notifyItemChanged(position);
            } else {
                Toast.makeText(context, "Action failed", Toast.LENGTH_SHORT).show();
                holder.btnAttend.setEnabled(true);
            }

            loadingDialog.dismiss();
        });


        holder.btnResolveAlert.setOnClickListener(v -> {
            holder.btnResolveAlert.setEnabled(false);
            AlertDialog loadingDialog = showLoadingDialog(context, "Resolving alert...");

            if (context instanceof DashboardActivity) {
                ((DashboardActivity) context).manageAlert(alert.getId(), "resolved");
                alert.setStatus("resolved");
                notifyItemChanged(position);
            } else {
                Toast.makeText(context, "Action failed", Toast.LENGTH_SHORT).show();
                holder.btnResolveAlert.setEnabled(true);
            }

            loadingDialog.dismiss();
        });


    }


    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, location, time, status, severity, attendedUser;
        Button btnAttend, btnLocation, btnResolveAlert;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.alertTitle);
            description = itemView.findViewById(R.id.alertDescription);
            location = itemView.findViewById(R.id.alertLocation);
            time = itemView.findViewById(R.id.alertTimestamp);
            status = itemView.findViewById(R.id.alertStatus);
            severity = itemView.findViewById(R.id.alertSeverity);
            attendedUser = itemView.findViewById(R.id.alertAttendedUser);
            btnAttend = itemView.findViewById(R.id.btnAttendAlert);
            btnLocation = itemView.findViewById(R.id.btnViewLocation);
            btnResolveAlert = itemView.findViewById(R.id.btnResolveAlert);
        }
    }

    private void fetchLocationAndShowPopup(Context context, int alertId) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("alert_id", alertId);
            jsonBody.put("stage", "dev");

            Log.d("FETCH_LOCATION", "Sending request: " + jsonBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    AppConfig.GET_LOCATION_URL,
                    jsonBody,
                    response -> {
                        try {


                            Log.e("resp",response.toString());
                            JSONObject result = response.getJSONObject("RESULT");

                            String status = result.optString("p_out_mssg_flg", "E");
                            String message = result.optString("p_out_mssg", "No message");

                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

                            if (status.equalsIgnoreCase("S")) {
                                String location = result.optString("location", "Unknown");
                                String latitude = result.optString("latitude", "N/A");
                                String longitude = result.optString("longitude", "N/A");

                                showLocationPopup(context, latitude, longitude, location);
                            } else {
                                showErrorPopup(context, message);
                            }

                        } catch (Exception e) {
                            Log.e("FETCH_LOCATION", "Parse error: " + e.getMessage());
                            e.printStackTrace();
                            showErrorPopup(context, "Error parsing response");
                        }

                    },
                    error -> {
                        Toast.makeText(context, "Location fetch failed", Toast.LENGTH_SHORT).show();
                        Log.e("FETCH_LOCATION", "Volley error: " + error.toString());
                    }) {

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("x-api-key", AppConfig.API_KEY);
                    return headers;
                }
            };

            VolleySingleton.getInstance(context).addToRequestQueue(request);

        } catch (Exception e) {
            Log.e("FETCH_LOCATION", "Request error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showLocationPopup(Context context, String latitude, String longitude, String location) {
        if (!(context instanceof Activity) || ((Activity) context).isFinishing()) {
            return;
        }

        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_location, null);
        TextView latitudeText = popupView.findViewById(R.id.tv_latitude);
        TextView longitudeText = popupView.findViewById(R.id.tv_longitude);
        TextView locationText = popupView.findViewById(R.id.tv_location);
        Button okButton = popupView.findViewById(R.id.btn_ok);

        latitudeText.setText("Latitude: " + latitude);
        longitudeText.setText("Longitude: " + longitude);
        locationText.setText("Location: " + capitalizeFirst(location));

        AlertDialog dialog = new AlertDialog.Builder((Activity) context)
                .setView(popupView)
                .create();

        okButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showErrorPopup(Context context, String message) {
        if (!(context instanceof Activity) || ((Activity) context).isFinishing()) {
            return;
        }

        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_location, null);
        TextView latitudeText = popupView.findViewById(R.id.tv_latitude);
        TextView longitudeText = popupView.findViewById(R.id.tv_longitude);
        TextView locationText = popupView.findViewById(R.id.tv_location);
        Button okButton = popupView.findViewById(R.id.btn_ok);

        latitudeText.setText("");
        longitudeText.setText("");
        locationText.setText(message);

        AlertDialog dialog = new AlertDialog.Builder((Activity) context)
                .setView(popupView)
                .create();

        okButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String formatTimestamp(String rawTimestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(rawTimestamp);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return rawTimestamp;
        }
    }

    private AlertDialog showLoadingDialog(Context context, String message) {
        View loaderView = LayoutInflater.from(context).inflate(R.layout.popup_loader, null);
        TextView loadingText = loaderView.findViewById(R.id.loading_text);
        loadingText.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(loaderView)
                .setCancelable(false)
                .create();
        dialog.show();
        return dialog;
    }

}
