package com.example.smart_cam;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "elephant_alert_channel"; // Use new channel ID if muted earlier

    @Override
    public void onNewToken(String token) {
        Log.d("FCM", "New token: " + token);
        // You may want to send this token to your backend server
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("FCM", "Message received");

        String title = "Elephant Alert";
        String body = "Check the alert now!";

        if (remoteMessage.getNotification() != null) {
            // Notification payload
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } else if (remoteMessage.getData().size() > 0) {
            // Data payload
            title = remoteMessage.getData().get("title") != null ? remoteMessage.getData().get("title") : title;
            body = remoteMessage.getData().get("body") != null ? remoteMessage.getData().get("body") : body;
        }

        Log.d("FCM_DATA", "Title: " + title + ", Body: " + body);
        sendNotification(title, body);
    }

    private void sendNotification(String title, String body) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alert_sound);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Elephant Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for elephant conflict alerts");
            channel.setSound(soundUri, audioAttributes);
            channel.enableLights(true);
            channel.enableVibration(true);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);

        Intent intent;
        if (isLoggedIn) {
            intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("username", prefs.getString("username", ""));
            intent.putExtra("user_id", prefs.getInt("user_id", -1));
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ? PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_ONE_SHOT
        );

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.smart_cam); // Replace with your icon

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.smart_cam) // White-transparent icon recommended
                .setLargeIcon(largeIcon)
                .setContentTitle(title != null ? title : "Elephant Alert")
                .setContentText(body != null ? body : "Check the alert now.")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setContentIntent(pendingIntent);

        if (manager != null) {
            manager.notify(1, builder.build());
        }
    }
}
