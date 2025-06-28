package com.wcucampus.campuscompass.controller;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;

import android.support.v7.app.AppCompatActivity;

import com.wcucampus.campuscompass.R;


public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the welcome page layout
        setContentView(R.layout.page);

        // Start a timer for 2 seconds (adjust as needed)
        new CountDownTimer(4000, 1000) {
            public void onFinish() {
                // Create an intent to start the main activity
                Intent intent = new Intent(WelcomeActivity.this, Main2Activity.class);
                startActivity(intent);

                // Finish the welcome activity so the user can't go back to it
                finish();
            }

            public void onTick(long millisUntilFinished) {
                // Do nothing on tick
            }
        }.start();
    }
}

