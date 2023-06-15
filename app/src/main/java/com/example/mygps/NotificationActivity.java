package com.example.mygps;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

//notification activity to receive notification exactly like in the lab
public class NotificationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String message = getIntent().getStringExtra("message");
    }
}