package com.example.connectu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 1. Display splash screen for 2 seconds
        new Handler().postDelayed(() -> {

            // 2. Check whether the user has an existing login session
            SharedPreferences prefs =
                    getSharedPreferences("ConnectU", MODE_PRIVATE);

            String savedStudentId =
                    prefs.getString("studentId", null);

            if (savedStudentId != null &&
                    !savedStudentId.isEmpty()) {
                // 3. Redirect logged-in user to the main page
                Intent intent =
                        new Intent(SplashActivity.this,
                                MainActivity.class);
                intent.putExtra("studentId", savedStudentId);
                startActivity(intent);
            } else {
                // 4. Redirect new user to the login page
                Intent intent =
                        new Intent(SplashActivity.this,
                                LoginActivity.class);
                startActivity(intent);
            }

            // 5. Close splash activity
            finish();

        }, 2000);
    }
}