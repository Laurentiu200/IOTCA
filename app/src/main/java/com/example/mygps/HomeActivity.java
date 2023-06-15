package com.example.mygps;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

// view to subscribe to topic, like in the lab but adapted to our needs
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        subscribeToNotificationTopic("trouble");
    }

    private void subscribeToNotificationTopic(String topic){
        FirebaseApp.initializeApp(this);
        FirebaseMessaging.getInstance().subscribeToTopic(topic);
    }
}