package com.example.smart_cam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.smart_cam.network.VolleySingleton;
import com.example.smart_cam.utils.AppConfig;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BaseActivity {

    EditText edtUsername, edtPassword;
    Button btnLogin;
    ImageView iv_toggle_password;
    boolean isPasswordVisible = false;
    String fcmToken = "";
    private boolean isLoginInProgress = false;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate called");
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            isLoginInProgress = savedInstanceState.getBoolean("isLoginInProgress", false);
            fcmToken = savedInstanceState.getString("fcmToken", "");
            Log.d("MainActivity", "Restored state: isLoginInProgress=" + isLoginInProgress + ", fcmToken=" + fcmToken);
        }

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        Log.d("MainActivity", "Checking session: is_logged_in=" + prefs.getBoolean("is_logged_in", false) +
                ", username=" + prefs.getString("username", "") + ", user_id=" + prefs.getInt("user_id", -1));
        if (prefs.getBoolean("is_logged_in", false) && !isLoginInProgress) {
            Log.d("MainActivity", "User already logged in, navigating to DashboardActivity");
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            intent.putExtra("username", prefs.getString("username", ""));
            intent.putExtra("user_id", prefs.getInt("user_id", -1));
            startActivity(intent);
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.light_status_bar_bg));
        }

        try {
            edtUsername = findViewById(R.id.et_guard_id);
            edtPassword = findViewById(R.id.et_password);
            btnLogin = findViewById(R.id.btn_login);
            iv_toggle_password = findViewById(R.id.iv_toggle_password);
            Log.d("MainActivity", "UI elements initialized successfully");

            btnLogin.setEnabled(false);
            btnLogin.setOnClickListener(v -> {
                Log.d("MainActivity", "Login button clicked, enabled=" + btnLogin.isEnabled());
                Toast.makeText(this, "loggin button clicked", Toast.LENGTH_SHORT).show();
                loginUser();
            });
            Log.d("MainActivity", "Login button listener set, clickable=" + btnLogin.isClickable());
        } catch (Exception e) {
            Log.e("MainActivity", "Error initializing UI elements: " + e.getMessage(), e);
            Toast.makeText(this, "UI initialization failed", Toast.LENGTH_LONG).show();
            return;
        }

        iv_toggle_password.setOnClickListener(v -> {
            Log.d("MainActivity", "Password visibility toggle clicked");
            if (isPasswordVisible) {
                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                iv_toggle_password.setImageResource(R.drawable.baseline_visibility_off_24);
            } else {
                edtPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                iv_toggle_password.setImageResource(R.drawable.baseline_remove_red_eye_24);
            }
            edtPassword.setSelection(edtPassword.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });

        requestNotificationPermission();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isLoginInProgress", isLoginInProgress);
        outState.putString("fcmToken", fcmToken);
        Log.d("MainActivity", "Saving state: isLoginInProgress=" + isLoginInProgress + ", fcmToken=" + fcmToken);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MainActivity", "onStart called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("MainActivity", "onStop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "onDestroy called");
    }

    private void requestNotificationPermission() {
        Log.d("MainActivity", "Requesting notification permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification permission not granted, requesting");
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST);
                return;
            }
        }
        Log.d("MainActivity", "Notification permission already granted or not required");
        fetchFCMToken();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "Notification permission granted");
            } else {
                Log.w("Permission", "Notification permission denied");
                Toast.makeText(this, "Notification permission denied. Alerts may be suppressed.", Toast.LENGTH_SHORT).show();
            }
            fetchFCMToken();
        }
    }

    private void fetchFCMToken() {
        Log.d("FCM", "Fetching FCM token");
        btnLogin.setEnabled(false);
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM token failed", task.getException());
                        Toast.makeText(this, "FCM token fetch failed. Try again.", Toast.LENGTH_SHORT).show();
                        btnLogin.setEnabled(true);
                        return;
                    }
                    fcmToken = task.getResult();
                    Log.d("FCM", "Token: " + fcmToken);
                    btnLogin.setEnabled(true);
                    Log.d("MainActivity", "Login button enabled after FCM token fetch");
                });
    }

    private void loginUser() {
        Log.d("API_LOGIN", "loginUser called");
        if (isLoginInProgress) {
            Log.d("API_LOGIN", "Login already in progress, ignoring new request");
            return;
        }

        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Log.w("API_LOGIN", "Username or password empty");
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            btnLogin.setEnabled(true);
            return;
        }

        if (fcmToken == null || fcmToken.isEmpty()) {
            Log.w("API_LOGIN", "FCM token not ready");
            Toast.makeText(this, "FCM Token not ready. Please wait.", Toast.LENGTH_SHORT).show();
            btnLogin.setEnabled(true);
            return;
        }

        isLoginInProgress = true;
        btnLogin.setEnabled(false);
        Log.d("API_LOGIN", "Initiating login request for username: " + username);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("password", password);
            jsonBody.put("stage", "dev");
            jsonBody.put("fcm_token", fcmToken);

            Log.d("API_LOGIN", "Request body: " + jsonBody.toString());
            Log.d("API_LOGIN", "Sending request to: " + AppConfig.LOGIN_URL);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    AppConfig.LOGIN_URL,
                    jsonBody,
                    response -> {
                        isLoginInProgress = false;
                        Log.d("API_LOGIN", "Login response: " + response.toString());
                        try {
                            JSONObject result = response.getJSONObject("RESULT");
                            String flag = result.getString("p_out_mssg_flg");
                            String message = result.getString("p_out_mssg");

                            if (!flag.equals("S")) {
                                Log.e("API_LOGIN", "Login failed: " + message);
                                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                                btnLogin.setEnabled(true);
                                return;
                            }

                            int userId = result.getInt("user_id");
                            String responseUsername = result.optString("username", username);
                            // Remove auth_token storage - FCM token is not an auth token

                            SharedPreferences.Editor editor = getSharedPreferences("user_session", MODE_PRIVATE).edit();
                            editor.clear(); // Clear stale data
                            editor.putBoolean("is_logged_in", true);
                            editor.putString("username", responseUsername);
                            editor.putInt("user_id", userId);
                            // Don't store FCM token as auth_token
                            editor.apply();

                            Log.d("API_LOGIN", "Stored: username=" + responseUsername + ", user_id=" + userId);

                            Toast.makeText(this, "login successfull", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                            intent.putExtra("username", responseUsername);
                            intent.putExtra("user_id", userId);
                            // Don't pass FCM token as auth_token
                            startActivity(intent);
                            finish();

                        } catch (Exception e) {
                            isLoginInProgress = false;
                            Log.e("API_LOGIN", "Parsing error: " + e.getMessage(), e);
                            Toast.makeText(this, "Invalid response from server", Toast.LENGTH_SHORT).show();
                            btnLogin.setEnabled(true);
                        }
                    },
                    error -> {
                        isLoginInProgress = false;
                        btnLogin.setEnabled(true);
                        String errorMsg = "Login failed. Please try again.";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMsg = new String(error.networkResponse.data);
                            if (errorMsg.toLowerCase().contains("invalid")) {
                                errorMsg = "Invalid username or password";
                            }
                        }
                        Log.e("API_LOGIN", "Network error: " + errorMsg, error);
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("x-api-key", AppConfig.API_KEY);
                    Log.d("API_LOGIN", "Request headers: " + headers.toString());
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (Exception e) {
            isLoginInProgress = false;
            btnLogin.setEnabled(true);
            Log.e("API_LOGIN", "Exception during login request: " + e.getMessage(), e);
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }
}