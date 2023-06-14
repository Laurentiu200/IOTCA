package com.example.mygps;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;


public class NotificationActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String message = getIntent().getStringExtra("message");
    }
}