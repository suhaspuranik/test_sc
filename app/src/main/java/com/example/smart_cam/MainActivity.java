package com.example.smart_cam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        if (prefs.getBoolean("is_logged_in", false)) {
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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_status_bar_fallback));
        }

        edtUsername = findViewById(R.id.et_guard_id);
        edtPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        iv_toggle_password = findViewById(R.id.iv_toggle_password);

        iv_toggle_password.setOnClickListener(v -> {
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

        btnLogin.setOnClickListener(v -> loginUser());

        fetchFCMToken();
    }

    private void fetchFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM token failed", task.getException());
                        return;
                    }
                    fcmToken = task.getResult();
                    Log.d("FCM", "Token: " + fcmToken);
                });
    }

    private void loginUser() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("password", password);
            jsonBody.put("stage", "dev");
            jsonBody.put("fcm_token", fcmToken);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    AppConfig.LOGIN_URL,
                    jsonBody,
                    response -> {
                        Log.d("API_LOGIN", "Login response: " + response.toString());

                        try {
                            JSONObject result = response.getJSONObject("RESULT");

                            // Check for expected fields
                            if (!result.has("user_id")) {
                                throw new Exception("Missing user_id in response");
                            }

                            int userId = result.getInt("user_id");

                            // ✅ Now show login success toast
                            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();

                            SharedPreferences.Editor editor = getSharedPreferences("user_session", MODE_PRIVATE).edit();
                            editor.putBoolean("is_logged_in", true);
                            editor.putString("username", username);
                            editor.putInt("user_id", userId);
                            editor.apply();

                            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                            intent.putExtra("username", username);
                            intent.putExtra("user_id", userId);
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            Log.e("API_LOGIN", "Parsing error: " + e.getMessage());
                            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                            btnLogin.setEnabled(true);
                        }
                    },
                    error -> {
                        btnLogin.setEnabled(true);
                        String errorMsg = "Login failed. Please try again.";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMsg = new String(error.networkResponse.data);
                            if (errorMsg.toLowerCase().contains("invalid")) {
                                errorMsg = "Invalid username or password";
                            }
                        }
                        Log.e("API_LOGIN", "Error: " + errorMsg);
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
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
            btnLogin.setEnabled(true);
            Log.e("API_LOGIN", "Exception: " + e.getMessage());
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

}
