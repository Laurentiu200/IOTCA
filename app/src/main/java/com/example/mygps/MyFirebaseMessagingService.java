package com.example.mygps;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

//firebase messaging service to receive notification almost exactly like in the lab
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FIREBASE_MESSAGING";
    private String channelId="1234";

    public MyFirebaseMessagingService() {
    }

    @Override
    public void onNewToken(@NonNull String token) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            String id = remoteMessage.getData().get("id");
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("body");
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            sendLocal(title,message,id);

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    private void handleNow() {
    }

    private void scheduleJob() {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendLocal(String title, String message, String id){
        Intent intent = new Intent(this, NotificationActivity.class);
        intent.putExtra("message",message);
        intent.setAction(Long.toString(System.currentTimeMillis())); // extras are not added if there is no action

        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent pendingIntent=PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Uri sound= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        NotificationChannel notificationChannel= new NotificationChannel(
                channelId,
                "NOTIFICATION_CHANNEL",
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationChannel.setVibrationPattern(new long[]{0,500});
        notificationChannel.setSound(sound,null);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,channelId)
                .setContentIntent(pendingIntent)
                .setContentTitle(title)
                .setContentText(message)
                .setSound(sound);
        notificationManager.notify(1234,builder.build());
    }
}