package com.example.activityfeedback.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.activityfeedback.R;
import com.example.activityfeedback.models.User;
import com.example.activityfeedback.utils.FirebaseHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseHelper.initialize();

        // Add a delay to show splash screen
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserAndNavigate, 1500);
    }

    private void checkUserAndNavigate() {
        try {
            String currentUserId = FirebaseHelper.getCurrentUserId();

            if (currentUserId != null) {
                // User is logged in, get user details and navigate accordingly
                FirebaseHelper.getCurrentUser(user -> {
                    if (user != null) {
                        navigateBasedOnRole(user);
                    } else {
                        // If we can't get the user data, log them out and go to login
                        FirebaseHelper.logoutUser();
                        navigateToLogin();
                    }
                });
            } else {
                // No user logged in, navigate to login
                navigateToLogin();
            }
        } catch (Exception e) {
            // If any error occurs during initialization, go to login
            Log.e("MainActivity", "Error during initialization", e);
            navigateToLogin();
        }
    }

    private void navigateBasedOnRole(User user) {
        Intent intent;
        if (user.isProfessor()) {
            intent = new Intent(MainActivity.this, ProfessorDashboardActivity.class);
        } else {
            intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
        }
        intent.putExtra("userId", user.getUserId());
        intent.putExtra("userName", user.getName());
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}