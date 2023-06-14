package com.example.mygps;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

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